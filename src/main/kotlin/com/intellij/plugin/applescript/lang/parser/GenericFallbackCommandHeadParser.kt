package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

internal object GenericFallbackCommandHeadParser {
    private val commandEntryPreviousTokens =
        TokenSet.create(
            NLS,
            LPAREN,
            IF,
            THEN,
            TO,
        )

    fun parse(
        builder: PsiBuilder,
        parsedName: Ref<String>,
    ): Boolean {
        var result = false
        if (isCommandLegalContext(builder)) {
            val allowsPropertyReferenceTail = allowsPropertyReferenceTail(builder)
            val marker = builder.mark()
            val words = parseHeadWords(builder)
            if (isAcceptedHead(words, builder, allowsPropertyReferenceTail)) {
                marker.drop()
                parsedName.set(words.joinToString(" "))
                configureParameterParsing(builder, words)
                result = true
            } else {
                marker.rollbackTo()
            }
        }
        return result
    }

    private fun parseHeadWords(builder: PsiBuilder): List<String> {
        val words = mutableListOf<String>()
        while (builder.tokenType === VAR_IDENTIFIER) {
            if (words.isNotEmpty() && isIdentifierCommandTailStart(builder)) {
                break
            }
            words += builder.tokenText.orEmpty()
            builder.advanceLexer()
        }
        if (words.isNotEmpty() && isCommandSelectorStart(builder.tokenType)) {
            val marker = builder.mark()
            val phraseWords = mutableListOf(builder.tokenText.orEmpty())
            builder.advanceLexer()
            while (builder.tokenType === VAR_IDENTIFIER) {
                phraseWords += builder.tokenText.orEmpty()
                builder.advanceLexer()
            }
            if (phraseWords.size > 1 && FallbackCommandParameterParser.isBuiltInClassDirectParameterStart(builder)) {
                marker.drop()
                words += phraseWords
            } else {
                marker.rollbackTo()
            }
        }
        return words
    }

    private fun configureParameterParsing(
        builder: PsiBuilder,
        words: List<String>,
    ) {
        if (words.size == 1 && FallbackCommandParameterParser.isGrammarValueDirectParameterStart(builder)) {
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE,
                FallbackCommandParameterMode.OptionalDirectParameter,
            )
        } else {
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED, true)
        }
    }

    private fun isAcceptedHead(
        words: List<String>,
        builder: PsiBuilder,
        allowsPropertyReferenceTail: Boolean,
    ): Boolean =
        words.isNotEmpty() &&
            !isSingleWordCoercionCandidate(words, builder) &&
            isCommandTailStart(builder, allowsPropertyReferenceTail)

    private fun isCommandLegalContext(builder: PsiBuilder): Boolean {
        val isInsideFallbackParameters =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS) == true
        val isInsideDictionaryParameters =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS) == true
        return !isInsideFallbackParameters &&
            !isInsideDictionaryParameters &&
            isCommandEntryBoundary(builder)
    }

    private fun isCommandEntryBoundary(builder: PsiBuilder): Boolean {
        val previousToken = AppleScriptParserTrivia.previousNonSpaceToken(builder)
        return previousToken == null || commandEntryPreviousTokens.contains(previousToken)
    }

    private fun isCommandTailStart(
        builder: PsiBuilder,
        allowsPropertyReferenceTail: Boolean,
    ): Boolean {
        val tokenType = builder.tokenType
        return tokenType != null &&
            (
                isCommandSelectorStart(tokenType) ||
                    FallbackCommandParameterParser.isValueLiteralStart(tokenType) ||
                    FallbackCommandParameterParser.isBuiltInClassDirectParameterStart(builder) ||
                    FallbackCommandParameterParser.isGrammarValueDirectParameterStart(builder) ||
                    (
                        allowsPropertyReferenceTail &&
                            FallbackCommandParameterParser.isPropertyReferenceDirectParameterStart(builder)
                    ) ||
                    isIdentifierCommandTailStart(builder)
            )
    }

    private fun allowsPropertyReferenceTail(builder: PsiBuilder): Boolean =
        AppleScriptParserTrivia.previousNonSpaceToken(builder) !== TO

    private fun isIdentifierCommandTailStart(builder: PsiBuilder): Boolean {
        val nextTokenType = builder.lookAhead(1)
        return builder.tokenType === VAR_IDENTIFIER &&
            (
                isCommandSelectorStart(nextTokenType) ||
                    nextTokenType === OF ||
                    FallbackCommandParameterTokens.isExpressionContinuationStart(nextTokenType)
            )
    }

    private fun isSingleWordCoercionCandidate(
        words: List<String>,
        builder: PsiBuilder,
    ): Boolean = words.size == 1 && builder.tokenType === AS

    private fun isCommandSelectorStart(tokenType: IElementType?): Boolean =
        FallbackCommandParameterTokens.isCommandSelectorStart(tokenType)
}
