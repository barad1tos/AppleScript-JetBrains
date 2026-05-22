package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.PsiElement

class AppleScriptClassType : PsiType("class") {

    @Suppress("unused")
    internal fun resolve(): PsiElement? = null
}
