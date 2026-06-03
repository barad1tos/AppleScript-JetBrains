package com.intellij.plugin.applescript.smoke

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

private const val FIXTURE_ROOT_PROP: String = "applescript.smoke.fixtureRoot"
private const val DAEMON_DRAIN_ITERATIONS: Int = 50

private val LOG: Logger = Logger.getInstance("#${AppleScriptSmokeStarter::class.java.name}")

/**
 * Headless smoke entry point for SDEF-14. Boots IDEA, opens AppleScript fixtures, and
 * asserts three Phase 8 invariants:
 *
 *  1. Composite 2-token fallback parses `tell application "Music" to play track 1` with
 *     zero [PsiErrorElement] nodes (Phase 8 D-15 invariant — fallback methods in
 *     `AppleScriptGeneratedParserUtil`).
 *  2. `BASIC` completion on `play ` produces a non-empty active lookup (validates the
 *     `CommandCompletionContributor` wiring end-to-end).
 *  3. `tell application "NoSuchApp_xyz" to activate` does not produce public
 *     `WARNING` / `ERROR` editor markup (Phase 8 D-15: missing-app refs must stay
 *     out of the Problems panel + editor gutter). Exact `WEAK_WARNING` severity is
 *     covered by the code-insight fixture tests, which can inspect `HighlightInfo`.
 *
 * Invoked by starting the IDE with the `applescript-smoke` command. Reads fixture root from
 * `-Dapplescript.smoke.fixtureRoot=<path>` (set by the `runIdeHeadlessSmoke` Gradle task to
 * `src/test/resources/testData/runIde`).
 *
 * Project-bootstrap recipe: [ProjectUtil.openOrImport] is the primary path — it mirrors
 * the IDE's File -> Open lifecycle and gives a real [Project] + [PsiManager]. If it
 * returns null on a given platform version, the fallback uses
 * [ProjectManagerEx.openProject] with a static [OpenProjectTask.build] no-arg call
 * (the public factory that does not depend on the internal Kotlin-DSL builder).
 *
 * Threading: readiness waits run off the EDT; every PSI / [DaemonCodeAnalyzer] interaction
 * runs inside `ApplicationManager.invokeAndWait { ... }` (Pattern C — EDT contract).
 * Error handling catches platform bootstrap exceptions only; [ProcessCanceledException]
 * and [Error] propagate (Pattern B).
 *
 * Public-API-only policy: the assertion path uses [DocumentMarkupModel.forDocument]
 * (public, in `editor-ui-ex/api-dump.txt`) + [HighlighterLayer] constants (public, in
 * `editor-ui-api/api-dump.txt`) to inspect daemon-produced highlighters instead of
 * `DaemonCodeAnalyzerImpl.getHighlights` (internal, `*Impl` package). This satisfies the
 * project rule "no internal APIs in plugin code" (see
 * `memory/feedback_no_internal_apis.md`).
 */
class AppleScriptSmokeStarter : ApplicationStarter {
    override fun main(args: List<String>) {
        val fixtureRoot = System.getProperty(FIXTURE_ROOT_PROP)
        val fixtureDir = fixtureDirectoryOrExit(fixtureRoot) ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val runner = AppleScriptSmokeRunner(fixtureRoot.orEmpty(), fixtureDir)
            exitSmoke(runner.run())
        }
    }

    private fun fixtureDirectoryOrExit(fixtureRoot: String?): File? {
        val fixtureDir = fixtureRoot?.takeIf { it.isNotBlank() }?.let(::File)
        val failureMessage =
            when {
                fixtureRoot.isNullOrBlank() -> "-D$FIXTURE_ROOT_PROP not set"
                fixtureDir?.isDirectory != true -> "fixture root is not a directory: $fixtureRoot"
                else -> null
            }
        if (failureMessage != null) {
            LOG.error("[applescript-smoke] FAIL: $failureMessage")
            exitSmoke(1)
        }

        return fixtureDir?.takeIf { failureMessage == null }
    }

    private fun exitSmoke(code: Int) {
        try {
            ApplicationManager.getApplication().exit(true, true, false, code)
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: IllegalStateException) {
            LOG.warn("ApplicationManager.exit threw", exception)
        } catch (exception: IllegalArgumentException) {
            LOG.warn("ApplicationManager.exit threw", exception)
        }
    }
}

