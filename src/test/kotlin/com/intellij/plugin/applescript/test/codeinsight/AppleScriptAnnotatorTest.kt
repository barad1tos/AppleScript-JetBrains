package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptAnnotatorTest : BasePlatformTestCase() {
    fun testKnownApplicationDoesNotProduceError() {
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
                play
            end tell
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Known application must not produce errors", errors.isEmpty())
    }

    fun testHandlerCallHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on greet(theName)
                return "Hello " & theName
            end greet

            on run
                greet("World")
            end run
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }

    fun testIfThenElseHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on checkValue(x)
                if x > 0 then
                    return "positive"
                else if x < 0 then
                    return "negative"
                else
                    return "zero"
                end if
            end checkValue
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }

    fun testTellBlockHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                set f to folder "Desktop"
                get name of f
            end tell
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }

    fun testTryOnErrorHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            try
                set x to 1 / 0
            on error errMsg number errNum
                display dialog "Error: " & errMsg & " (" & errNum & ")"
            end try
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }

    fun testRepeatLoopHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            set total to 0
            repeat with i from 1 to 10
                set total to total + i
            end repeat
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }

    fun testScriptObjectHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            script MyScript
                property counter : 0

                on increment
                    set counter to counter + 1
                    return counter
                end increment
            end script
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }

    fun testConsideringIgnoringHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            considering case
                if "ABC" is "abc" then
                    log "equal"
                end if
            end considering
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }

    fun testMultipleTellBlocksHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                activate
            end tell

            tell application "TextEdit"
                activate
            end tell
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }

    fun testNestedHandlersHighlighting() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on outerHandler()
                on innerHandler(x)
                    return x * 2
                end innerHandler

                return innerHandler(5)
            end outerHandler
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull(highlights)
    }
}
