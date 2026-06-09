package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptSearchAndCompletionAdditionalTest : BasePlatformTestCase() {
    fun testHandlerReferencesSearchIsRegistered() {
        val element =
            PluginDescriptorTestSupport.findElement(
                "referencesSearch",
                "com.intellij.plugin.applescript.lang.ide.search.AppleScriptHandlerReferencesSearch",
            )
        assertNotNull("HandlerReferencesSearch must be registered", element)
    }

    fun testDictionaryComponentReferencesSearchIsRegistered() {
        val element =
            PluginDescriptorTestSupport.findElement(
                "referencesSearch",
                "com.intellij.plugin.applescript.lang.ide.search.AppleScriptDictionaryComponentReferencesSearch",
            )
        assertNotNull("DictionaryComponentReferencesSearch must be registered", element)
    }

    fun testHandlerDeclarationSearcherIsRegistered() {
        val element =
            PluginDescriptorTestSupport.findElement(
                "pom.declarationSearcher",
                "com.intellij.plugin.applescript.lang.ide.search.AppleScriptHandlerDeclarationSearcher",
            )
        assertNotNull("HandlerDeclarationSearcher must be registered", element)
    }

    fun testCompletionWeigherIsRegistered() {
        val element =
            PluginDescriptorTestSupport.findElement(
                "weigher",
                "com.intellij.plugin.applescript.lang.ide.completion.AppleScriptCompletionWeigher",
            )
        assertNotNull("CompletionWeigher must be registered", element)
    }

    fun testCompletionOnKeywords() {
        myFixture.configureByText(AppleScriptFileType, "<caret>")
        myFixture.completeBasic()
        val elements = myFixture.lookupElements
        assertTrue("Completion must return elements", elements != null && elements.isNotEmpty())
    }

    fun testCompletionInsideTellBlock() {
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
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
        assertTrue("Completion must return elements inside tell block", elements != null && elements.isNotEmpty())
    }
}
