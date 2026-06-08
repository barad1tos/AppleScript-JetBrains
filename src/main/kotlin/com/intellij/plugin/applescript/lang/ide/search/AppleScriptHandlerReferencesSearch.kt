package com.intellij.plugin.applescript.lang.ide.search

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor

class AppleScriptHandlerReferencesSearch : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ): Boolean =
        ApplicationManager.getApplication().runReadAction(
            Computable { doExecute(queryParameters, consumer) },
        )

    private fun doExecute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        // The element returned is the first immediate PsiNamedElement up the hierarchy from the caret (as for
        // Find Usages). Extensions that redefine this so a declaration resolves the whole method are:
        //  1. TargetElementEvaluator#getElementByReference / TargetElementEvaluatorEx2#getNamedElement
        //  2. PomDeclarationSearcher#findDeclarationsAt
        val element = queryParameters.elementToSearch
        val handler = element as? AppleScriptHandler

        val parts = handler?.getParameters().orEmpty()
        return if (handler == null || parts.isEmpty()) {
            true
        } else {
            val handlerSelector = handler.getSelector()
            val helper = PsiSearchHelper.getInstance(element.project)
            val searchWord = parts[0].getSelectorPart().removeSuffix(":")

            searchWord.isEmpty() ||
                helper.processElementsWithWord(
                    HandlerOccurrenceProcessor(handler, handlerSelector, consumer),
                    queryParameters.effectiveSearchScope,
                    searchWord,
                    UsageSearchContext.IN_CODE,
                    true,
                )
        }
    }

    private class HandlerOccurrenceProcessor(
        private val handler: AppleScriptHandler,
        private val handlerSelector: String,
        private val consumer: Processor<in PsiReference>,
    ) : TextOccurenceProcessor {
        override fun execute(
            element: PsiElement,
            offsetInElement: Int,
        ): Boolean {
            val reference =
                if (element is AppleScriptHandlerCall && handlerSelector == element.getHandlerSelector()) {
                    element.references.firstOrNull { it.isReferenceTo(handler) }
                } else {
                    null
                }
            return reference?.let(consumer::process) ?: true
        }
    }
}
