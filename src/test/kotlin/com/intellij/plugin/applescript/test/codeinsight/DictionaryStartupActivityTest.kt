package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DictionaryStartupActivityTest : BasePlatformTestCase() {
    fun testStartupActivityIsRegistered() {
        val activity =
            PluginDescriptorTestSupport.findElement(
                "postStartupActivity",
                "com.intellij.plugin.applescript.lang.ide.sdef.DictionaryStartupActivity",
            )

        assertNotNull("Dictionary startup activity must be registered", activity)
    }
}
