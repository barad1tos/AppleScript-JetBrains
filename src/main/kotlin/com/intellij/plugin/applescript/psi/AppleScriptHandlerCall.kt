package com.intellij.plugin.applescript.psi

import com.intellij.psi.PsiReference

interface AppleScriptHandlerCall : AppleScriptPsiElement {
    fun getHandlerSelector(): String

    override fun getReference(): PsiReference

    fun getArguments(): List<AppleScriptHandlerArgument>
}
