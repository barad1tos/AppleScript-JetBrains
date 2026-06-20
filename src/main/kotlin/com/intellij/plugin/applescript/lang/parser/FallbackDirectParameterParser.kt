package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.psi.tree.IElementType

internal object FallbackDirectParameterParser {
    fun parseOptionalDirectParameter(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        consumeToken(builder, OF)
        if (!FallbackCommandParameterTokens.isDirectParameterStart(builder.tokenType)) return false

        val parameterMarker = enter_section_(builder, level, _NONE_, "<fallback direct parameter>")
        val parameterResult = parseValue(builder, level + 1)
        exit_section_(builder, level, parameterMarker, DIRECT_PARAMETER_VAL, parameterResult, false, null)
        return parameterResult
    }

    private fun parseValue(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        parseCompleteNumberLiteral(builder, level + 1) ||
            FallbackCommandParameterValueBoundaries.parseBuiltInClassValueBeforeBoundary(
                builder,
                level + 1,
            ) ||
            FallbackCommandParameterValueBoundaries.parsePropertyReferenceValueBeforeBoundary(
                builder,
                level + 1,
            ) ||
            FallbackCommandParameterValueBoundaries.consumeIdentifierPhraseBeforePrepositionSelector(builder) ||
            FallbackCommandParameterValueBoundaries.parseExpressionAtValueBoundary(builder, level + 1) ||
            parseStructuredBracketFallback(builder)

    private fun parseStructuredBracketFallback(builder: PsiBuilder): Boolean =
        AppleScriptBracketedValueParser.isBracketStart(builder.tokenType) &&
            PermissiveCommandTailParser.consumeBracketedValue(builder)

    private fun parseCompleteNumberLiteral(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        val marker = builder.mark()
        val parsed = AppleScriptParser.numberLiteralExpression(builder, level + 1)
        val isCompleteValue = parsed && isDirectParameterBoundary(builder.tokenType)
        if (isCompleteValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isCompleteValue
    }

    private fun isDirectParameterBoundary(tokenType: IElementType?): Boolean =
        tokenType == null ||
            tokenType === NLS ||
            tokenType === COMMENT ||
            FallbackCommandParameterTokens.isParameterSelectorStart(tokenType)
}
