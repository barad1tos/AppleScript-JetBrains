package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COLON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DATE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EVENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FILE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LIST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SCRIPT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TAB
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

internal object PropertyLabelParser {
    fun parse(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parsePropertyLabelIdentifier")) return false

        val marker = builder.mark()
        val result = parseLabelWords(builder) && builder.tokenType === COLON
        if (result) marker.drop() else marker.rollbackTo()
        return result
    }

    private fun parseLabelWords(builder: PsiBuilder): Boolean {
        var hasLabel = false
        while (isLabelWord(builder.tokenType)) {
            builder.advanceLexer()
            hasLabel = true
        }
        return hasLabel
    }

    private fun isLabelWord(tokenType: IElementType?): Boolean =
        tokenType === VAR_IDENTIFIER ||
            tokenType === EVENT ||
            tokenType === TAB ||
            tokenType === FILE ||
            tokenType === DATE ||
            tokenType === LIST ||
            tokenType === SCRIPT
}
