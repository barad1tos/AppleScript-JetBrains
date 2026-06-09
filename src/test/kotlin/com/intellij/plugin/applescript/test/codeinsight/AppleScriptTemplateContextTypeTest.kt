package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.AppleScriptTemplateContextType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptTemplateContextTypeTest : BasePlatformTestCase() {
    fun testTemplateContextTypeIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "liveTemplateContext",
            "com.intellij.plugin.applescript.lang.ide.AppleScriptTemplateContextType",
        )
        assertNotNull("TemplateContextType must be registered", element)
    }

    fun testTemplateContextTypeIsNotNull() {
        val contextType = AppleScriptTemplateContextType()
        assertNotNull("Template context type must not be null", contextType)
    }
}