private class AppleScriptSmokeRunner(
    private val fixtureRoot: String,
    private val fixtureDir: File,
) {
    private val assertions = AppleScriptSmokeAssertions()

    fun run(): Int {
        val failures = mutableListOf<String>()
        var project: Project? = null
        try {
            project = openProjectOnEdt(fixtureDir.toPath())
            val openedProject = project
            if (openedProject == null) {
                failures.add(
                    "ProjectUtil.openOrImport + ProjectManagerEx fallback " +
                        "both returned null for $fixtureRoot",
                )
            } else {
                failures.addAll(collectProjectFailures(openedProject))
            }
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: IllegalStateException) {
            failures.add("smoke aborted: ${exception.message}")
            LOG.error("[applescript-smoke] aborted", exception)
        } catch (exception: IllegalArgumentException) {
            failures.add("smoke aborted: ${exception.message}")
            LOG.error("[applescript-smoke] aborted", exception)
        }

        // Close the opened project so the test environment exits cleanly. Errors
        // here do not affect the assertion outcome; cancellation still propagates.
        project?.let(::closeProjectAfterSmoke)

        return if (failures.isEmpty()) {
            LOG.info("[applescript-smoke] PASS")
            0
        } else {
            failures.forEach { LOG.error("[applescript-smoke] FAIL: $it") }
            1
        }
    }

    private fun openProjectOnEdt(fixturePath: Path): Project? {
        var project: Project? = null
        ApplicationManager.getApplication().invokeAndWait {
            project = openOrCreateProject(fixturePath)
        }
        return project
    }

    private fun collectProjectFailures(openedProject: Project): List<String> {
        val failures = mutableListOf<String>()
        val registry = AppleScriptSystemDictionaryRegistryService.getInstance()
        val appsReady = awaitAppsReady(registry)
        if (appsReady.isFailure) {
            failures.add(
                "dictionary registry did not become ready: " +
                    appsReady.exceptionOrNull()?.message,
            )
        } else {
            DumbService.getInstance(openedProject).waitForSmartMode()
            assertions.run(openedProject, fixtureDir, failures)
        }
        return failures
    }

    private fun awaitAppsReady(registry: AppleScriptSystemDictionaryRegistryService): Result<Unit> {
        val future = CompletableFuture<Result<Unit>>()
        val job =
            registry.serviceScope.launch(start = CoroutineStart.UNDISPATCHED) {
                future.complete(registry.awaitAppsReadyInternal())
            }
        job.invokeOnCompletion { cause ->
            if (cause != null) {
                future.complete(Result.failure(cause))
            }
        }

        return try {
            future.get()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            Result.failure(exception)
        } catch (exception: ExecutionException) {
            Result.failure(exception.cause ?: exception)
        }
    }

    private fun closeProjectAfterSmoke(project: Project) {
        try {
            ApplicationManager.getApplication().invokeAndWait {
                val fileEditorManager = FileEditorManager.getInstance(project)
                fileEditorManager.openFiles.forEach { file ->
                    fileEditorManager.closeFile(file)
                }
                ProjectManager.getInstance().closeAndDispose(project)
            }
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: IllegalStateException) {
            LOG.warn("[applescript-smoke] close project failed", exception)
        } catch (exception: IllegalArgumentException) {
            LOG.warn("[applescript-smoke] close project failed", exception)
        }
    }

    /**
     * Project bootstrap: [ProjectUtil.openOrImport] is the primary path (mirrors the
     * File -> Open lifecycle). Falls back to
     * [ProjectManagerEx.openProject] only if the primary returns null. Both options land
     * us with a real [Project] + [PsiManager] for the assertions.
     *
     * Public-API-only fallback: uses the no-arg static [OpenProjectTask.build] factory
     * (published in `ide-core-impl/api-dump.txt` — the vetted surface), NOT the internal
     * Kotlin-DSL `OpenProjectTask { ... }` extension that lives in
     * `com.intellij.ide.impl.OpenProjectTaskKt` and triggers `INTERNAL_API_USAGES`.
     */
    private fun openOrCreateProject(fixtureDir: Path): Project? {
        val primary =
            try {
                ProjectUtil.openOrImport(fixtureDir, null, true)
            } catch (exception: ProcessCanceledException) {
                throw exception
            } catch (exception: IllegalStateException) {
                LOG.warn("ProjectUtil.openOrImport threw for $fixtureDir", exception)
                null
            } catch (exception: IllegalArgumentException) {
                LOG.warn("ProjectUtil.openOrImport threw for $fixtureDir", exception)
                null
            }
        if (primary != null) return primary

        // Fallback path - useful if openOrImport semantics shift between platform minor
        // versions. OpenProjectTask.build() returns a default-configured task that
        // matches the previous `OpenProjectTask {}` empty-DSL call site (no
        // customizations needed for the smoke fixture).
        return try {
            ProjectManagerEx.getInstanceEx().openProject(fixtureDir, OpenProjectTask.build())
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: IllegalStateException) {
            LOG.warn("ProjectManagerEx.openProject fallback threw for $fixtureDir", exception)
            null
        } catch (exception: IllegalArgumentException) {
            LOG.warn("ProjectManagerEx.openProject fallback threw for $fixtureDir", exception)
            null
        }
    }
}

