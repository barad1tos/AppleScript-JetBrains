package com.intellij.plugin.applescript.lang.formatter

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptTypes

internal class AppleScriptIndentProcessor {
    fun getChildIndent(node: ASTNode): Indent {
        val parentElementType = node.treeParent?.elementType
        return if (parentElementType === AppleScriptTypes.BLOCK_BODY
        ) {
            Indent.getNormalIndent()
        } else {
            Indent.getNoneIndent()
        }
    }
}
