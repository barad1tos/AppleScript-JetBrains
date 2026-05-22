package com.intellij.plugin.applescript.psi

import com.intellij.psi.PsiElement

interface AppleScriptPsiObject : AppleScriptComponent {

    /** Object properties — see AppleScriptObjectPropertyDeclaration. */
    fun getProperties(): List<PsiElement>

    fun getProperty(name: String): PsiElement?
}
