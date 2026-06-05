package com.intellij.plugin.applescript.lang.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.SystemInfo
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.files.SdefFileProvider
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
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
                    // PITFALLS Pattern J — integration lifecycle gate. Short-circuit BEFORE the
                    // app-name enumeration when the catalog isn't ready. PITFALLS §7.1 prevention
                    // pattern: register restart so the IDE re-triggers completion automatically
                    // once `appsReady` completes mid-session (Context7 SDK explicit).
                    if (!systemDictionaryRegistry.areAppDictionariesIndexed()) {
                        result.restartCompletionWhenNothingMatches()
                        return
                    }
                    val appNameList = ArrayList<String>()
                    val persistenceService = SdefPersistenceService.getInstance()
                    if (SystemInfo.isMac) {
                        appNameList.addAll(ApplicationDiscoveryService.getInstance().getDiscoveredApplicationNames())
                        appNameList.removeAll(persistenceService.notScriptableSnapshot)
                        appNameList.removeAll(SdefFileProvider.getInstance().getScriptingAdditions())
                        appNameList.remove(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
                        appNameList.remove(ApplicationDictionary.COCOA_STANDARD_LIBRARY)
                    } else {
                        appNameList.addAll(persistenceService.cachedApplicationNamesSnapshot)
                    }
                    for (appName in appNameList) {
                        result.addElement(LookupElementBuilder.create(appName))
                    }
                }
            },
        )
    }
}
