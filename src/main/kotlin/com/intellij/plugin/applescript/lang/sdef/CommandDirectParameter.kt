package com.intellij.plugin.applescript.lang.sdef

class CommandDirectParameter @JvmOverloads constructor(
    private val myCommand: AppleScriptCommand,
    private val typeSpecifier: String,
    private val description: String?,
    private val optional: Boolean = false,
) {
    fun getTypeSpecifier(): String = typeSpecifier
    fun isOptional(): Boolean = optional
    fun getDescription(): String? = description
    fun getCommand(): AppleScriptCommand = myCommand
}
