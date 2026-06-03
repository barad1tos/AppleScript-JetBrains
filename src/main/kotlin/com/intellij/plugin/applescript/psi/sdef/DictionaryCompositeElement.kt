package com.intellij.plugin.applescript.psi.sdef

import com.intellij.plugin.applescript.psi.AppleScriptPsiElement

interface DictionaryCompositeElement : AppleScriptPsiElement {
    override fun getReference(): DictionaryReference

    fun getCompositeNameElement(): DictionaryCompositeName
}
