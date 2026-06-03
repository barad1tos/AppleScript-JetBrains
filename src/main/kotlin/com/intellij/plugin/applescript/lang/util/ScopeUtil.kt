package com.intellij.plugin.applescript.lang.util

import com.intellij.plugin.applescript.lang.parser.APPLE_SCRIPT_FILE_ELEMENT_TYPE
import com.intellij.plugin.applescript.psi.AppleScriptReferenceElement
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.HANDLER_DEFINITIONS
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiElement

object ScopeUtil {
    @JvmStatic
    fun getMaxLocalScopeForTargetOrReference(element: PsiElement?): PsiElement? {
        if (element !is AppleScriptTargetVariable && element !is AppleScriptReferenceElement) return null
        // Local scope: handler definition, current file, object target property declaration.
        var currentScope: PsiElement? = element.context
        var roofReached = false
        while (!roofReached && currentScope != null) {
            val elementType = currentScope.node.elementType
            val isObjectTargetDecl = elementType === AppleScriptTypes.OBJECT_TARGET_PROPERTY_DECLARATION
            roofReached = HANDLER_DEFINITIONS.contains(elementType) ||
                elementType === APPLE_SCRIPT_FILE_ELEMENT_TYPE ||
                isObjectTargetDecl
            if (!roofReached) currentScope = currentScope.context
        }
        return currentScope
    }
}
