package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.lang.formatter.settings.AppleScriptCodeStyleSettingsProvider
import com.intellij.plugin.applescript.lang.formatter.settings.AppleScriptLanguageCodeStyleSettingsProvider
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptCodeStyleSettingsTest : BasePlatformTestCase() {
    fun testLanguageProviderReturnsAppleScriptLanguage() {
        val provider = AppleScriptLanguageCodeStyleSettingsProvider()
        assertEquals(AppleScriptLanguage, provider.language)
    }

    fun testLanguageProviderCodeSampleIsNotEmpty() {
        val provider = AppleScriptLanguageCodeStyleSettingsProvider()
        for (settingsType in SettingsType.entries) {
            val sample = provider.getCodeSample(settingsType)
            assertNotNull("Code sample must not be null for $settingsType", sample)
            assertTrue("Code sample must not be empty for $settingsType", sample.isNotEmpty())
        }
    }

    fun testLanguageProviderIndentOptionsEditorIsNotNull() {
        val provider = AppleScriptLanguageCodeStyleSettingsProvider()
        assertNotNull("Indent options editor must not be null", provider.indentOptionsEditor)
    }

    fun testCodeStyleSettingsProviderDisplayName() {
        val provider = AppleScriptCodeStyleSettingsProvider()
        assertEquals("AppleScript", provider.configurableDisplayName)
    }

    fun testCodeStyleSettingsProviderCreatesConfigurable() {
        val provider = AppleScriptCodeStyleSettingsProvider()
        val settings = com.intellij.psi.codeStyle.CodeStyleSettings()
        val configurable = provider.createConfigurable(settings, settings)
        assertNotNull("Configurable must not be null", configurable)
    }

    fun testCodeStyleSettingsProviderIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "codeStyleSettingsProvider",
            "com.intellij.plugin.applescript.lang.formatter.settings.AppleScriptCodeStyleSettingsProvider",
        )
        assertNotNull("AppleScriptCodeStyleSettingsProvider must be registered", element)
    }

    fun testLanguageCodeStyleSettingsProviderIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "langCodeStyleSettingsProvider",
            "com.intellij.plugin.applescript.lang.formatter.settings.AppleScriptLanguageCodeStyleSettingsProvider",
        )
        assertNotNull("AppleScriptLanguageCodeStyleSettingsProvider must be registered", element)
    }
}
