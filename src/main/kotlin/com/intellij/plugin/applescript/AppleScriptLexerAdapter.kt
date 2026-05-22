package com.intellij.plugin.applescript

import com.intellij.lexer.FlexAdapter
import com.intellij.plugin.applescript.lang.lexer._AppleScriptLexer

class AppleScriptLexerAdapter : FlexAdapter(_AppleScriptLexer(null))
