package com.intellij.plugin.applescript.psi

import com.intellij.psi.PsiReference

interface AppleScriptReferenceElement :
    AppleScriptPsiElement,
    PsiReference {
    override fun getReference(): PsiReference = this

    override fun getCanonicalText(): String = text

    override fun isSoft(): Boolean = false
}
