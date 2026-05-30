package com.intellij.plugin.applescript.test.lexer

import com.intellij.plugin.applescript.AppleScriptLexerAdapter
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.TokenType
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
}
