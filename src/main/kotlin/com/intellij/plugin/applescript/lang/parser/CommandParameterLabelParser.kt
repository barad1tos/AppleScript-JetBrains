package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIGITS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

/**
 * Parses a bare-identifier command parameter label — e.g. `direction` in
 * `sort by column 1 direction ascending` — for dictionary commands parsed without the dictionary
 * that defines the label. The grammar only reaches this after at least one keyword label, so a
 * leading bare identifier is never read as a command call (BL-C7).
 *
 * A VAR_IDENTIFIER is taken as a label only when a grammar value-start token clearly follows.
 * Under-matching is safe — an unrecognized value-start just leaves the
 * construct to the existing parse — while over-matching would mis-read existing handler calls.
 * Parser-level only: no PSI node, mirroring the Standard Additions object tokens (PARSE-04).
 */
internal object CommandParameterLabelParser {
    private val VALUE_START_TOKENS: Set<IElementType> =
        setOf(VAR_IDENTIFIER, DIGITS, STRING_LITERAL, LCURLY, LPAREN)

    fun parseBareLabel(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseBareCommandParameterLabel")) return false
        if (builder.tokenType !== VAR_IDENTIFIER) return false
        if (builder.lookAhead(1) !in VALUE_START_TOKENS) return false
        builder.advanceLexer()
        return true
    }
}
