package com.intellij.plugin.applescript.lang.sdef

sealed interface DictionarySuite : DictionaryComponent {
    fun addCommand(command: AppleScriptCommand): Boolean

    fun addClass(appleScriptClass: AppleScriptClass): Boolean

    fun findClassByCode(code: String): AppleScriptClass?

    fun addProperty(property: AppleScriptPropertyDefinition): Boolean

    fun addEnumeration(enumeration: DictionaryEnumeration): Boolean

    fun addRecord(record: DictionaryRecord)

    fun findClassByPluralName(pluralForm: String): AppleScriptClass?
}
