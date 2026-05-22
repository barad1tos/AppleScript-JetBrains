package com.intellij.plugin.applescript.lang.formatter

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.codeStyle.CommonCodeStyleSettings

internal class AppleScriptIndentProcessor(@Suppress("unused") private val settings: CommonCodeStyleSettings) {

    fun getChildIndent(node: ASTNode): Indent {
        val elementType = node.elementType
        return if (elementType === AppleScriptTypes.BLOCK_BODY ||
            elementType === AppleScriptTypes.TOP_BLOCK_BODY ||
            elementType === AppleScriptTypes.SCRIPT_BODY
        ) {
            Indent.getNormalIndent()
        } else {
            Indent.getNoneIndent()
        }
    }
}
