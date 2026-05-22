package com.intellij.plugin.applescript.psi.sdef

import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement

interface DictionaryCompositeName : AppleScriptPsiElement {

    fun getIdentifiers(): List<PsiElement>

    fun getCompositeName(): String
}
