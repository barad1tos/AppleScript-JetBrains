package com.intellij.plugin.applescript.lang.lexer

import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

open class AppleScriptTokenType(
    @NonNls debugName: String,
) : IElementType(debugName, AppleScriptLanguage)
