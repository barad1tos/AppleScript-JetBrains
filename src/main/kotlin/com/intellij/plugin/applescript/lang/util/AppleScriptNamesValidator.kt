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
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.refactoring.rename.PsiElementRenameHandler

class AppleScriptNamesValidator : NamesValidator {
    override fun isKeyword(
        name: String,
        project: Project?,
    ): Boolean = isKeyword(name)

    private fun isKeyword(name: String): Boolean {
        val tokenType = getTokenType(name)
        return AppleScriptTokenTypesSets.KEYWORDS.contains(tokenType)
    }

    private fun getTokenType(name: String): IElementType? {
        val lexer = AppleScriptLexerAdapter()
        lexer.start(name)
        val tokenType = lexer.tokenType
        lexer.advance()
        return if (lexer.tokenType == null) tokenType else null
    }

    override fun isIdentifier(
        name: String,
        project: Project?,
    ): Boolean =
        // KEEP (Phase 8 / v2.0 backlog: BL-E6): the rename-handler probe below is a workaround for
        // multi-part handler names; removing it cleanly needs a dedicated rename handler plus
        // change-signature support, which alters refactoring behaviour. Out of the v1.x
        // cleanup scope (behaviour-preserving only).
        isIdentifier(name) || (project != null && isRenamingHandlerWithValidName(name, project))

    private fun isRenamingHandlerWithValidName(
        name: String,
        project: Project,
    ): Boolean {
        val oldName = (getElementToRename(project) as? AppleScriptHandler)?.name
        val newParts = name.split(":")
        val oldParts = oldName?.split(":")
        return oldParts != null &&
            oldParts.size == newParts.size &&
            newParts.all(::isIdentifier)
    }

    private fun getElementToRename(project: Project): PsiElement? {
        val editor = selectedTextEditor(project) ?: return null
        val dataContext = DataManager.getInstance().getDataContext(editor.component)
        return PsiElementRenameHandler.getElement(dataContext)
    }

    private fun selectedTextEditor(project: Project) =
        if (ApplicationManager.getApplication().isDispatchThread) {
            FileEditorManager.getInstance(project).selectedTextEditor
        } else {
            null
        }

    private fun isIdentifier(name: String): Boolean = getTokenType(name) === AppleScriptTypes.VAR_IDENTIFIER
}
