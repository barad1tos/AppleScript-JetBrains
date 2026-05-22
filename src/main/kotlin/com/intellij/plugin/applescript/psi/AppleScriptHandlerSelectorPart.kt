package com.intellij.plugin.applescript.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.util.IncorrectOperationException

interface AppleScriptHandlerSelectorPart : AppleScriptNamedElement, PsiNameIdentifierOwner {

    fun findParameters(): List<AppleScriptComponent>

    override fun getReference(): PsiReference?

    fun getParameterName(): String?

    fun findParameterNode(): ASTNode?

    fun getParameter(): PsiElement?

    override fun getNameIdentifier(): PsiElement?

    override fun getIdentifier(): AppleScriptIdentifier

    fun getSelectorPart(): String

    override fun getPresentation(): ItemPresentation?

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement

    fun getSelectorNameIdentifier(): AppleScriptIdentifier
}
