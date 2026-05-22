package com.intellij.plugin.applescript.psi

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * PSI element representing a definition/declaration.
 */
interface AppleScriptComponent : AppleScriptNamedElement, PsiNameIdentifierOwner {

    fun isScriptProperty(): Boolean

    fun isHandler(): Boolean

    /**
     * @return original declaration of this element. For example variable which was declared as a property but
     * later used in a set statement.
     */
    fun getOriginalDeclaration(): PsiElement?

    /** True if this is a value-object property (AppleScript record or application object property). */
    fun isObjectProperty(): Boolean

    /**
     * True if this target component is a single variable (not a list or record of variables) — local or global
     * variable declaration, or single variable in a creation statement.
     */
    fun isVariable(): Boolean

    fun findAssignedValue(): AppleScriptExpression?

    override fun getPresentation(): ItemPresentation?
}
