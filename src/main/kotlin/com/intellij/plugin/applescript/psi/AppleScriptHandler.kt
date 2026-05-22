package com.intellij.plugin.applescript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

interface AppleScriptHandler : AppleScriptComponent {

    override fun isHandler(): Boolean

    override fun getName(): String?

    override fun getNameIdentifier(): PsiElement?

    override fun getReference(): PsiReference?

    fun getSelector(): String

    fun getSelectors(): List<AppleScriptIdentifier>

    fun getParameters(): List<AppleScriptHandlerSelectorPart>
}
