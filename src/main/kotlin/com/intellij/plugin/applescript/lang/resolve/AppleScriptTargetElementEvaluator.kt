package com.intellij.plugin.applescript.lang.resolve

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.plugin.applescript.psi.AppleScriptHandlerSelectorPart
import com.intellij.psi.PsiElement

class AppleScriptTargetElementEvaluator : TargetElementEvaluatorEx2() {
    override fun getNamedElement(element: PsiElement): PsiElement? {
        val parent = element.parent as? AppleScriptHandlerSelectorPart ?: return null
        return if (element.node !== parent.findParameterNode()) parent.parent else null
    }
}
