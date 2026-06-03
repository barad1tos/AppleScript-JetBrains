package com.intellij.plugin.applescript.psi

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * PSI element representing a definition/declaration.
 */
interface AppleScriptComponent :
    AppleScriptNamedElement,
    PsiNameIdentifierOwner {
    fun isScriptProperty(): Boolean =
        node.findChildByType(AppleScriptTypes.PROP) != null ||
            node.findChildByType(AppleScriptTypes.PROPERTY) != null ||
            getOriginalDeclaration() is AppleScriptScriptPropertyDeclaration

    fun isHandler(): Boolean =
        this is AppleScriptHandlerPositionalParametersDefinition ||
            this is AppleScriptHandlerLabeledParametersDefinition ||
            this is AppleScriptHandlerInterleavedParametersDefinition

    /**
     * @return original declaration of this element. For example variable which was declared as a property but
     * later used in a set statement.
     */
    fun getOriginalDeclaration(): PsiElement? = reference?.resolve()

    /** True if this is a value-object property (AppleScript record or application object property). */
    fun isObjectProperty(): Boolean =
        context is AppleScriptRecordLiteralExpression ||
            context is AppleScriptTargetRecordLiteral

    /**
     * True if this target component is a single variable (not a list or record of variables) — local or global
     * variable declaration, or single variable in a creation statement.
     */
    fun isVariable(): Boolean =
        (
            node.findChildByType(AppleScriptTypes.LOCAL) != null ||
                node.findChildByType(AppleScriptTypes.GLOBAL) != null
        ) ||
            (this is AppleScriptTargetVariable && firstChild is AppleScriptIdentifier) ||
            this is AppleScriptVarDeclarationListPart ||
            context is AppleScriptLabeledParameterDeclarationList

    fun findAssignedValue(): AppleScriptExpression? =
        if (isScriptProperty() && this is AppleScriptScriptPropertyDeclaration) {
            expression
        } else {
            null
        }

    override fun getPresentation(): ItemPresentation?
}
