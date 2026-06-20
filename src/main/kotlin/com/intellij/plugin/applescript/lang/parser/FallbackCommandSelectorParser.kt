package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_TYPE_S
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TYPE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH

internal object FallbackCommandSelectorParser {
    fun parseCommandParameters(
        builder: PsiBuilder,
        level: Int,
    ) {
        var shouldContinue = true
        while (shouldContinue) {
            val isParameterStart =
                builder.tokenType === OF ||
                    FallbackCommandParameterTokens.isPrepositionParameterStart(builder.tokenType) ||
                    builder.tokenType === VAR_IDENTIFIER
            if (!isParameterStart) break

            val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter>")
            val result =
                parseParameterSelector(builder, level + 1) &&
                    parseParameterValue(builder, level + 1)
            exit_section_(builder, level, marker, COMMAND_PARAMETER, result, false, null)
            shouldContinue = result
        }
    }

    fun parseSelectorTokens(builder: PsiBuilder): Boolean =
        when {
            parseDictionarySelectorTokens(builder) -> true
            builder.tokenType === OF ||
                FallbackCommandParameterTokens.isPrepositionParameterStart(builder.tokenType) -> {
                val selectorStart = builder.tokenType
                builder.advanceLexer()
                val hasSelectorLabel =
                    (
                        builder.tokenType === VAR_IDENTIFIER ||
                            builder.tokenType === BUILT_IN_TYPE_S ||
                            builder.tokenType === TYPE
                    ) &&
                        (
                            selectorStart === WITH ||
                                FallbackCommandParameterTokens.isValueLiteralStart(builder.lookAhead(1))
                        )
                if (hasSelectorLabel) {
                    builder.advanceLexer()
                }
                true
            }
            builder.tokenType === VAR_IDENTIFIER -> {
                parseBareParameterSelector(builder)
                true
            }
            else -> false
        }

    private fun parseParameterValue(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        parseSimpleLiteralValueBeforeSelector(builder) ||
            FallbackCommandParameterValueBoundaries.consumeIdentifierPhraseBeforeStructuredBareSelector(builder) ||
            parseSingleIdentifierValueBeforeBareSelector(builder) ||
            FallbackCommandParameterValueBoundaries.parseExpressionAtValueBoundary(builder, level + 1) ||
            parseStructuredBracketFallback(builder)

    private fun parseStructuredBracketFallback(builder: PsiBuilder): Boolean =
        AppleScriptBracketedValueParser.isBracketStart(builder.tokenType) &&
            PermissiveCommandTailParser.consumeBracketedValue(builder)

    private fun parseSimpleLiteralValueBeforeSelector(builder: PsiBuilder): Boolean {
        val isBoundedLiteral =
            builder.tokenType === STRING_LITERAL &&
                (
                    builder.lookAhead(1) === OF ||
                        FallbackCommandParameterTokens.isParameterSelectorStart(builder.lookAhead(1))
                )
        if (isBoundedLiteral) {
            builder.advanceLexer()
        }
        return isBoundedLiteral
    }

    private fun parseSingleIdentifierValueBeforeBareSelector(builder: PsiBuilder): Boolean {
        if (builder.tokenType !== VAR_IDENTIFIER) return false

        val marker = builder.mark()
        builder.advanceLexer()
        val isBoundedValue =
            builder.tokenType === VAR_IDENTIFIER &&
                AppleScriptParserPhrases.isKnownFallbackBareSelectorStart(builder)
        if (isBoundedValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isBoundedValue
    }

    private fun parseParameterSelector(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseParameterSelector")) return false
        val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter selector>")
        val result = parseSelectorTokens(builder)
        exit_section_(builder, level, marker, COMMAND_PARAMETER_SELECTOR, result, false, null)
        return result
    }

    private fun parseDictionarySelectorTokens(builder: PsiBuilder): Boolean {
        val parameterNames =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_NAMES).orEmpty()
        return parameterNames
            .asSequence()
            .map { parameterName -> parameterName.split(' ') }
            .sortedByDescending { words -> words.size }
            .any { words -> AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, words) }
    }

    private fun parseBareParameterSelector(builder: PsiBuilder) {
        when {
            builder.tokenText.equals("default", ignoreCase = true) &&
                (builder.lookAhead(1) === VAR_IDENTIFIER || builder.lookAhead(1) === BUILT_IN_TYPE_S) -> {
                builder.advanceLexer()
                builder.advanceLexer()
            }
            AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "starting", "at") -> Unit
            AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "sound", "name") -> Unit
            AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "giving", "up", "after") -> Unit
            else -> builder.advanceLexer()
        }
    }
}