private class AppleScriptSmokeAssertions {
    fun run(
        project: Project,
        fixtureDir: File,
        failures: MutableList<String>,
    ) {
        val playFile = File(fixtureDir, "play.applescript")
        val unresolvedFile = File(fixtureDir, "unresolved-app.applescript")

        ApplicationManager.getApplication().invokeAndWait {
            assertComposite2TokenFallback(project, playFile, failures)
            assertCompletionOnPlay(project, playFile, failures)
            assertWeakWarningOnUnresolvedApp(project, unresolvedFile, failures)
        }
    }

    // --- Assertion 1 — composite 2-token fallback (Phase 8 D-15) ---------------------

    private fun assertComposite2TokenFallback(
        project: Project,
        file: File,
        failures: MutableList<String>,
    ) {
        val psiFile = loadPsiFile(project, file, failures) ?: return
        val errors =
            PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java).toList()
        if (errors.isNotEmpty()) {
            val sample = errors.first().errorDescription
            failures.add(
                "composite 2-token fallback broken in ${file.name}: " +
                    "${errors.size} PsiErrorElement(s); first='$sample'",
            )
        }
    }

    // --- Assertion 2 — BASIC completion non-empty on `play ` -------------------------

    private fun assertCompletionOnPlay(
        project: Project,
        file: File,
        failures: MutableList<String>,
    ) {
        val context = loadEditorContext(project, file, failures, "completion") ?: return
        val playOffset = context.document.charsSequence.indexOf("play ")
        if (playOffset < 0) {
            failures.add("completion: 'play ' token not found in ${file.name}")
        } else {
            val caret = playOffset + "play ".length
            val descriptor = OpenFileDescriptor(project, context.virtualFile, caret)
            val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            if (editor == null) {
                failures.add("completion: editor could not be opened for ${file.name}")
            } else {
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
        }
    }

    // --- Assertion 3 — WEAK_WARNING (NOT WARNING) on unresolved app ------------------

    private fun assertWeakWarningOnUnresolvedApp(
        project: Project,
        file: File,
        failures: MutableList<String>,
    ) {
        val context = loadEditorContext(project, file, failures, "annotator") ?: return
        val descriptor = OpenFileDescriptor(project, context.virtualFile, 0)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)

        // Trigger the daemon and drain the EDT queue so public editor markup is populated.
        // The exact WEAK_WARNING severity is asserted in AppleScriptCodeInsightTest via
        // test-fixture HighlightInfo; this runtime smoke stays on public editor APIs.
        DaemonCodeAnalyzer.getInstance(project).restart(context.psiFile)
        repeat(DAEMON_DRAIN_ITERATIONS) {
            UIUtil.dispatchAllInvocationEvents()
        }

        val targetText = "NoSuchApp_xyz"
        val targetOffset = context.document.charsSequence.indexOf(targetText)
        if (targetOffset < 0) {
            failures.add("annotator: '$targetText' not found in ${file.name}")
        } else {
            val targetRange = TextRange(targetOffset, targetOffset + targetText.length)

            val markup = DocumentMarkupModel.forDocument(context.document, project, false)
            val overlapping =
                markup.allHighlighters.filter { highlighter ->
                    targetRange.intersects(highlighter.startOffset, highlighter.endOffset)
                }
            val warnCount = overlapping.count { it.layer == HighlighterLayer.WARNING }
            val errorCount = overlapping.count { it.layer == HighlighterLayer.ERROR }

            if (warnCount > 0 || errorCount > 0) {
                val highlighterDetails =
                    overlapping.joinToString { highlighter ->
                        "layer=${highlighter.layer}, tooltip=${highlighter.errorStripeTooltip}"
                    }
                failures.add(
                    "Phase 8 D-15 broken: expected no WARNING/ERROR-layer highlighter over " +
                        "'$targetText' in ${file.name}, got warnings=$warnCount, errors=$errorCount " +
                        "(total overlapping=${overlapping.size}, highlighters=[$highlighterDetails])",
                )
            }
        }
    }

    // --- Helpers ---------------------------------------------------------------------

    private fun loadPsiFile(
        project: Project,
        file: File,
        failures: MutableList<String>,
    ): PsiFile? {
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

    private fun loadEditorContext(
        project: Project,
        file: File,
        failures: MutableList<String>,
        featureName: String,
    ): EditorContext? {
        var context: EditorContext? = null
        val psiFile = loadPsiFile(project, file, failures)
        if (psiFile != null) {
            val virtualFile = psiFile.virtualFile
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            when {
                virtualFile == null -> failures.add("$featureName: virtualFile null for ${file.name}")
                document == null -> failures.add("$featureName: document null for ${file.name}")
                else -> context = EditorContext(psiFile, virtualFile, document)
            }
        }
        return context
    }

    private data class EditorContext(
        val psiFile: PsiFile,
        val virtualFile: VirtualFile,
        val document: Document,
    )
}
