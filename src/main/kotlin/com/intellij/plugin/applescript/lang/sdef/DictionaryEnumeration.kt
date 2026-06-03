package com.intellij.plugin.applescript.lang.sdef

interface DictionaryEnumeration : DictionaryComponent {
    fun getEnumerators(): List<DictionaryEnumerator>?

    fun setEnumerators(enumerators: List<@JvmSuppressWildcards DictionaryEnumerator>?)

    /** JVM-visible as `getSuite()`; narrows `DictionaryComponent.suite` to non-null (05-04 lockstep). */
    override val suite: Suite
}
