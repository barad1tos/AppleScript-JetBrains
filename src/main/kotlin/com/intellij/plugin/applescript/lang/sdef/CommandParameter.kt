package com.intellij.plugin.applescript.lang.sdef

interface CommandParameter : DictionaryComponent {

    fun isOptional(): Boolean

    fun getTypeSpecifier(): String

    fun getMyCommand(): AppleScriptCommand
}
