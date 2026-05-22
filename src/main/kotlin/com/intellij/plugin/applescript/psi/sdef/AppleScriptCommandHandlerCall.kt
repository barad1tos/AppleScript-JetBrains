package com.intellij.plugin.applescript.psi.sdef

import com.intellij.plugin.applescript.psi.AppleScriptExpression
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement

interface AppleScriptCommandHandlerCall :
    AppleScriptPsiElement,
    DictionaryCompositeElement,
    AppleScriptExpression {

    fun getCommandName(): String

    fun getDirectParameter(): PsiElement?

    fun getCommandParameters(): List<AppleScriptCommandHandlerParameter>?
}
