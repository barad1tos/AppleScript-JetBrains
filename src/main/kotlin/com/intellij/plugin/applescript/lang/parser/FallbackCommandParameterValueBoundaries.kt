package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_PROPERTY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

internal object FallbackCommandParameterValueBoundaries {
    fun hasBuiltInClassValueBeforeBoundary(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        val parsed = AppleScriptParser.builtInClassIdentifier(builder, 0)
        val hasBoundary =
            parsed &&
                (
                    isBuiltInClassValueBoundary(builder.tokenType) ||
                        (
                            AppleScriptParser.expression(builder, 0) &&
                                isBuiltInClassValueBoundary(builder.tokenType)
                        )
                )
        marker.rollbackTo()
        return hasBoundary
    }

    fun parseBuiltInClassValueBeforeBoundary(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        val marker = builder.mark()
        val parsed = AppleScriptParser.builtInClassIdentifier(builder, level + 1)
        val isCompleteValue =
            parsed &&
                (
                    isBuiltInClassValueBoundary(builder.tokenType) ||
                        (
                            AppleScriptParser.expression(builder, level + 1) &&
                                isBuiltInClassValueBoundary(builder.tokenType)
                        )
                )
        if (isCompleteValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isCompleteValue
    }

    fun hasPropertyReferenceValueBeforeBoundary(builder: PsiBuilder): Boolean {
        if (builder.tokenType !== BUILT_IN_PROPERTY) return false
        val marker = builder.mark()
        val parsed = AppleScriptParser.propertyReference(builder, 0)
        val hasBoundary = parsed && isPropertyReferenceValueBoundary(builder.tokenType)
        marker.rollbackTo()
        return hasBoundary
    }

    fun parsePropertyReferenceValueBeforeBoundary(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (builder.tokenType !== BUILT_IN_PROPERTY) return false
        val marker = builder.mark()
        val parsed = AppleScriptParser.propertyReference(builder, level + 1)
        val isCompleteValue = parsed && isPropertyReferenceValueBoundary(builder.tokenType)
        if (isCompleteValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isCompleteValue
    }

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

    fun hasGrammarValueBeforeBoundary(builder: PsiBuilder): Boolean =
        hasValueBeforeBoundary(builder) { candidateBuilder, level ->
            AppleScriptParser.arbitraryReference(candidateBuilder, level)
        } ||
            hasValueBeforeBoundary(builder) { candidateBuilder, level ->
                AppleScriptParser.everyRangeReference(candidateBuilder, level)
            } ||
            hasValueBeforeBoundary(builder) { candidateBuilder, level ->
                AppleScriptParser.everyElemReference(candidateBuilder, level)
            } ||
            hasValueBeforeBoundary(builder) { candidateBuilder, level ->
                AppleScriptParser.middleElemReference(candidateBuilder, level)
            } ||
            hasValueBeforeBoundary(builder) { candidateBuilder, level ->
                AppleScriptParser.relativeReference(candidateBuilder, level)
            } ||
            hasValueBeforeBoundary(builder) { candidateBuilder, level ->
                AppleScriptParser.indexReference(candidateBuilder, level)
            } ||
            hasValueBeforeBoundary(builder) { candidateBuilder, level ->
                AppleScriptParser.literalExpression(candidateBuilder, level)
            }

    private fun hasValueBeforeBoundary(
        builder: PsiBuilder,
        parseValue: (PsiBuilder, Int) -> Boolean,
    ): Boolean {
        val marker = builder.mark()
        val hasBoundary = parseValue(builder, 0) && isValueBoundary(builder.tokenType)
        marker.rollbackTo()
        return hasBoundary
    }

    fun hasIdentifierPhraseBeforeCommandSelector(builder: PsiBuilder): Boolean =
        identifierRunLength(builder).let { identifiers ->
            identifiers > 0 &&
                FallbackCommandParameterTokens.isCommandSelectorStart(builder.lookAhead(identifiers))
        }

    fun consumeIdentifierPhraseBeforeCommandSelector(builder: PsiBuilder): Boolean {
        val identifierCount = identifierRunLength(builder)
        val shouldConsume =
            identifierCount > 0 &&
                FallbackCommandParameterTokens.isCommandSelectorStart(builder.lookAhead(identifierCount))
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

    fun consumeIdentifierPhraseExpressionBeforeBoundary(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        val parsed = consumeIdentifierPhraseExpression(builder)
        val isCompleteValue = parsed && isValueBoundary(builder.tokenType)
        if (isCompleteValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isCompleteValue
    }

    private fun consumeIdentifierPhraseExpression(builder: PsiBuilder): Boolean {
        val firstOperandIdentifiers = consumeIdentifierPhraseOperand(builder)
        var isCompleteExpression = firstOperandIdentifiers > 0
        var hasIdentifierPhraseOperand = firstOperandIdentifiers > 1
        while (
            isCompleteExpression &&
            FallbackCommandParameterTokens.isExpressionContinuationStart(builder.tokenType)
        ) {
            builder.advanceLexer()
            val nextOperandIdentifiers = consumeIdentifierPhraseOperand(builder)
            isCompleteExpression = nextOperandIdentifiers > 0
            hasIdentifierPhraseOperand = hasIdentifierPhraseOperand || nextOperandIdentifiers > 1
        }
        return isCompleteExpression && hasIdentifierPhraseOperand
    }

    private fun consumeIdentifierPhraseOperand(builder: PsiBuilder): Int {
        val identifierCount = identifierRunLength(builder)
        if (identifierCount > 0) {
            repeat(identifierCount) {
                builder.advanceLexer()
            }
        }
        return identifierCount
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
            tokenType === RPAREN ||
            tokenType === RCURLY ||
            tokenType === THEN ||
            FallbackCommandParameterTokens.isParameterSelectorStart(tokenType)

    private fun isBuiltInClassValueBoundary(tokenType: IElementType?): Boolean =
        tokenType == null ||
            tokenType === NLS ||
            tokenType === COMMENT ||
            tokenType === RPAREN ||
            tokenType === RCURLY ||
            tokenType === THEN ||
            FallbackCommandParameterTokens.isCommandSelectorStart(tokenType)

    private fun isPropertyReferenceValueBoundary(tokenType: IElementType?): Boolean =
        FallbackCommandParameterTokens.isParameterSelectorStart(tokenType)
}
