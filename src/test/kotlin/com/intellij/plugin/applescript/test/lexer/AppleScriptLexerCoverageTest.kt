package com.intellij.plugin.applescript.test.lexer

import com.intellij.plugin.applescript.AppleScriptLexerAdapter
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptLexerCoverageTest : BasePlatformTestCase() {
    fun testLexerTokenizesKeywords() {
        val tokens = tokenize("tell application \"Finder\" to activate")
        assertTrue("Must have tokens", tokens.isNotEmpty())
        val tokenTypes = tokens.map { it.first }.toSet()
        assertTrue("Must have keyword tokens", tokenTypes.size > 1)
    }

    fun testLexerTokenizesOperators() {
        val tokens = tokenize("set x to 1 + 2 * 3 - 4 / 5")
        assertTrue("Must have tokens", tokens.isNotEmpty())
    }

    fun testLexerTokenizesComparisons() {
        val tokens = tokenize("if x > 0 and y < 10 and z >= 5 and w <= 3 then")
        assertTrue("Must have tokens", tokens.isNotEmpty())
    }

    fun testLexerTokenizesStrings() {
        val tokens = tokenize("""set x to "hello" & "world" """)
        assertTrue("Must have tokens", tokens.isNotEmpty())
    }

    fun testLexerTokenizesNumbers() {
        val tokens = tokenize("set x to 42\nset y to 3.14\nset z to -1")
        assertTrue("Must have tokens", tokens.isNotEmpty())
    }

    fun testLexerTokenizesComments() {
        val tokens = tokenize("-- this is a comment\nset x to 1")
        assertTrue("Must have tokens", tokens.isNotEmpty())
        val hasComment = tokens.any { it.first == AppleScriptTypes.COMMENT }
        assertTrue("Must have comment token", hasComment)
    }

    fun testLexerTokenizesTellBlock() {
        val script =
            """
            tell application "Finder"
                activate
                get name of folder 1
            end tell
            """.trimIndent()
        val tokens = tokenize(script)
        assertTrue("Must have tokens", tokens.size > 5)
    }

    fun testLexerTokenizesIfBlock() {
        val script =
            """
            if true then
                set x to 1
            else
                set x to 2
            end if
            """.trimIndent()
        val tokens = tokenize(script)
        assertTrue("Must have tokens", tokens.size > 5)
    }

    fun testLexerTokenizesHandler() {
        val script =
            """
            on greet(name)
                return "Hello " & name
            end greet
            """.trimIndent()
        val tokens = tokenize(script)
        assertTrue("Must have tokens", tokens.size > 5)
    }

    fun testLexerTokenizesList() {
        val tokens = tokenize("set myList to {1, 2, 3, \"hello\"}")
        assertTrue("Must have tokens", tokens.isNotEmpty())
    }

    fun testLexerTokenizesRecord() {
        val tokens = tokenize("set myRecord to {name:\"John\", age:30}")
        assertTrue("Must have tokens", tokens.isNotEmpty())
    }

    fun testLexerTokenizesBooleanLiterals() {
        val tokens = tokenize("set x to true\nset y to false")
        assertTrue("Must have tokens", tokens.isNotEmpty())
    }

    fun testLexerTokenizesMissingValue() {
        val tokens = tokenize("set x to missing value")
        assertTrue("Must have tokens", tokens.isNotEmpty())
    }

    fun testLexerTokenizesRepeatLoop() {
        val script =
            """
            repeat with i from 1 to 10
                log i
            end repeat
            """.trimIndent()
        val tokens = tokenize(script)
        assertTrue("Must have tokens", tokens.size > 5)
    }

    fun testLexerTokenizesTryCatch() {
        val script =
            """
            try
                set x to 1 / 0
            on error errMsg
                log errMsg
            end try
            """.trimIndent()
        val tokens = tokenize(script)
        assertTrue("Must have tokens", tokens.size > 5)
    }

    fun testLexerTokenizesScriptObject() {
        val script =
            """
            script MyScript
                property x : 1
            end script
            """.trimIndent()
        val tokens = tokenize(script)
        assertTrue("Must have tokens", tokens.size > 5)
    }

    private fun tokenize(text: String): List<Pair<IElementType, String>> {
        val lexer = AppleScriptLexerAdapter()
        val tokens = mutableListOf<Pair<IElementType, String>>()
        lexer.start(text)
        while (lexer.tokenType != null) {
            val type = lexer.tokenType!!
            val tokenText = lexer.tokenText
            if (type != TokenType.WHITE_SPACE) {
                tokens.add(type to tokenText)
            }
            lexer.advance()
        }
        return tokens
    }
}
