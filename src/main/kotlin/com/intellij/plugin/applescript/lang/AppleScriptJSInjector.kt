package com.intellij.plugin.applescript.lang

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.psi.AppleScriptStringLiteralExpression
import com.intellij.plugin.applescript.psi.sdef.AppleScriptCommandHandlerCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil

/** Injects JavaScript into the string argument of `do javascript` Cocoa commands. */
class AppleScriptJSInjector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is AppleScriptStringLiteralExpression) return
        val asCommand = PsiTreeUtil.getContextOfType(context, AppleScriptCommandHandlerCall::class.java) ?: return
        if (!asCommand.getCommandName().equals("do javascript", ignoreCase = true)) return

        val javascript = Language.findInstancesByMimeType("javascript")
        if (javascript.isEmpty()) return

        registrar.startInjecting(javascript.iterator().next())
            .addPlace(null, null, context as PsiLanguageInjectionHost, TextRange(1, context.getTextLength() - 1))
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> = listOf(AppleScriptStringLiteralExpression::class.java)
}
