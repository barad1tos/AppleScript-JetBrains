package com.intellij.plugin.applescript.test.parser

import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.tree.IElementType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Locks the two sides of the soft-keyword seam together: every token enrolled in the
 * grammar rule `softKeywordIdentifier` (AppleScript.bnf) must also be enrolled in
 * [AppleScriptTokenTypesSets.SOFT_KEYWORD_IDENTIFIERS], and vice versa. Without this
 * parity a newly enrolled soft keyword would parse as an identifier while rename
 * refactoring still rejects it — the exact bug class the seam exists to prevent.
 */
class SoftKeywordSeamParityTest {
    @Test
    fun tokenSetMatchesGrammarSeam() {
        val bnf = File("src/main/resources/AppleScript.bnf").readText()
        val seamRule =
            Regex("""private softKeywordIdentifier ::=\s*([^\n]+)""").find(bnf)
                ?: error("softKeywordIdentifier rule not found in AppleScript.bnf")
        val enrolledFromGrammar =
            seamRule.groupValues[1]
                .split('|')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { tokenName ->
                    AppleScriptTypes::class.java.getField(tokenName.uppercase()).get(null) as IElementType
                }.toSet()

        assertEquals(
            "SOFT_KEYWORD_IDENTIFIERS must enroll exactly the softKeywordIdentifier tokens from AppleScript.bnf",
            enrolledFromGrammar,
            AppleScriptTokenTypesSets.SOFT_KEYWORD_IDENTIFIERS.types.toSet(),
        )
    }
}
