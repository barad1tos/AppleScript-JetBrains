package com.intellij.plugin.applescript.lang.resolve

import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptReferenceElement
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.HANDLER_DEFINITIONS
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil

object AppleScriptComponentScopeResolver :
    ResolveCache.AbstractResolver<AppleScriptReferenceElement, List<PsiElement>> {

    override fun resolve(scopeElement: AppleScriptReferenceElement, incompleteCode: Boolean): List<PsiElement> {
        val resultSet = HashSet<AppleScriptComponent>()
        // Local scope.
        val maxScope = getMaxScope(scopeElement)
        val resolveProcessor = AppleScriptComponentScopeProcessor(resultSet)
        PsiTreeUtil.treeWalkUp(resolveProcessor, scopeElement, maxScope, ResolveState.initial())
        return ArrayList(resultSet)
    }

    /** @return the maximum resolve scope for [scopeElement], or null if none applies. */
    private fun getMaxScope(scopeElement: AppleScriptReferenceElement): PsiElement? =
        getHandlerDefinitionScope(scopeElement.context) // currently only handler definitions are processed

    private fun getHandlerDefinitionScope(scopeElement: PsiElement?): PsiElement? {
        var currentScope: PsiElement? = scopeElement
        while (currentScope != null) {
            if (HANDLER_DEFINITIONS.contains(currentScope.node.elementType)) return currentScope
            currentScope = currentScope.context
        }
        return null
    }

    @Suppress("unused")
    private fun isInsideHandlerDefinition(context: PsiElement?): Boolean {
        var cursor = context
        while (cursor != null) {
            if (HANDLER_DEFINITIONS.contains(cursor.node.elementType)) return true
            cursor = cursor.context
        }
        return false
    }
}
