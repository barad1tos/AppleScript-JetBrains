package com.intellij.plugin.applescript.lang.formatter

import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CommonCodeStyleSettings

class AppleScriptWrappingProcessor(
    @Suppress("unused") private val myNode: ASTNode,
    @Suppress("unused") private val mySettings: CommonCodeStyleSettings,
) {
    @Suppress("UNUSED_PARAMETER")
    fun createChildWrap(child: ASTNode, defaultWrap: Wrap, childWrap: Wrap?): Wrap = defaultWrap
}
