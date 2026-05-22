package com.intellij.plugin.applescript.lang.ide.search

import com.intellij.openapi.application.ReadAction
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
    ): Boolean = ReadAction.compute<Boolean, RuntimeException> { doExecute(queryParameters, consumer) }

    private fun doExecute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        // The element returned is the first immediate PsiNamedElement up the hierarchy from the caret (as for
        // Find Usages). Extensions that redefine this so a declaration resolves the whole method are:
        //  1. TargetElementEvaluator#getElementByReference / TargetElementEvaluatorEx2#getNamedElement
        //  2. PomDeclarationSearcher#findDeclarationsAt
        val element = queryParameters.elementToSearch
        val handler = element as? AppleScriptHandler ?: return true

        val parts = handler.getParameters()
        if (parts.isEmpty()) return true
        val handlerSelector = handler.getSelector()
        val helper = PsiSearchHelper.getInstance(element.project)

        val firstSelector = parts[0].getSelectorPart()
        val searchWord = if (firstSelector.endsWith(":")) firstSelector.substring(0, firstSelector.length - 1) else firstSelector

        return searchWord.isEmpty() || helper.processElementsWithWord(
            MyOccurrenceProcessor(handler, handlerSelector, consumer),
            queryParameters.effectiveSearchScope,
            searchWord,
            UsageSearchContext.IN_CODE,
            true,
        )
    }

    private class MyOccurrenceProcessor(
        private val myHandler: AppleScriptHandler,
        private val myHandlerSelector: String,
        private val myConsumer: Processor<in PsiReference>,
    ) : TextOccurenceProcessor {

        override fun execute(element: PsiElement, offsetInElement: Int): Boolean {
            if (element is AppleScriptHandlerCall) {
                val selector = element.getHandlerSelector()
                if (myHandlerSelector == selector) {
                    for (ref in element.references) {
                        if (ref.isReferenceTo(myHandler)) {
                            return myConsumer.process(ref)
                        }
                    }
                }
            }
            return true
        }
    }
}
