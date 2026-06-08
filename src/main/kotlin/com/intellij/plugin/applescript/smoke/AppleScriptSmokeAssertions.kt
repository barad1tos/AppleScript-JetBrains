package com.intellij.plugin.applescript.smoke

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.ide.structure.AppleScriptStructureViewModel
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ui.UIUtil
import java.io.File

private const val DAEMON_DRAIN_ITERATIONS: Int = 50

/**
 * Runs end-to-end smoke assertions for parser fallback, completion, and unresolved
 * application diagnostics. PSI and daemon interactions run inside
 * `ApplicationManager.invokeAndWait { ... }` to keep the EDT contract explicit.
 *
 * The unresolved-application assertion uses [DocumentMarkupModel.forDocument] and
 * [HighlighterLayer] constants to inspect daemon-produced public editor markup instead
 * of internal daemon implementation APIs.
 */
internal class AppleScriptSmokeAssertions {
    fun run(
        project: Project,
        fixtureDir: File,
        failures: MutableList<String>,
    ) {
        val playFile = File(fixtureDir, "play.applescript")
        val structureFile = File(fixtureDir, "structure.applescript")
        val unresolvedFile = File(fixtureDir, "unresolved-app.applescript")

        ApplicationManager.getApplication().invokeAndWait {
            assertComposite2TokenFallback(project, playFile, failures)
            assertStructureView(project, structureFile, failures)
            assertCompletionOnPlay(project, playFile, failures)
            assertWeakWarningOnUnresolvedApp(project, unresolvedFile, failures)
        }
    }

    // Assertion 1: composite two-token fallback.

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

    // Assertion 2: Structure View exposes top-level script declarations.

    private fun assertStructureView(
        project: Project,
        file: File,
        failures: MutableList<String>,
    ) {
        val context = loadEditorContext(project, file, failures, "structure") ?: return
        val editor =
            FileEditorManager.getInstance(project).openTextEditor(
                OpenFileDescriptor(project, context.virtualFile, 0),
                true,
            )
        if (editor == null) {
            failures.add("structure: editor could not be opened for ${file.name}")
            return
        }

        val model = AppleScriptStructureViewModel(context.psiFile, editor)
        val childNames =
            model.root.children
                .mapNotNull { child -> child.presentation.presentableText }

        if (
            childNames.none { childName -> childName.startsWith("scriptName") } ||
            childNames.none { childName -> childName.startsWith("run") } ||
            childNames.none { childName -> childName == "Worker" }
        ) {
            failures.add(
                "structure: expected scriptName, run, and Worker in ${file.name}; got $childNames",
            )
        }
    }

    // Assertion 3: BASIC completion is non-empty on `play `.

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

    // Assertion 4: unresolved application references stay below WARNING severity.

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
        DaemonCodeAnalyzer.getInstance(project).settingsChanged()
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
