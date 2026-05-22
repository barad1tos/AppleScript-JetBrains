package com.intellij.plugin.applescript.lang.sdef

interface DictionaryEnumeration : DictionaryComponent {

    fun getEnumerators(): List<DictionaryEnumerator>?

    fun setEnumerators(enumerators: List<@JvmSuppressWildcards DictionaryEnumerator>?)

    override fun getSuite(): Suite
}
