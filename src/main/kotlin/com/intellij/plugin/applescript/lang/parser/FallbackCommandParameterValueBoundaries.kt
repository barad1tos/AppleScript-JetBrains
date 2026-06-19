package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

internal object FallbackCommandParameterValueBoundaries {
    fun parseExpressionAtValueBoundary(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        val marker = builder.mark()
        val parsed = AppleScriptParser.expression(builder, level + 1)
        val isCompleteValue = parsed && isValueBoundary(builder.tokenType)
        if (isCompleteValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isCompleteValue
    }

    fun hasIdentifierPhraseBeforePrepositionSelector(builder: PsiBuilder): Boolean =
        identifierRunLength(builder).let { identifiers ->
            identifiers > 0 &&
                FallbackCommandParameterTokens.isPrepositionParameterStart(builder.lookAhead(identifiers))
        }

    fun consumeIdentifierPhraseBeforePrepositionSelector(builder: PsiBuilder): Boolean {
        val identifierCount = identifierRunLength(builder)
        val shouldConsume =
            identifierCount > 0 &&
                FallbackCommandParameterTokens.isPrepositionParameterStart(builder.lookAhead(identifierCount))
        if (shouldConsume) {
            repeat(identifierCount) {
                builder.advanceLexer()
            }
        }
        return shouldConsume
    }

    fun consumeIdentifierPhraseBeforeStructuredBareSelector(builder: PsiBuilder): Boolean {
        val identifierCount = identifierRunLength(builder)
        val shouldConsume =
            identifierCount >= 2 &&
                AppleScriptBracketedValueParser.isBracketStart(builder.lookAhead(identifierCount))
        if (shouldConsume) {
            repeat(identifierCount - 1) {
                builder.advanceLexer()
            }
        }
        return shouldConsume
    }

    private fun identifierRunLength(builder: PsiBuilder): Int {
        var offset = 0
        while (builder.lookAhead(offset) === VAR_IDENTIFIER) {
            offset += 1
        }
        return offset
    }

    private fun isValueBoundary(tokenType: IElementType?): Boolean =
        tokenType == null ||
            tokenType === NLS ||
            tokenType === COMMENT ||
            FallbackCommandParameterTokens.isParameterSelectorStart(tokenType)
}
