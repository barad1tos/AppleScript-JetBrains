package com.intellij.plugin.applescript.lang.resolve

import com.intellij.plugin.applescript.psi.AppleScriptReferenceElement
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil

object AppleScriptResolver : ResolveCache.AbstractResolver<AppleScriptReferenceElement, List<PsiElement>> {

    private fun resolveSimpleReference(scopeElement: PsiElement, name: String): List<PsiElement> {
        val resolveProcessor = AppleScriptResolveProcessor(name)
        PsiTreeUtil.treeWalkUp(resolveProcessor, scopeElement, null, ResolveState.initial())
        return listOfNotNull(resolveProcessor.getResult())
    }

    override fun resolve(reference: AppleScriptReferenceElement, incompleteCode: Boolean): List<PsiElement> =
        resolveSimpleReference(reference, reference.canonicalText)
}
