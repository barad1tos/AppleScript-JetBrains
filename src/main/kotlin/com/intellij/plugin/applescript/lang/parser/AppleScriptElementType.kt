package com.intellij.plugin.applescript.lang.parser

import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

open class AppleScriptElementType(@NonNls debugName: String) :
    IElementType(debugName, AppleScriptLanguage)
