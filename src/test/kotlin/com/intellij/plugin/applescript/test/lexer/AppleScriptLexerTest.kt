package com.intellij.plugin.applescript.test.lexer

import com.intellij.plugin.applescript.AppleScriptLexerAdapter
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import junit.framework.TestCase

/**
 * Created by Andrey on 02/01/16.
 *
 * JUnit3 lexer test: extends [TestCase] and is discovered by the `test` name prefix
 * (Vintage). Boots no Platform fixture — instantiates [AppleScriptLexerAdapter] directly,
 * so it runs unconditionally in the light suite under the `lexer.*` matcher.
 */
class AppleScriptLexerTest : TestCase() {

    fun testLexerTest() {
        val lexer = AppleScriptLexerAdapter()
        val ch: CharSequence = "the \"My string\""
        lexer.start(ch)
        var tt = lexer.tokenType
        assertTrue(tt === AppleScriptTypes.THE_KW)
        lexer.advance()
        tt = lexer.tokenType
        assertTrue(tt === TokenType.WHITE_SPACE)
        lexer.advance()
        tt = lexer.tokenType
        assertTrue(tt === AppleScriptTypes.STRING_LITERAL)
    }

    private fun tokenTypesOf(input: CharSequence): List<IElementType> {
        val lexer = AppleScriptLexerAdapter()
        lexer.start(input)
        val types = mutableListOf<IElementType>()
        while (true) {
            val tt = lexer.tokenType ?: break
            types.add(tt)
            lexer.advance()
        }
        return types
    }

    // PARSE-05 regression-lock: the non-ASCII operators ≥ ≤ ≠ ÷ must lex to their existing
    // operator tokens (GE/LE/NE/DIV) with no BAD_CHARACTER. These assertions fail loudly if a
    // future _AppleScriptLexer.flex edit drops a mapping. (AppleScript has no "!=" operator —
    // its inequality operator is the non-ASCII ≠, which the flex NE disjunction already covers.)

    fun testGreaterThanOrEqualSymbol() {
        val types = tokenTypesOf("a ≥ b")
        assertTrue("≥ must lex to GE", types.contains(AppleScriptTypes.GE))
        assertFalse("≥ must not produce BAD_CHARACTER", types.contains(TokenType.BAD_CHARACTER))
    }

    fun testLessThanOrEqualSymbol() {
        val types = tokenTypesOf("a ≤ b")
        assertTrue("≤ must lex to LE", types.contains(AppleScriptTypes.LE))
        assertFalse("≤ must not produce BAD_CHARACTER", types.contains(TokenType.BAD_CHARACTER))
    }

    fun testNotEqualSymbol() {
        val types = tokenTypesOf("a ≠ b")
        assertTrue("≠ must lex to NE", types.contains(AppleScriptTypes.NE))
        assertFalse("≠ must not produce BAD_CHARACTER", types.contains(TokenType.BAD_CHARACTER))
    }

    fun testDivideSymbol() {
        val types = tokenTypesOf("a ÷ b")
        assertTrue("÷ must lex to DIV", types.contains(AppleScriptTypes.DIV))
        assertFalse("÷ must not produce BAD_CHARACTER", types.contains(TokenType.BAD_CHARACTER))
    }
}
