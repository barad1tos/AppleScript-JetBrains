package com.intellij.plugin.applescript.lang.ide.findUsages

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.plugin.applescript.psi.AppleScriptReferenceElement
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class AppleScriptReadWriteAccessDetector : ReadWriteAccessDetector() {

    override fun isReadWriteAccessible(element: PsiElement): Boolean =
        element is AppleScriptTargetVariable || element is AppleScriptReferenceElement

    override fun isDeclarationWriteAccess(element: PsiElement): Boolean = element is AppleScriptTargetVariable

    override fun getReferenceAccess(referencedElement: PsiElement, reference: PsiReference): Access =
        getExpressionAccess(reference.element)

    override fun getExpressionAccess(expression: PsiElement): Access {
        if (isDeclarationWriteAccess(expression)) return Access.Write
        return Access.Read
    }
}
