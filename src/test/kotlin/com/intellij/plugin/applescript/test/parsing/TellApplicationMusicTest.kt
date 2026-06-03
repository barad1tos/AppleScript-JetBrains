package com.intellij.plugin.applescript.test.parsing

import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TellApplicationMusicTest : BasePlatformTestCase() {
    override fun shouldRunTest(): Boolean = SystemInfo.isMac && super.shouldRunTest()

    fun testTellApplicationMusic() {
        val script =
            """
            tell application "Music"
              property effect
              get properties
            end tell
            """.trimIndent()

        myFixture.configureByText("test.applescript", script)
        myFixture.checkHighlighting(false, false, true, false)
    }
}
