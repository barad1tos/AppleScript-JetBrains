package com.intellij.plugin.applescript.lang.sdef

interface DictionaryRecord : DictionaryComponent {

    fun getProperties(): List<AppleScriptPropertyDefinition>

    fun setProperties(properties: List<@JvmSuppressWildcards AppleScriptPropertyDefinition>?)

    /** JVM-visible as `getSuite()`; narrows `DictionaryComponent.suite` to non-null (05-04 lockstep). */
    override val suite: Suite
}
