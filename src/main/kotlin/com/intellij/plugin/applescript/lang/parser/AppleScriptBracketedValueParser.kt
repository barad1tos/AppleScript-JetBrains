package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RPAREN
import com.intellij.psi.tree.IElementType

internal data class BracketedValueScan(
    val endOffset: Int,
)

internal object AppleScriptBracketedValueParser {
    fun isBracketStart(tokenType: IElementType?): Boolean = tokenType === LCURLY || tokenType === LPAREN

    fun scan(
        builder: PsiBuilder,
        allowNewlines: Boolean,
    ): BracketedValueScan? {
        if (!isBracketStart(builder.tokenType)) return null

        val expectedClosers = mutableListOf<IElementType>()
        var offset = 0
        while (true) {
            when (val tokenType = builder.lookAhead(offset)) {
                null -> return null
                LCURLY -> expectedClosers += RCURLY
                LPAREN -> expectedClosers += RPAREN
                RCURLY, RPAREN -> {
                    val isExpectedCloser =
                        expectedClosers.isNotEmpty() &&
                            expectedClosers.removeAt(expectedClosers.lastIndex) === tokenType
                    if (!isExpectedCloser) return null
                    if (expectedClosers.isEmpty()) return BracketedValueScan(endOffset = offset + 1)
                }
                NLS -> if (!allowNewlines) return null
            }
            offset += 1
        }
    }

    fun consume(
        builder: PsiBuilder,
        allowNewlines: Boolean,
    ): Boolean {
        if (!isBracketStart(builder.tokenType)) return false

        val expectedClosers = mutableListOf<IElementType>()
        var consumedBalancedValue = false
        var shouldContinue = true
        while (!builder.eof() && shouldContinue) {
            val isValidToken =
                when (val tokenType = builder.tokenType) {
                    LCURLY -> {
                        expectedClosers += RCURLY
                        true
                    }
                    LPAREN -> {
                        expectedClosers += RPAREN
                        true
                    }
                    RCURLY, RPAREN -> {
                        expectedClosers.isNotEmpty() &&
                            expectedClosers.removeAt(expectedClosers.lastIndex) === tokenType
                    }
                    NLS -> allowNewlines || expectedClosers.isEmpty()
                    else -> true
                }
            if (isValidToken) {
                builder.advanceLexer()
                consumedBalancedValue = expectedClosers.isEmpty()
            }
            shouldContinue = isValidToken && !consumedBalancedValue
        }
        return consumedBalancedValue
    }
}
