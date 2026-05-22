package com.intellij.plugin.applescript.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptTargetReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult

class AppleScriptTargetReferenceImpl(private val myElement: AppleScriptComponent) :
    AppleScriptReferenceElementImpl(myElement.node),
    AppleScriptTargetReference {

    override fun getRangeInElement(): TextRange {
        val nameNode = myElement.nameIdentifier?.node ?: node
        return nameNode.textRange.shiftRight(-myElement.node.startOffset)
    }

    override fun getTextRange(): TextRange = super.getTextRange()

    override fun getTextOffset(): Int = myElement.textOffset

    override fun resolve(): PsiElement? {
        val results = multiResolve(true)
        return when {
            results.isEmpty() -> null
            !results[0].isValidResult -> null
            else -> results[0].element
        }
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val results = super.multiResolve(incompleteCode)
        return if (results.isNotEmpty()) results else arrayOf(PsiElementResolveResult(myElement))
    }

    override fun getElement(): PsiElement = myElement
}
