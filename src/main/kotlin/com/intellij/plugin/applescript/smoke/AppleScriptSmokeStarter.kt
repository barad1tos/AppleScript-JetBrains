package com.intellij.plugin.applescript.smoke

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ui.UIUtil
import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Headless smoke entry point for SDEF-14. Boots IDEA, opens AppleScript fixtures, and
 * asserts three Phase 8 invariants:
 *
 *  1. Composite 2-token fallback parses `tell application "Music" to play track 1` with
 *     zero `PsiErrorElement` nodes (Phase 8 D-15 invariant — fallback methods in
 *     `AppleScriptGeneratedParserUtil`).
 *  2. `BASIC` completion on `play ` produces a non-empty active lookup (validates the
 *     `CommandCompletionContributor` wiring end-to-end).
 *  3. `AppleScriptColorAnnotator` emits `WEAK_WARNING` (NOT `WARNING`) for
 *     `tell application "NoSuchApp_xyz" to activate` (Phase 8 D-15: missing-app refs
 *     must stay out of the Problems panel + editor gutter).
 *
 * Invoked via `-Didea.ApplicationStarter.command=applescript-smoke`. Reads fixture root
 * from `-Dapplescript.smoke.fixtureRoot=<path>` (set by the `runIdeHeadlessSmoke` Gradle
 * task to `src/test/resources/testData/runIde`).
 *
 * Project-bootstrap recipe: `ProjectUtil.openOrImport(Path, null, true)` is the primary
 * path — it mirrors the IDE's File -> Open lifecycle and gives a real `Project` +
 * `PsiManager`. If it returns null on a given platform version, the fallback uses
 * `ProjectManagerEx.openProject(Path, OpenProjectTask)`. Both paths are documented in
 * `02-02-SUMMARY.md`.
 *
 * Threading: every PSI / `DaemonCodeAnalyzer` interaction runs inside
 * `ApplicationManager.invokeAndWait { ... }` (Pattern C - EDT contract). Error handling
 * catches `RuntimeException` only; `Error` propagates (Pattern B).
 */
class AppleScriptSmokeStarter : ApplicationStarter {

    override val commandName: String = COMMAND_NAME

    override fun main(args: List<String>) {
        val fixtureRoot = System.getProperty(FIXTURE_ROOT_PROP)
        if (fixtureRoot.isNullOrBlank()) {
            LOG.error("[applescript-smoke] FAIL: -D$FIXTURE_ROOT_PROP not set")
            exitSmoke(1)
            return
        }
        val fixtureDir = File(fixtureRoot)
        if (!fixtureDir.isDirectory) {
            LOG.error("[applescript-smoke] FAIL: fixture root is not a directory: $fixtureRoot")
            exitSmoke(1)
            return
        }

        val failures = mutableListOf<String>()
        var project: Project? = null
        try {
            project = openOrCreateProject(fixtureDir.toPath())
            if (project == null) {
                failures.add("ProjectUtil.openOrImport + ProjectManagerEx fallback both returned null for $fixtureRoot")
            } else {
                val openedProject = project
                ApplicationManager.getApplication().invokeAndWait {
                    runAssertions(openedProject, fixtureDir, failures)
                }
            }
        } catch (exception: RuntimeException) {
            failures.add("smoke aborted: ${exception.message}")
            LOG.error("[applescript-smoke] aborted", exception)
        } finally {
            // Close the opened project so the test environment exits cleanly. Errors
            // here do not affect the assertion outcome — they are logged as warnings.
            project?.let { proj ->
                try {
                    ApplicationManager.getApplication().invokeAndWait {
                        ProjectManager.getInstance().closeAndDispose(proj)
                    }
                } catch (exception: RuntimeException) {
                    LOG.warn("[applescript-smoke] close project failed", exception)
                }
            }
        }

        if (failures.isEmpty()) {
            LOG.info("[applescript-smoke] PASS")
            exitSmoke(0)
        } else {
            failures.forEach { LOG.error("[applescript-smoke] FAIL: $it") }
            exitSmoke(1)
        }
    }

    private fun runAssertions(project: Project, fixtureDir: File, failures: MutableList<String>) {
        val playFile = File(fixtureDir, "play.applescript")
        val unresolvedFile = File(fixtureDir, "unresolved-app.applescript")

        assertComposite2TokenFallback(project, playFile, failures)
        assertCompletionOnPlay(project, playFile, failures)
        assertWeakWarningOnUnresolvedApp(project, unresolvedFile, failures)
    }

    // --- Assertion 1 — composite 2-token fallback (Phase 8 D-15) ---------------------

    private fun assertComposite2TokenFallback(project: Project, file: File, failures: MutableList<String>) {
        val psiFile = loadPsiFile(project, file, failures) ?: return
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        if (errors.isNotEmpty()) {
            val sample = errors.first().errorDescription
            failures.add(
                "composite 2-token fallback broken in ${file.name}: " +
                    "${errors.size} PsiErrorElement(s); first='$sample'",
            )
        }
    }

    // --- Assertion 2 — BASIC completion non-empty on `play ` -------------------------

