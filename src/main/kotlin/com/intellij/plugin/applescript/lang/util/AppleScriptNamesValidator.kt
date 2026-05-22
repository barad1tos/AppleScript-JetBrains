package com.intellij.plugin.applescript.lang.util

import com.intellij.ide.DataManager
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.AppleScriptLexerAdapter
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.tree.IElementType
import com.intellij.refactoring.rename.PsiElementRenameHandler

class AppleScriptNamesValidator : NamesValidator {

    override fun isKeyword(name: String, project: Project?): Boolean = isKeyword(name)

    private fun isKeyword(name: String): Boolean {
        val tokenType = getTokenType(name)
        return AppleScriptTokenTypesSets.KEYWORDS.contains(tokenType)
    }

    private fun getTokenType(name: String): IElementType? {
        val lexer = AppleScriptLexerAdapter()
        lexer.start(name)
        val tt = lexer.tokenType
        lexer.advance()
        return if (lexer.tokenType == null) tt else null
    }

    override fun isIdentifier(name: String, project: Project?): Boolean =
        // TODO: remove this hack via rename handler + change-signature refactoring.
        isIdentifier(name) || (project != null && isRenamingHandlerWithValidName(name, project))

    private fun isRenamingHandlerWithValidName(name: String, project: Project): Boolean {
        val editor = if (ApplicationManager.getApplication().isDispatchThread) {
            FileEditorManager.getInstance(project).selectedTextEditor
        } else {
            null
        } ?: return false

        val dataContext = DataManager.getInstance().getDataContext(editor.component)
        val elementToRename = PsiElementRenameHandler.getElement(dataContext)
        if (elementToRename !is AppleScriptHandler) return false

        val oldName = elementToRename.getName()
        val newParts = name.split(":")
        val oldParts = oldName?.split(":")
        if (oldParts == null || oldParts.size != newParts.size) return false

        for (part in newParts) {
            if (!isIdentifier(part)) return false
        }
        return true
    }

    private fun isIdentifier(name: String): Boolean =
        getTokenType(name) === AppleScriptTypes.VAR_IDENTIFIER
}
