package com.intellij.plugin.applescript.lang

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.psi.AppleScriptStringLiteralExpression
import com.intellij.plugin.applescript.psi.sdef.AppleScriptCommandHandlerCall
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

private const val DO_JAVASCRIPT_COMMAND = "do javascript"
private const val JAVASCRIPT_MIME_TYPE = "javascript"
private const val STRING_DELIMITER_LENGTH = 1

/** Injects JavaScript into the string argument of `do javascript` Cocoa commands. */
class AppleScriptJSInjector : MultiHostInjector {
    override fun getLanguagesToInject(
        registrar: MultiHostRegistrar,
        context: PsiElement,
    ) {
        val stringLiteral = context as? AppleScriptStringLiteralExpression ?: return
        val command =
            PsiTreeUtil.getContextOfType(
                stringLiteral,
                AppleScriptCommandHandlerCall::class.java,
            )
        val isDoJavascriptCommand =
            command
                ?.getCommandName()
                ?.equals(DO_JAVASCRIPT_COMMAND, ignoreCase = true) == true
        val javascriptLanguages = Language.findInstancesByMimeType(JAVASCRIPT_MIME_TYPE)
        val javascript = javascriptLanguages.firstOrNull()

        if (isDoJavascriptCommand && javascript != null) {
            registrar.startInjecting(javascript)
            registrar.addPlace(
                null,
                null,
                stringLiteral,
                TextRange(
                    STRING_DELIMITER_LENGTH,
                    stringLiteral.textLength - STRING_DELIMITER_LENGTH,
                ),
            )
            registrar.doneInjecting()
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(
            AppleScriptStringLiteralExpression::class.java,
        )
}
