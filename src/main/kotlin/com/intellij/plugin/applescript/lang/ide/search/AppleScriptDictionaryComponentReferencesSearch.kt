package com.intellij.plugin.applescript.lang.ide.search

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.sdef.AppleScriptCommandHandlerCall
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor

class AppleScriptDictionaryComponentReferencesSearch : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
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
        val element = queryParameters.elementToSearch
        val dictionaryComponent = element as? DictionaryComponent

        val parts = dictionaryComponent?.nameIdentifiers.orEmpty()
        return if (dictionaryComponent == null || parts.isEmpty()) {
            true
        } else {
            val componentName = dictionaryComponent.getName()
            val helper = PsiSearchHelper.getInstance(element.project)

            val searchWord = parts[0]
            searchWord.isEmpty() ||
                helper.processElementsWithWord(
                    DictionaryComponentOccurrenceProcessor(dictionaryComponent, componentName, consumer),
                    queryParameters.scopeDeterminedByUser,
                    searchWord,
                    UsageSearchContext.IN_CODE,
                    true,
                )
        }
    }

    private class DictionaryComponentOccurrenceProcessor(
        private val dictionaryComponent: DictionaryComponent,
        private val componentName: String,
        private val consumer: Processor<in PsiReference>,
    ) : TextOccurenceProcessor {
        override fun execute(
            element: PsiElement,
            offsetInElement: Int,
        ): Boolean {
            val selector: String? =
                when (element) {
                    is AppleScriptCommandHandlerCall -> element.getCommandName()
                    is DictionaryCompositeElement -> element.getCompositeNameElement().getCompositeName()
                    else -> null
                }
            if (selector != null && componentName == selector) {
                for (reference in element.references) {
                    if (reference.isReferenceTo(dictionaryComponent)) {
                        return consumer.process(reference)
                    }
                }
            }
            return true
        }
    }
}
