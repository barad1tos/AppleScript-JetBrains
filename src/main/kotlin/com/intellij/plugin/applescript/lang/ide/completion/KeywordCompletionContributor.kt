package com.intellij.plugin.applescript.lang.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns
import com.intellij.plugin.applescript.AppleScriptFile
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiComment
import com.intellij.util.ProcessingContext

class KeywordCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            psiElement().inFile(StandardPatterns.instanceOf(AppleScriptFile::class.java)),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    completionParameters: CompletionParameters,
                    processingContext: ProcessingContext,
                    completionResultSet: CompletionResultSet,
                ) {
                    val file = completionParameters.originalFile
                    if (file !is AppleScriptFile) return
                    val position = completionParameters.position
                    if (position is PsiComment) return

                    val node = position.node
                    if (node.elementType === AppleScriptTypes.STRING_LITERAL) return

                    for (kwElem in AppleScriptTokenTypesSets.KEYWORDS.types) {
                        completionResultSet.addElement(
                            LookupElementBuilder
                                .create(kwElem.toString().lowercase().replace("_", " "))
                                .bold()
                                .withTypeText("keyword", true),
                        )
                    }
                }
            },
        )
    }
}
