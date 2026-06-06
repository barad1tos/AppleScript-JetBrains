package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GIVEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT
import com.intellij.psi.tree.IElementType

internal object FallbackCommandParameterParser {
    private enum class ParameterMode {
        OptionalDirectParameter,
        ParametersOnly,
    }

    fun parseParameters(
        builder: PsiBuilder,
        level: Int,
        commandName: String?,
    ): Boolean {
        val mode = commandName?.let(::parameterMode) ?: return false
        if (mode == ParameterMode.OptionalDirectParameter) {
            parseOptionalDirectParameter(builder, level + 1)
        }
        val previousFallbackContext =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS)
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS, true)
        try {
            parseCommandParameters(builder, level + 1)
        } finally {
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS,
                previousFallbackContext,
            )
        }
        return true
    }

    private fun parameterMode(commandName: String): ParameterMode? =
        when (commandName.lowercase()) {
            "make" -> ParameterMode.ParametersOnly
            "choose from list",
            "display dialog",
            "do shell script",
            "run script",
            "read",
            "move",
            "exists",
            "run",
            "delete",
            "execute",
            -> ParameterMode.OptionalDirectParameter
            else -> {
                if (commandName.contains(" ")) ParameterMode.OptionalDirectParameter else null
            }
        }

    private fun parseOptionalDirectParameter(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        consumeToken(builder, OF)
        if (!isDirectParameterStart(builder.tokenType)) return false

        val parameterMarker = enter_section_(builder, level, _NONE_, "<fallback direct parameter>")
        val parameterResult = AppleScriptParser.expression(builder, level + 1)
        exit_section_(builder, level, parameterMarker, DIRECT_PARAMETER_VAL, parameterResult, false, null)
        return parameterResult
    }

    private fun parseCommandParameters(
        builder: PsiBuilder,
        level: Int,
    ) {
        while (isPrepositionParameterStart(builder.tokenType) || builder.tokenType === VAR_IDENTIFIER) {
            val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter>")
            val result =
                parseParameterSelector(builder, level + 1) &&
                    AppleScriptParser.expression(builder, level + 1)
            exit_section_(builder, level, marker, COMMAND_PARAMETER, result, false, null)
            if (!result) return
        }
    }

    fun parseParameterSelector(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseParameterSelector")) return false
        val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter selector>")
        val result = parseSelectorTokens(builder)
        exit_section_(builder, level, marker, COMMAND_PARAMETER_SELECTOR, result, false, null)
        return result
    }

    fun parseSelectorTokens(builder: PsiBuilder): Boolean =
        when {
            isPrepositionParameterStart(builder.tokenType) -> {
                val selectorStart = builder.tokenType
                builder.advanceLexer()
                if (selectorStart === WITH && builder.tokenType === VAR_IDENTIFIER) {
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

    private fun parseBareParameterSelector(builder: PsiBuilder) {
        val firstWord = builder.tokenText.orEmpty()
        builder.advanceLexer()
        if (firstWord.equals("default", ignoreCase = true) && builder.tokenType === VAR_IDENTIFIER) {
            builder.advanceLexer()
            return
        }
        if (firstWord.equals("giving", ignoreCase = true) &&
            builder.tokenText.equals("up", ignoreCase = true) &&
            builder.lookAhead(1).hasText("after")
        ) {
            builder.advanceLexer()
            builder.advanceLexer()
        }
    }

    private fun isDirectParameterStart(tokenType: IElementType?): Boolean =
        tokenType != null &&
            tokenType !== NLS &&
            tokenType !== COMMENT &&
            !isPrepositionParameterStart(tokenType)

    private fun isPrepositionParameterStart(tokenType: IElementType?): Boolean =
        tokenType === TO ||
            tokenType === INTO ||
            tokenType === FROM ||
            tokenType === ON ||
            tokenType === WITH ||
            tokenType === WITHOUT ||
            tokenType === GIVEN

    private fun IElementType?.hasText(text: String) = this != null && toString().equals(text, ignoreCase = true)
}
