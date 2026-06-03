package com.intellij.plugin.applescript.psi

import com.intellij.psi.PsiNamedElement

interface AppleScriptNamedElement :
    AppleScriptPsiElement,
    PsiNamedElement {
    fun getIdentifier(): AppleScriptIdentifier
}
