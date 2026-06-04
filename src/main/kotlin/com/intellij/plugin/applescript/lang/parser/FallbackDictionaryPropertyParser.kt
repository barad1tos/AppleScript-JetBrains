package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER

internal object FallbackDictionaryPropertyParser {
    fun parseKeywordAsProperty(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseKeywordAsPropertyFallback")) return false
        return parseContextualProperty(builder) ||
            parseIdentifierThenContextualProperty(builder) ||
            parseContextualPropertyThenIdentifier(builder)
    }

    fun parsePropertyName(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseFallbackBareIdentifier")) return false
        return builder.tokenType === VAR_IDENTIFIER && parseAnchoredOrPropertyIdentifier(builder)
    }

    private fun parseContextualProperty(builder: PsiBuilder): Boolean {
        val result =
            FallbackDictionaryTermPredicates.isContextualPropertyTerm(builder.tokenType) &&
                FallbackDictionaryTermPredicates.isFallbackAnchorForProperty(builder.lookAhead(1))
        if (result) {
            builder.advanceLexer()
        }
        return result
    }

    private fun parseIdentifierThenContextualProperty(builder: PsiBuilder): Boolean {
        val result =
            builder.tokenType === VAR_IDENTIFIER &&
                FallbackDictionaryTermPredicates.isContextualPropertyTerm(builder.lookAhead(1)) &&
                FallbackDictionaryTermPredicates.isFallbackAnchorForProperty(builder.lookAhead(2))
        if (result) {
            builder.advanceLexer()
            builder.advanceLexer()
        }
        return result
    }

    private fun parseContextualPropertyThenIdentifier(builder: PsiBuilder): Boolean {
        val result =
            FallbackDictionaryTermPredicates.isContextualPropertyTerm(builder.tokenType) &&
                builder.lookAhead(1) === VAR_IDENTIFIER &&
                FallbackDictionaryTermPredicates.isFallbackAnchorForProperty(builder.lookAhead(2))
        if (result) {
            builder.advanceLexer()
            builder.advanceLexer()
        }
        return result
    }

    private fun parseAnchoredOrPropertyIdentifier(builder: PsiBuilder): Boolean =
        if (FallbackDictionaryTermPredicates.isFallbackAnchorForProperty(builder.lookAhead(1))) {
            FallbackDictionaryTermActions.advanceTerm(builder)
        } else {
            parsePropertyIdentifier(builder)
        }

    private fun parsePropertyIdentifier(builder: PsiBuilder): Boolean =
        when {
            FallbackDictionaryPropertyPatterns.isContextualPropertyPairWithAnchor(builder) ->
                FallbackDictionaryTermActions.advanceTermPair(builder)
            FallbackDictionaryPropertyPatterns.isContextualPropertyPairWithTerminator(builder) ->
                FallbackDictionaryTermActions.advanceTermPair(builder)
            FallbackDictionaryPropertyPatterns.isIdentifierPairWithTerminator(builder) ->
                FallbackDictionaryTermActions.advanceTermPair(builder)
            else -> parseTwoWordIdentifier(builder)
        }

    private fun parseTwoWordIdentifier(builder: PsiBuilder): Boolean {
        val result = FallbackDictionaryPropertyPatterns.isTwoWordIdentifierWithAnchor(builder)
        if (result) {
            FallbackDictionaryTermActions.advanceTermPair(builder)
        }
        return result
    }
}
