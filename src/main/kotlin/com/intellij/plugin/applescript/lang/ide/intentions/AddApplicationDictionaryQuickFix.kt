package com.intellij.plugin.applescript.lang.ide.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.actions.LoadDictionaryAction
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

class AddApplicationDictionaryQuickFix(private val newApplicationName: String) : IntentionAction {

    override fun getText(): String = "Add Dictionary for Application"

    override fun getFamilyName(): String = "AppleScript"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        file?.fileType === AppleScriptFileType

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val vFile = LocalFileSystem.getInstance().findFileByPath("/Applications/")
        LoadDictionaryAction.openLoadDirectoryDialog(project, vFile, newApplicationName)
    }

    override fun startInWriteAction(): Boolean = false
}