    private fun assertCompletionOnPlay(project: Project, file: File, failures: MutableList<String>) {
        val psiFile = loadPsiFile(project, file, failures) ?: return
        val virtualFile = psiFile.virtualFile ?: run {
            failures.add("completion: virtualFile null for ${file.name}")
            return
        }
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: run {
            failures.add("completion: document null for ${file.name}")
            return
        }
        val playOffset = document.charsSequence.indexOf("play ")
        if (playOffset < 0) {
            failures.add("completion: 'play ' token not found in ${file.name}")
            return
        }
        val caret = playOffset + "play ".length
        val descriptor = OpenFileDescriptor(project, virtualFile, caret)
        val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true) ?: run {
            failures.add("completion: editor could not be opened for ${file.name}")
            return
        }

        CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(project, editor)
        UIUtil.dispatchAllInvocationEvents()

        val lookup = LookupManager.getInstance(project).activeLookup
        if (lookup == null || lookup.items.isEmpty()) {
            failures.add(
                "completion empty on 'play ' in ${file.name}: " +
                    "lookup=${if (lookup == null) "null" else "0 items"}",
            )
        }
    }

    // --- Assertion 3 — WEAK_WARNING (NOT WARNING) on unresolved app ------------------

    private fun assertWeakWarningOnUnresolvedApp(project: Project, file: File, failures: MutableList<String>) {
        val psiFile = loadPsiFile(project, file, failures) ?: return
        val virtualFile = psiFile.virtualFile ?: run {
            failures.add("annotator: virtualFile null for ${file.name}")
            return
        }
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: run {
            failures.add("annotator: document null for ${file.name}")
            return
        }
        val descriptor = OpenFileDescriptor(project, virtualFile, 0)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)

        // Trigger the daemon and drain the EDT queue so highlights are produced.
        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
        // Pump events until the daemon settles. UIUtil drains pending invocation events;
        // we loop a bounded number of times to give async highlight passes time to land.
        repeat(DAEMON_DRAIN_ITERATIONS) {
            UIUtil.dispatchAllInvocationEvents()
        }

        val targetText = "NoSuchApp_xyz"
        val targetOffset = document.charsSequence.indexOf(targetText)
        if (targetOffset < 0) {
            failures.add("annotator: '$targetText' not found in ${file.name}")
            return
        }
        val targetRange = TextRange(targetOffset, targetOffset + targetText.length)

        val weak = DaemonCodeAnalyzerImpl
            .getHighlights(document, HighlightSeverity.WEAK_WARNING, project)
            .filter { hi -> targetRange.intersects(hi.startOffset, hi.endOffset) }
        val warn = DaemonCodeAnalyzerImpl
            .getHighlights(document, HighlightSeverity.WARNING, project)
            .filter { hi -> hi.severity == HighlightSeverity.WARNING && targetRange.intersects(hi.startOffset, hi.endOffset) }

        if (weak.isEmpty()) {
            failures.add(
                "Phase 8 D-15 broken: expected >=1 WEAK_WARNING over '$targetText' in ${file.name}, " +
                    "got 0 (warn count=${warn.size})",
            )
        }
        if (warn.isNotEmpty()) {
            failures.add(
                "Phase 8 D-15 broken: expected 0 WARNING over '$targetText' in ${file.name}, " +
                    "got ${warn.size} (severity should be WEAK_WARNING)",
            )
        }
    }

    // --- Helpers ---------------------------------------------------------------------

    private fun loadPsiFile(project: Project, file: File, failures: MutableList<String>): PsiFile? {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile == null) {
            failures.add("VFile missing: ${file.absolutePath}")
            return null
        }
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
        if (psiFile == null) {
            failures.add("PsiFile missing for ${file.name}")
        }
        return psiFile
    }

    /**
     * Project bootstrap: `ProjectUtil.openOrImport` is the primary path (mirrors the
     * File -> Open lifecycle). Falls back to
     * `ProjectManagerEx.getInstanceEx().openProject(Path, OpenProjectTask)` only if the
     * primary returns null. Both options land us with a real `Project` + `PsiManager`
     * for the assertions.
     */
    private fun openOrCreateProject(fixtureDir: Path): Project? {
        val primary = try {
            ProjectUtil.openOrImport(fixtureDir, null, true)
        } catch (exception: RuntimeException) {
            LOG.warn("ProjectUtil.openOrImport threw for $fixtureDir", exception)
            null
        }
        if (primary != null) return primary

        // Fallback path - useful if openOrImport semantics shift between platform minor
        // versions. We keep the call surface narrow: openProject(Path, OpenProjectTask)
        // is available across 2024.3 -> 2025.x.
        return try {
            ProjectManagerEx.getInstanceEx().openProject(fixtureDir, OpenProjectTask {})
        } catch (exception: RuntimeException) {
            LOG.warn("ProjectManagerEx.openProject fallback threw for $fixtureDir", exception)
            null
        }
    }

    private fun exitSmoke(code: Int) {
        try {
            ApplicationManager.getApplication().exit(true, true, false)
        } catch (exception: RuntimeException) {
            LOG.warn("ApplicationManager.exit threw", exception)
        }
        exitProcess(code)
    }

    private companion object {
        const val COMMAND_NAME: String = "applescript-smoke"
        const val FIXTURE_ROOT_PROP: String = "applescript.smoke.fixtureRoot"
        const val DAEMON_DRAIN_ITERATIONS: Int = 50

        private val LOG: Logger = Logger.getInstance("#${AppleScriptSmokeStarter::class.java.name}")
    }
}
