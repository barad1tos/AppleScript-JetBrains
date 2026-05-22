package com.intellij.plugin.applescript.lang.util

import com.intellij.plugin.applescript.lang.parser.AppleScriptParserDefinition
import com.intellij.plugin.applescript.psi.AppleScriptReferenceElement
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.HANDLER_DEFINITIONS
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiElement

object ScopeUtil {

    @JvmStatic
    fun getMaxLocalScopeForTargetOrReference(element: PsiElement?): PsiElement? {
        if (element == null) return null
        if (element !is AppleScriptTargetVariable && element !is AppleScriptReferenceElement) return null
        // Local scope: handler definition, current file, object declaration.
        var currentScope: PsiElement? = element.context
        var roofReached = false
        while (!roofReached && currentScope != null) {
            val elementType = currentScope.node.elementType
            // Original Java compares element (PsiElement) to AppleScriptTypes.OBJECT_TARGET_PROPERTY_DECLARATION
            // (IElementType) — always false. Preserved verbatim per 1:1 port; will be cleaned up in v1.1.
            @Suppress("EqualsBetweenInconvertibleTypes")
            val isObjectTargetDecl = element == AppleScriptTypes.OBJECT_TARGET_PROPERTY_DECLARATION
            roofReached = HANDLER_DEFINITIONS.contains(elementType) ||
                elementType === AppleScriptParserDefinition.FILE ||
                isObjectTargetDecl
            if (!roofReached) currentScope = currentScope.context
        }
        return currentScope
    }
}
