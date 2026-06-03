package com.intellij.plugin.applescript.test.parsing

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TellApplicationMusicTest : BasePlatformTestCase() {
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
