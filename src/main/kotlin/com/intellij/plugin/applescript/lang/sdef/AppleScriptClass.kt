package com.intellij.plugin.applescript.lang.sdef

interface AppleScriptClass : DictionaryComponent {

    fun getContents(): List<AppleScriptClass>

    fun getProperties(): List<AppleScriptPropertyDefinition>

    fun setProperties(properties: List<@JvmSuppressWildcards AppleScriptPropertyDefinition>)

    override fun getSuite(): Suite

    fun getParentClassName(): String?

    fun getParentClass(): AppleScriptClass?

    fun getElementNames(): List<String>?

    fun getElements(): List<AppleScriptClass>

    fun getRespondingCommands(): List<AppleScriptCommand>

    fun getPluralClassName(): String

    fun setPluralClassName(pluralClassName: String): DictionaryClass
}
