package com.intellij.plugin.applescript.lang.ide.search

import com.intellij.openapi.application.ReadAction
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
    ): Boolean = ReadAction.compute<Boolean, RuntimeException> { doExecute(queryParameters, consumer) }

    private fun doExecute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        val element = queryParameters.elementToSearch
        val dictionaryComponent = element as? DictionaryComponent ?: return true

        val parts = dictionaryComponent.getNameIdentifiers()
        if (parts.isEmpty()) return true
        val componentName = dictionaryComponent.getName()
        val helper = PsiSearchHelper.getInstance(element.project)

        val searchWord = parts[0]
        return searchWord.isEmpty() || helper.processElementsWithWord(
            MyOccurrenceProcessor(dictionaryComponent, componentName, consumer),
            queryParameters.scopeDeterminedByUser,
            searchWord,
            UsageSearchContext.IN_CODE,
            true,
        )
    }

    private class MyOccurrenceProcessor(
        private val myDictionaryComponent: DictionaryComponent,
        private val myComponentName: String,
        private val myConsumer: Processor<in PsiReference>,
    ) : TextOccurenceProcessor {

        override fun execute(element: PsiElement, offsetInElement: Int): Boolean {
            val selector: String? = when (element) {
                is AppleScriptCommandHandlerCall -> element.getCommandName()
                is DictionaryCompositeElement -> element.getCompositeNameElement().getCompositeName()
                else -> null
            }
            if (selector != null && myComponentName == selector) {
                for (ref in element.references) {
                    if (ref.isReferenceTo(myDictionaryComponent)) {
                        return myConsumer.process(ref)
                    }
                }
            }
            return true
        }
    }
}
