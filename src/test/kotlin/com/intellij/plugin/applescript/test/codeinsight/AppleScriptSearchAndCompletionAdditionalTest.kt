package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.completion.AppleScriptCompletionWeigher
import com.intellij.plugin.applescript.lang.ide.search.AppleScriptDictionaryComponentReferencesSearch
import com.intellij.plugin.applescript.lang.ide.search.AppleScriptHandlerDeclarationSearcher
import com.intellij.plugin.applescript.lang.ide.search.AppleScriptHandlerReferencesSearch
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptSearchAndCompletionAdditionalTest : BasePlatformTestCase() {
    fun testHandlerReferencesSearchIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "referencesSearch",
            "com.intellij.plugin.applescript.lang.ide.search.AppleScriptHandlerReferencesSearch",
        )
        assertNotNull("HandlerReferencesSearch must be registered", element)
    }

    fun testDictionaryComponentReferencesSearchIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "referencesSearch",
            "com.intellij.plugin.applescript.lang.ide.search.AppleScriptDictionaryComponentReferencesSearch",
        )
        assertNotNull("DictionaryComponentReferencesSearch must be registered", element)
    }

    fun testHandlerDeclarationSearcherIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "pom.declarationSearcher",
            "com.intellij.plugin.applescript.lang.ide.search.AppleScriptHandlerDeclarationSearcher",
        )
        assertNotNull("HandlerDeclarationSearcher must be registered", element)
    }

    fun testCompletionWeigherIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "weigher",
            "com.intellij.plugin.applescript.lang.ide.completion.AppleScriptCompletionWeigher",
        )
        assertNotNull("CompletionWeigher must be registered", element)
    }

    fun testCompletionWeigherIsNotNull() {
        val weigher = AppleScriptCompletionWeigher()
        assertNotNull("Completion weigher must not be null", weigher)
    }

    fun testHandlerReferencesSearchIsNotNull() {
        val search = AppleScriptHandlerReferencesSearch()
        assertNotNull("Handler references search must not be null", search)
    }

    fun testDictionaryComponentReferencesSearchIsNotNull() {
        val search = AppleScriptDictionaryComponentReferencesSearch()
        assertNotNull("Dictionary component references search must not be null", search)
    }

    fun testHandlerDeclarationSearcherIsNotNull() {
        val searcher = AppleScriptHandlerDeclarationSearcher()
        assertNotNull("Handler declaration searcher must not be null", searcher)
    }

    fun testCompletionOnKeywords() {
        myFixture.configureByText(AppleScriptFileType, "<caret>")
        myFixture.completeBasic()
        val elements = myFixture.lookupElements
        assertNotNull("Completion must return elements", elements)
    }

    fun testCompletionInsideTellBlock() {
        val registryService = com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService.getInstance()
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching(
            "Standard dictionaries were not initialized",
            { registryService.isInitialized() },
            10,
        )
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Music"
                <caret>
            end tell
            """.trimIndent(),
        )
        myFixture.completeBasic()
        val elements = myFixture.lookupElements
        assertNotNull("Completion must return elements inside tell block", elements)
    }
}
