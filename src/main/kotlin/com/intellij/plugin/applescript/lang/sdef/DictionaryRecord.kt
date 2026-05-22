package com.intellij.plugin.applescript.lang.sdef

interface DictionaryRecord : DictionaryComponent {

    fun getProperties(): List<AppleScriptPropertyDefinition>

    fun setProperties(properties: List<@JvmSuppressWildcards AppleScriptPropertyDefinition>?)

    override fun getSuite(): Suite
}
