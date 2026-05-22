package com.intellij.plugin.applescript.lang.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.SystemInfo
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.AppleScriptApplicationReference
import com.intellij.util.ProcessingContext

class ApplicationNameCompletionContributor : CompletionContributor() {

    init {
        val inAppReferenceString = psiElement().withSuperParent(1, AppleScriptApplicationReference::class.java)
        extend(
            CompletionType.BASIC,
            inAppReferenceString,
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    val systemDictionaryRegistry = AppleScriptSystemDictionaryRegistryService.getInstance()
                    val appNameList = ArrayList<String>()
                    if (SystemInfo.isMac) {
                        appNameList.addAll(systemDictionaryRegistry.getDiscoveredApplicationNames())
                        appNameList.removeAll(systemDictionaryRegistry.getNotScriptableApplicationList())
                        appNameList.removeAll(systemDictionaryRegistry.getScriptingAdditions())
                        appNameList.remove(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
                        appNameList.remove(ApplicationDictionary.COCOA_STANDARD_LIBRARY)
                    } else {
                        appNameList.addAll(systemDictionaryRegistry.getCachedApplicationNames())
                    }
                    for (appName in appNameList) {
                        result.addElement(LookupElementBuilder.create(appName))
                    }
                }
            },
        )
    }
}
