package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighter
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptHighlightingTest : BasePlatformTestCase() {
    fun testHighlighterReturnsLexer() {
        val highlighter = AppleScriptSyntaxHighlighter()
        assertNotNull("Highlighting lexer must not be null", highlighter.highlightingLexer)
    }

    fun testTellBlockHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            """tell application "Finder"
    activate
end tell""",
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testCommentHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            "-- this is a comment\nset x to 1",
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testStringHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            """set x to "hello world"""",
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testHandlerCallHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on greet(name)
                return "Hello " & name
            end greet

            on run
                greet("World")
            end run
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testNestedTellBlockHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                tell folder "Desktop"
                    get name
                end tell
            end tell
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testIfBlockHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            if true then
                set x to 1
            else
                set x to 2
            end if
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testRepeatBlockHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            repeat with i from 1 to 10
                log i
            end repeat
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testTryBlockHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            try
                set x to 1 / 0
            on error errMsg number errNum
                display dialog errMsg
            end try
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testMultipleHandlersHighlightingDoesNotCrash() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on add(a, b)
                return a + b
            end add

            on subtract(a, b)
                return a - b
            end subtract

            on run
                set result1 to add(1, 2)
                set result2 to subtract(5, 3)
            end run
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testLogicalOperatorHighlighting() {
        val highlighter = AppleScriptSyntaxHighlighter()
        val keys = highlighter.getTokenHighlights(AppleScriptTypes.LAND)
        val keyNames = keys.map { it.externalName }.toSet()
        assertTrue("Logical operator must have highlighting", keyNames.any { it.contains("LOGICAL") })
    }

    fun testComparisonOperatorHighlighting() {
        val highlighter = AppleScriptSyntaxHighlighter()
        val keys = highlighter.getTokenHighlights(AppleScriptTypes.GT)
        val keyNames = keys.map { it.externalName }.toSet()
        assertTrue("Comparison operator must have highlighting", keyNames.any { it.contains("COMPARISON") })
    }

    fun testMissingValueHighlighting() {
        val highlighter = AppleScriptSyntaxHighlighter()
        val keys = highlighter.getTokenHighlights(AppleScriptTypes.MISSING_VALUE)
        val keyNames = keys.map { it.externalName }.toSet()
        assertTrue("missing value must have highlighting", keyNames.any { it.contains("LANGUAGE_LITERAL") })
    }

    fun testBuiltInTypeHighlighting() {
        val highlighter = AppleScriptSyntaxHighlighter()
        val keys = highlighter.getTokenHighlights(AppleScriptTypes.INTEGER)
        val keyNames = keys.map { it.externalName }.toSet()
        assertTrue("Built-in type must have highlighting", keyNames.any { it.contains("BUILT_IN_TYPE") })
    }

    fun testScriptObjectHighlightingDoesNotCrash() {
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
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testConsideringIgnoringHighlightingDoesNotCrash() {
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
        assertNotNull("Highlights must not be null", highlights)
    }

    fun testDictionaryCommandHighlightingDoesNotCrash() {
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
        assertNotNull("Highlights must not be null", highlights)
    }
}
