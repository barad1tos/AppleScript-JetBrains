package com.intellij.plugin.applescript.lang.parser

import com.intellij.plugin.applescript.psi.AppleScriptTypes.ABOUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AGAINST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BAND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CURRENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIGITS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIV
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DOES_NOT_CONTAIN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DOT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ENDS_WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EQ
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FALSE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GIVEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INT_DIV
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_CONTAIN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_NOT_IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LAND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MINUS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MISSING_VALUE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MOD
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OVER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.PLUS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.POW
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RETURN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STAR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STARTS_BEGINS_WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TRUE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.UNDER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.USING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

internal object FallbackCommandParameterTokens {
    private val expressionContinuationStarts =
        TokenSet.create(
            AS,
            BAND,
            DIV,
            DOES_NOT_CONTAIN,
            ENDS_WITH,
            EQ,
            GE,
            GT,
            INT_DIV,
            IS_CONTAIN,
            IS_IN,
            IS_NOT_IN,
            LAND,
            LE,
            LOR,
            LT,
            MINUS,
            MOD,
            NE,
            PLUS,
            POW,
            STAR,
            STARTS_BEGINS_WITH,
        )

    fun isValueLiteralStart(tokenType: IElementType?): Boolean =
        isStructuredDirectParameterStart(tokenType) ||
            tokenType === RETURN ||
            tokenType === TRUE ||
            tokenType === FALSE ||
            tokenType === MISSING_VALUE ||
            tokenType === CURRENT

    fun isStructuredDirectParameterStart(tokenType: IElementType?): Boolean =
        tokenType === STRING_LITERAL ||
            tokenType === DIGITS ||
            tokenType === DOT ||
            AppleScriptBracketedValueParser.isBracketStart(tokenType)

    fun isDirectParameterStart(tokenType: IElementType?): Boolean =
        tokenType != null &&
            tokenType !== NLS &&
            tokenType !== COMMENT &&
            !isPrepositionParameterStart(tokenType)

    fun isPrepositionParameterStart(tokenType: IElementType?): Boolean =
        tokenType === TO ||
            tokenType === INTO ||
            tokenType === FROM ||
            tokenType === ON ||
            tokenType === WITH ||
            tokenType === WITHOUT ||
            tokenType === GIVEN ||
            tokenType === AS ||
            tokenType === USING ||
            tokenType === ABOUT ||
            tokenType === AGAINST ||
            tokenType === BY ||
            tokenType === OVER ||
            tokenType === UNDER

    fun isParameterSelectorStart(tokenType: IElementType?): Boolean =
        isPrepositionParameterStart(tokenType) || tokenType === VAR_IDENTIFIER

    fun isExpressionContinuationStart(tokenType: IElementType?): Boolean =
        tokenType != null && expressionContinuationStarts.contains(tokenType)
}
