package com.intellij.plugin.applescript.lang.sdef

class CommandResult @JvmOverloads constructor(
    private val type: String,
    private val description: String? = null,
) {
    fun getType(): String = type
    fun getDescription(): String? = description
}
