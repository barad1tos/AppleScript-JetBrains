package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptBraceMatcherTest : BasePlatformTestCase() {
    fun testBraceMatchingTellBlock() {
        assertBraceMatches(
            """tell application "Finder"
    activate
end tell""",
            0,
        )
    }

    fun testBraceMatchingIfBlock() {
        assertBraceMatches(
            """if true then
    set x to 1
end if""",
            0,
        )
    }

    fun testBraceMatchingOnHandler() {
        assertBraceMatches(
            """on greet(name)
    return "Hello " & name
end greet""",
            0,
        )
    }

    fun testBraceMatchingTryBlock() {
        assertBraceMatches(
            """try
    set x to 1 / 0
on error errMsg
    display dialog errMsg
end try""",
            0,
        )
    }

    fun testBraceMatchingRepeatBlock() {
        assertBraceMatches(
            """repeat with i from 1 to 10
    log i
end repeat""",
            0,
        )
    }

    fun testBraceMatchingScriptBlock() {
        assertBraceMatches(
            """script MyScript
    property x : 1
end script""",
            0,
        )
    }

    fun testBraceMatchingConsideringBlock() {
        assertBraceMatches(
            """considering case
    set x to "A"
end considering""",
            0,
        )
    }

    fun testBraceMatchingNestedBlocks() {
        assertBraceMatches(
            """tell application "Finder"
    if true then
        repeat with i from 1 to 5
            log i
        end repeat
    end if
end tell""",
            0,
        )
    }

    fun testBraceMatchingCurlyBraces() {
        myFixture.configureByText(AppleScriptFileType, """set myRecord to {name:"test", value:123}""")
        val chars = myFixture.editor.document.charsSequence
        val leftOffset = chars.indexOf('{')
        val rightOffset = chars.indexOf('}')
        assertTrue("Must find opening curly brace", leftOffset >= 0)
        assertTrue("Must find closing curly brace", rightOffset >= 0)

        myFixture.editor.caretModel.moveToOffset(leftOffset)
        val matchedRight = BraceMatchingUtil.getMatchedBraceOffset(myFixture.editor, true, myFixture.file)
        assertEquals("Opening curly must match closing curly", rightOffset, matchedRight)
    }

    fun testBraceMatchingParentheses() {
        myFixture.configureByText(AppleScriptFileType, """set x to (1 + 2) * 3""")
        val chars = myFixture.editor.document.charsSequence
        val leftOffset = chars.indexOf('(')
        val rightOffset = chars.indexOf(')')
        assertTrue("Must find opening paren", leftOffset >= 0)
        assertTrue("Must find closing paren", rightOffset >= 0)

        myFixture.editor.caretModel.moveToOffset(leftOffset)
        val matchedRight = BraceMatchingUtil.getMatchedBraceOffset(myFixture.editor, true, myFixture.file)
        assertEquals("Opening paren must match closing paren", rightOffset, matchedRight)
    }

    private fun assertBraceMatches(
        script: String,
        leftBraceOffset: Int,
    ) {
        myFixture.configureByText(AppleScriptFileType, script)
        myFixture.editor.caretModel.moveToOffset(leftBraceOffset)
        val rightOffset = BraceMatchingUtil.getMatchedBraceOffset(myFixture.editor, true, myFixture.file)
        assertTrue("Expected brace match at offset $leftBraceOffset, got $rightOffset", rightOffset > leftBraceOffset)
        myFixture.editor.caretModel.moveToOffset(rightOffset)
        val matched = BraceMatchingUtil.getMatchedBraceOffset(myFixture.editor, false, myFixture.file)
        assertEquals("Brace match must be symmetric", leftBraceOffset, matched)
    }
}
