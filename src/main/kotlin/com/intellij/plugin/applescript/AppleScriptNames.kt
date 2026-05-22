package com.intellij.plugin.applescript

object AppleScriptNames {

    const val UNNAMED_ELEMENT: String = "<unnamed>"

    @JvmStatic
    fun isIdentifierStart(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c == '|'
}
