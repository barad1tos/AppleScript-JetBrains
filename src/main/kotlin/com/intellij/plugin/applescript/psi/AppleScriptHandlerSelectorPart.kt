package com.intellij.plugin.applescript.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface AppleScriptHandlerSelectorPart :
    AppleScriptNamedElement,
    PsiNameIdentifierOwner {
    fun findParameters(): List<AppleScriptComponent>

    fun getParameterName(): String?

    fun findParameterNode(): ASTNode?

    fun getParameter(): PsiElement?

    fun getSelectorPart(): String

    fun getSelectorNameIdentifier(): AppleScriptIdentifier
}
