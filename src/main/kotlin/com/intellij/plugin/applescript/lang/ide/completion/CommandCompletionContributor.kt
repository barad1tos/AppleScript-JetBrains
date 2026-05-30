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
import com.intellij.psi.PsiElement
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
                    var handlerCallExpression: AppleScriptCommandHandlerCall? =
                        PsiTreeUtil.getParentOfType(parameters.position, AppleScriptCommandHandlerCall::class.java)
                    val elemAtCaret: PsiElement? = parameters.originalFile.findElementAt(parameters.offset)
                    val currLine = parameters.editor.document.getLineNumber(parameters.offset)
                    var prevSibling = elemAtCaret?.prevSibling
                    while (prevSibling != null && prevSibling.node.elementType === TokenType.WHITE_SPACE) {
                        prevSibling = prevSibling.prevSibling
                    }
                    if (handlerCallExpression == null) {
                        handlerCallExpression = prevSibling as? AppleScriptCommandHandlerCall
                    }
                    if (handlerCallExpression == null) return

                    val handlerLine = parameters.editor.document.getLineNumber(handlerCallExpression.textOffset)
                    if (handlerLine != currLine) return

                    val target = handlerCallExpression.reference.resolve()
                    if (target is AppleScriptCommand) {
                        val sortedParams = ArrayList(target.parameters).apply {
                            sortWith { par1, par2 ->
                                val o1 = par1.isOptional
                                val o2 = par2.isOptional
                                when {
                                    o1 == o2 -> 0
                                    !o1 && o2 -> -1
                                    else -> 1
                                }
                            }
                        }
                        for (par in sortedParams) {
                            result.addElement(
                                LookupElementBuilder.create(par)
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
