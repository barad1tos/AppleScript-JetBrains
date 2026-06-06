package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EVERY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SOME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.TokenType
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
        if (isUnambiguousClassIntroducer(previousNonSpaceToken(builder))) {
            return parseIntroducedClassWords(builder)
        }
        return parseAnchoredOrClassIdentifier(builder)
    }

    private fun parseIntroducedClassWords(builder: PsiBuilder): Boolean {
        var consumed = 0
        while (FallbackDictionaryTermPredicates.isClassTermToken(builder.tokenType)) {
            builder.advanceLexer()
            consumed += 1
        }
        return consumed > 0
    }

    private fun isUnambiguousClassIntroducer(tokenType: IElementType?): Boolean =
        tokenType === EVERY || tokenType === SOME

    private fun previousNonSpaceToken(builder: PsiBuilder): IElementType? {
        var index = -1
        var tokenType = builder.rawLookup(index)
        while (tokenType === TokenType.WHITE_SPACE) {
            tokenType = builder.rawLookup(--index)
        }
        return tokenType
    }

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
            else -> parseTwoWordIdentifier(builder)
        }

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
