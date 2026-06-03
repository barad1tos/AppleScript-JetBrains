package com.intellij.plugin.applescript.lang.formatter

import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode

class AppleScriptWrappingProcessor {
    @Suppress("UNUSED_PARAMETER")
    fun createChildWrap(
        child: ASTNode,
        defaultWrap: Wrap,
        childWrap: Wrap?,
    ): Wrap = defaultWrap
}
