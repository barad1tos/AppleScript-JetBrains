package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BACK
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EIGHTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EVERY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FIFTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FIRST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FOURTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FRONT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LAST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NINTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SECOND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SEVENTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SIXTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SOME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TENTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THIRD
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

internal object FallbackDictionaryClassParser {
    fun parseClassName(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseFallbackBareIdentifier")) return false
        if (!FallbackDictionaryTermPredicates.isClassTermToken(builder.tokenType)) return false
        // After `every`/`some` the following word(s) are unambiguously a class name, so accept a bare
        // dictionary-style noun even when it is not followed by a class anchor, e.g. `every row to 24`,
        // `the name of every group` at line end. The generic anchor rules below only need to fire in
        // ambiguous class-vs-variable positions where no class-introducing keyword precedes the term.
        if (isUnambiguousClassIntroducer(AppleScriptParserTrivia.previousNonSpaceToken(builder))) {
            return parseIntroducedClassWords(builder)
        }
        return parseAnchoredOrClassIdentifier(builder)
    }

    private fun parseIntroducedClassWords(builder: PsiBuilder): Boolean {
        var consumed = 0
        while (isIntroducedClassWord(builder, consumed)) {
            builder.advanceLexer()
            consumed += 1
        }
        return consumed > 0
    }

    private fun isIntroducedClassWord(
        builder: PsiBuilder,
        consumed: Int,
    ): Boolean =
        FallbackDictionaryTermPredicates.isClassTermToken(builder.tokenType) ||
            (
                consumed > 0 &&
                    FallbackDictionaryTermPredicates.isClassContinuationKeyword(builder.tokenType) &&
                    isClassWordAfterContinuation(builder.lookAhead(1))
            )

    private fun isUnambiguousClassIntroducer(tokenType: IElementType?): Boolean =
        tokenType === EVERY || tokenType === SOME

    private fun parseAnchoredOrClassIdentifier(builder: PsiBuilder): Boolean =
        if (FallbackDictionaryTermPredicates.isFallbackAnchorForClass(builder.lookAhead(1))) {
            advanceTerm(builder)
        } else {
            parseClassIdentifier(builder)
        }

    private fun parseClassIdentifier(builder: PsiBuilder): Boolean =
        when {
            FallbackDictionaryTermPredicates.isClassDirectSelectorAnchor(builder.lookAhead(1)) -> advanceTerm(builder)
            builder.lookAhead(1) === VAR_IDENTIFIER &&
                FallbackDictionaryTermPredicates.isClassRangeAnchor(builder.lookAhead(2)) -> advanceTerm(builder)
            FallbackDictionaryTermPredicates.isProcessClassDirectReference(builder) -> advanceTerm(builder)
            parsesSetContinuationIdentifier(builder) -> advanceTermPair(builder).let { true }
            else -> parseTwoWordIdentifier(builder)
        }

    // `rule set` / `containing set`: SET is the SECOND word of a dictionary class noun phrase. The
    // generic two-word path rejects it because `set` lexes as SET, not VAR_IDENTIFIER. Accept it ONLY
    // when (a) operand position is safe — either the previous non-space token is OF/IN, or the class
    // is introduced by an object-reference index selector (`first rule set whose ...`); AND (b) the
    // token after `set` is a valid class terminator/selector: WHOSE/WHERE (filter), a line/paren end
    // (`containing set\n`), or a trailing VAR_IDENTIFIER by-name selector (`rule set theSet`).
    private fun parsesSetContinuationIdentifier(builder: PsiBuilder): Boolean =
        FallbackDictionaryTermPredicates.isClassContinuationKeyword(builder.lookAhead(1)) &&
            isSafeSetContinuationPosition(AppleScriptParserTrivia.previousNonSpaceToken(builder)) &&
            isClassWordAfterContinuation(builder.lookAhead(2))

    private fun isSafeSetContinuationPosition(tokenType: IElementType?): Boolean =
        tokenType === OF || tokenType === IN || isIndexSelector(tokenType)

    private fun isIndexSelector(tokenType: IElementType?): Boolean =
        tokenType === FIRST ||
            tokenType === SECOND ||
            tokenType === THIRD ||
            tokenType === FOURTH ||
            tokenType === FIFTH ||
            tokenType === SIXTH ||
            tokenType === SEVENTH ||
            tokenType === EIGHTH ||
            tokenType === NINTH ||
            tokenType === TENTH ||
            tokenType === LAST ||
            tokenType === FRONT ||
            tokenType === BACK

    private fun isClassWordAfterContinuation(tokenType: IElementType?): Boolean =
        tokenType === VAR_IDENTIFIER ||
            FallbackDictionaryTermPredicates.isFallbackAnchorForClass(tokenType) ||
            FallbackDictionaryAnchorPredicates.isClassDirectReferenceAnchor(tokenType)

    private fun parseTwoWordIdentifier(builder: PsiBuilder): Boolean {
        val result =
            builder.lookAhead(1) === VAR_IDENTIFIER &&
                FallbackDictionaryTermPredicates.isFallbackAnchorForClass(builder.lookAhead(2))
        if (result) {
            advanceTermPair(builder)
        }
        return result
    }

    private fun advanceTerm(builder: PsiBuilder): Boolean {
        builder.advanceLexer()
        return true
    }

    private fun advanceTermPair(builder: PsiBuilder) {
        builder.advanceLexer()
        builder.advanceLexer()
    }
}
