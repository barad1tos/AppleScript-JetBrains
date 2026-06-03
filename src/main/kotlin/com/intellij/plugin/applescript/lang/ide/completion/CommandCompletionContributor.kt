package com.intellij.plugin.applescript.lang.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.psi.sdef.AppleScriptCommandHandlerCall
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class CommandCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    // PITFALLS Pattern J — integration lifecycle gate. Short-circuit BEFORE any
                    // expensive symbol enumeration when the app catalog isn't ready yet.
                    // PITFALLS §7.1 prevention pattern: register restart so the IDE re-triggers
                    // completion automatically once `appsReady` completes mid-session (Context7
                    // SDK explicit; `CompletionResultSet.restartCompletionWhenNothingMatches()`
                    // is part of the public IntelliJ Platform API).
                    val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
                    if (!registryService.areAppDictionariesIndexed()) {
                        result.restartCompletionWhenNothingMatches()
                        return
                    }
                    val handlerCallExpression = findCommandHandlerCall(parameters)
                    val target = handlerCallExpression?.reference?.resolve()
                    if (target is AppleScriptCommand) {
                        for (par in target.parameters.sortedBy { it.isOptional }) {
                            result.addElement(
                                LookupElementBuilder
                                    .create(par)
                                    .withBoldness(!par.isOptional)
                                    .withIcon(par.getIcon(0)),
                            )
                        }
                        result.stopHere()
                    }
                }
            },
        )
    }
}

private fun findCommandHandlerCall(parameters: CompletionParameters): AppleScriptCommandHandlerCall? {
    val handlerCallExpression =
        PsiTreeUtil.getParentOfType(
            parameters.position,
            AppleScriptCommandHandlerCall::class.java,
        ) ?: previousCommandHandlerCall(parameters)
            ?: return null

    return handlerCallExpression.takeIf { it.isOnCaretLine(parameters) }
}

private fun previousCommandHandlerCall(parameters: CompletionParameters): AppleScriptCommandHandlerCall? {
    var previousSibling = parameters.originalFile.findElementAt(parameters.offset)?.prevSibling
    while (previousSibling != null && previousSibling.node.elementType === TokenType.WHITE_SPACE) {
        previousSibling = previousSibling.prevSibling
    }
    return previousSibling as? AppleScriptCommandHandlerCall
}

private fun AppleScriptCommandHandlerCall.isOnCaretLine(parameters: CompletionParameters): Boolean {
    val document = parameters.editor.document
    return document.getLineNumber(textOffset) == document.getLineNumber(parameters.offset)
}
