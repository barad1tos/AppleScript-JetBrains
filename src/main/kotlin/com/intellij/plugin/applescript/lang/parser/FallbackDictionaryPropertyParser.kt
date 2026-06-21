package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SET
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

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
                isPropertyAnchor(builder, builder.lookAhead(1))
        if (result) {
            builder.advanceLexer()
        }
        return result
    }

    private fun parseIdentifierThenContextualProperty(builder: PsiBuilder): Boolean {
        val result =
            builder.tokenType === VAR_IDENTIFIER &&
                FallbackDictionaryTermPredicates.isContextualPropertyTerm(builder.lookAhead(1)) &&
                isPropertyAnchor(builder, builder.lookAhead(2))
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
                isPropertyAnchor(builder, builder.lookAhead(2))
        if (result) {
            builder.advanceLexer()
            builder.advanceLexer()
        }
        return result
    }

    private fun parseAnchoredOrPropertyIdentifier(builder: PsiBuilder): Boolean =
        if (isPropertyAnchor(builder, builder.lookAhead(1))) {
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
            else -> parseAnchoredPropertyPhrase(builder)
        }

    private fun parseAnchoredPropertyPhrase(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        var wordCount = 0
        var shouldContinue = true

        while (shouldContinue) {
            val tokenType = builder.tokenType
            shouldContinue =
                tokenType === VAR_IDENTIFIER ||
                FallbackDictionaryTermPredicates.isContextualPropertyTerm(tokenType) ||
                (
                    wordCount > 1 &&
                        tokenType === TO &&
                        builder.lookAhead(1) === VAR_IDENTIFIER
                )
            if (shouldContinue) {
                builder.advanceLexer()
                wordCount += 1
            }
        }

        val result = wordCount > 1 && isPropertyAnchor(builder, builder.tokenType)
        if (result) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return result
    }

    private fun isPropertyAnchor(
        builder: PsiBuilder,
        tokenType: IElementType?,
    ): Boolean =
        FallbackDictionaryTermPredicates.isFallbackAnchorForProperty(tokenType) ||
            isAssignmentTargetTerminator(builder, tokenType)

    private fun isAssignmentTargetTerminator(
        builder: PsiBuilder,
        tokenType: IElementType?,
    ): Boolean =
        tokenType === TO &&
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_ASSIGNMENT_STATEMENT) == true &&
            AppleScriptParserTrivia.previousNonSpaceToken(builder) === SET
}
