package com.intellij.plugin.applescript.lang.ide.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.psi.AppleScriptHandlerParameterLabel
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

class RenameParameterLabelQuickFix(
    private val myHandlerParameterLabel: AppleScriptHandlerParameterLabel,
    private val myNewLabelName: String,
) : IntentionAction {
    override fun getFamilyName(): String = "AppleScript"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ): Boolean = file?.fileType is AppleScriptFileType

    @Throws(IncorrectOperationException::class)
    override fun invoke(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                CommandProcessor.getInstance().executeCommand(
                    project,
                    {
                        val newLabel =
                            AppleScriptPsiElementFactory.createHandlerParameterLabel(
                                project,
                                myNewLabelName,
                            )
                        if (newLabel != null) {
                            myHandlerParameterLabel.replace(newLabel)
                        }
                    },
                    RENAME_PARAMETER_LABEL_COMMAND,
                    null,
                )
            }
        }
    }

    override fun getText(): String = "Rename parameter label"

    override fun startInWriteAction(): Boolean = false

    private companion object {
        private const val RENAME_PARAMETER_LABEL_COMMAND = "Rename Parameter Label"
    }
}
