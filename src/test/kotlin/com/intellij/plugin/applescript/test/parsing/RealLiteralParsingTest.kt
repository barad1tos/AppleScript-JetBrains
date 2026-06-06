package com.intellij.plugin.applescript.test.parsing

import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Regression guard for decimal real literals. Before the `DOT` token was added, the lexer
 * emitted `BAD_CHARACTER` for `.`, so `realLiteralExpression` could not assemble cleanly and the
 * IDE flagged `0.3` / `3.14E5` with a `BAD_CHARACTER` highlight. These cases are dictionary-free
 * and platform-independent (plain assignment RHS, no `tell` block, no osax), so they run in the
 * cold CI suite too.
 *
 * Uses the `.applescript` extension to also lock in that the language infrastructure (lexer,
 * parser, highlighter) applies to `.applescript`, not only `.scpt`.
 */
class RealLiteralParsingTest : BasePlatformTestCase() {
    fun testDecimalRealLiteralsParseWithoutErrors() {
        val script =
            """
            set a to 0.3
            set b to 1.5
            set c to 3.14
            set d to 0.04
            set e to 3.14E5
            set f to 2.5e-3
            """.trimIndent()

        val file = myFixture.configureByText("test.applescript", script)

        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertEmpty("Decimal reals must parse without parser errors", errors.toList())

        val badChars = PsiTreeUtil.collectElements(file) { it.node?.elementType == TokenType.BAD_CHARACTER }
        assertEmpty("Decimal point must lex as DOT, not BAD_CHARACTER", badChars.toList())

        val reals =
            PsiTreeUtil.collectElements(file) {
                it.node?.elementType == AppleScriptTypes.REAL_LITERAL_EXPRESSION
            }
        assertSize(6, reals)
    }
}
