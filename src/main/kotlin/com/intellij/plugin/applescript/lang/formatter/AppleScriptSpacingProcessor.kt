package com.intellij.plugin.applescript.lang.formatter

import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
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
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiImplUtil
import com.intellij.psi.codeStyle.CommonCodeStyleSettings

class AppleScriptSpacingProcessor(
    @Suppress("unused") private val myNode: ASTNode,
    private val mySettings: CommonCodeStyleSettings,
) {

    fun getSpacing(child1: AppleScriptBlock?, child2: AppleScriptBlock): Spacing? {
        if (child1 == null) return null

        val node1 = child1.node
        val type1 = node1.elementType
        val node2 = child2.node
        val parent2 = node2.treeParent.elementType
        val type2 = node2.elementType

        if (LCURLY === type1 || RCURLY === type2) return Spacing.createSpacing(0, 0, 0, true, 0)

        // handlerCall(params)
        if (LPAREN === type2 && HANDLER_POSITIONAL_PARAMETERS_CALL_EXPRESSION === parent2) {
            return addSingleSpaceIf(mySettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES, false)
        }
        if (LPAREN === type2 && HANDLER_POSITIONAL_PARAMETERS_DEFINITION === parent2) {
            return addSingleSpaceIf(mySettings.SPACE_BEFORE_METHOD_PARENTHESES, false)
        }
        if (IF === type1 && LPAREN === type2) {
            return addSingleSpaceIf(mySettings.SPACE_BEFORE_IF_PARENTHESES, false)
        }
        if (LPAREN === type1 || RPAREN === type2) return Spacing.createSpacing(0, 0, 0, true, 0)
        if (COMMA === type2) return Spacing.createSpacing(0, 0, 0, true, 0)
        if (type1 === IDENTIFIER && type2 === HANDLER_PARAMETER_LABEL) {
            return Spacing.createSpacing(1, 1, 0, true, 0)
        }
        if (type2 === COLON) return Spacing.createSpacing(0, 0, 0, true, 0)
        if ((KEYWORDS.contains(type1) || HANDLER_PARAMETER_LABEL === type1) && NLS !== type2) {
            return Spacing.createSpacing(1, 1, 0, true, 0)
        }
        if (BAND === type1 || BAND === type2) {
            return Spacing.createSpacing(1, 1, 0, true, 0)
        }
        return null
    }

    @Suppress("unused")
    private fun addLineBreak(): Spacing =
        Spacing.createSpacing(0, 0, 1, false, mySettings.KEEP_BLANK_LINES_IN_CODE)

    private fun addSingleSpaceIf(condition: Boolean, linesFeed: Boolean): Spacing {
        val spaces = if (condition) 1 else 0
        val lines = if (linesFeed) 1 else 0
        return Spacing.createSpacing(spaces, spaces, lines, mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE)
    }

    private companion object {
        @JvmStatic
        private fun isWhiteSpace(node: ASTNode?): Boolean =
            node != null && (AppleScriptPsiImplUtil.isWhiteSpaceOrNls(node) || node.textLength == 0)
    }
}
