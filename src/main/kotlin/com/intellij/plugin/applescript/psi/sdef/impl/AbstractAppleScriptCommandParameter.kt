package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiElementImpl
import com.intellij.plugin.applescript.psi.sdef.AppleScriptCommandHandlerParameter

open class AbstractAppleScriptCommandParameter(
    node: ASTNode,
) : AppleScriptPsiElementImpl(node),
    AppleScriptCommandHandlerParameter
