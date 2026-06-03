package com.intellij.plugin.applescript.lang.formatter

import com.intellij.formatting.Spacing
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.KEYWORDS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BAND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COLON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMA
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_PARAMETER_LABEL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_POSITIONAL_PARAMETERS_CALL_EXPRESSION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_POSITIONAL_PARAMETERS_DEFINITION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RPAREN
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.tree.IElementType

class AppleScriptSpacingProcessor(
    private val mySettings: CommonCodeStyleSettings,
) {
    fun getSpacing(
        child1: AppleScriptBlock?,
        child2: AppleScriptBlock,
    ): Spacing? {
        if (child1 == null) return null

        val node1 = child1.node
        val type1 = node1.elementType
        val node2 = child2.node
        val parent2 = node2.treeParent.elementType
        val type2 = node2.elementType

        return braceSpacing(type1, type2)
            ?: handlerParenthesisSpacing(type2, parent2)
            ?: ifParenthesisSpacing(type1, type2)
            ?: parenthesisSpacing(type1, type2)
            ?: punctuationSpacing(type1, type2)
            ?: keywordSpacing(type1, type2)
            ?: operatorSpacing(type1, type2)
    }

    private fun braceSpacing(
        type1: IElementType,
        type2: IElementType,
    ): Spacing? =
        if (LCURLY === type1 || RCURLY === type2) {
            fixedSpacing(0)
        } else {
            null
        }

    // handlerCall(params)
    private fun handlerParenthesisSpacing(
        type2: IElementType,
        parent2: IElementType,
    ): Spacing? =
        when {
            LPAREN !== type2 -> null
            HANDLER_POSITIONAL_PARAMETERS_CALL_EXPRESSION === parent2 ->
                configurableSingleLineSpacing(mySettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES)
            HANDLER_POSITIONAL_PARAMETERS_DEFINITION === parent2 ->
                configurableSingleLineSpacing(mySettings.SPACE_BEFORE_METHOD_PARENTHESES)
            else -> null
        }

    private fun ifParenthesisSpacing(
        type1: IElementType,
        type2: IElementType,
    ): Spacing? =
        if (IF === type1 && LPAREN === type2) {
            configurableSingleLineSpacing(mySettings.SPACE_BEFORE_IF_PARENTHESES)
        } else {
            null
        }

    private fun parenthesisSpacing(
        type1: IElementType,
        type2: IElementType,
    ): Spacing? =
        if (LPAREN === type1 || RPAREN === type2) {
            fixedSpacing(0)
        } else {
            null
        }

    private fun punctuationSpacing(
        type1: IElementType,
        type2: IElementType,
    ): Spacing? =
        when {
            COMMA === type2 -> fixedSpacing(0)
            type1 === IDENTIFIER && type2 === HANDLER_PARAMETER_LABEL -> fixedSpacing(1)
            type2 === COLON -> fixedSpacing(0)
            else -> null
        }

    private fun keywordSpacing(
        type1: IElementType,
        type2: IElementType,
    ): Spacing? =
        if ((KEYWORDS.contains(type1) || HANDLER_PARAMETER_LABEL === type1) && NLS !== type2) {
            fixedSpacing(1)
        } else {
            null
        }

    private fun operatorSpacing(
        type1: IElementType,
        type2: IElementType,
    ): Spacing? =
        if (BAND === type1 || BAND === type2) {
            fixedSpacing(1)
        } else {
            null
        }

    private fun configurableSingleLineSpacing(condition: Boolean): Spacing {
        val spaces = if (condition) 1 else 0
        return Spacing.createSpacing(
            spaces,
            spaces,
            0,
            mySettings.KEEP_LINE_BREAKS,
            mySettings.KEEP_BLANK_LINES_IN_CODE,
        )
    }

    private fun fixedSpacing(spaces: Int): Spacing = Spacing.createSpacing(spaces, spaces, 0, true, 0)
}
