package com.intellij.plugin.applescript.lang.resolve

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.plugin.applescript.psi.AppleScriptHandlerSelectorPart
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.ThreeState

class AppleScriptTargetElementEvaluator : TargetElementEvaluatorEx2() {

    override fun getNamedElement(element: PsiElement): PsiElement? {
        val parent = element.parent
        if (parent is AppleScriptHandlerSelectorPart) {
            if (element !== parent.findParameterNode()) {
                return parent.parent // AppleScriptHandler
            }
        }
        return null
    }

    override fun isAcceptableReferencedElement(
        element: PsiElement,
        referenceOrReferencedElement: PsiElement?,
    ): ThreeState = super.isAcceptableReferencedElement(element, referenceOrReferencedElement)

    override fun includeSelfInGotoImplementation(element: PsiElement): Boolean =
        super.includeSelfInGotoImplementation(element)

    override fun getElementByReference(ref: PsiReference, flags: Int): PsiElement? = null
}
