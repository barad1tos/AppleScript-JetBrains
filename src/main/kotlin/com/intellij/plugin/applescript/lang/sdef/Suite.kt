package com.intellij.plugin.applescript.lang.sdef

interface Suite : DictionaryComponent {

    fun addClass(appleScriptClass: AppleScriptClass): Boolean

    fun getClassByName(name: String): AppleScriptClass?

    fun findClassByCode(code: String): AppleScriptClass?

    fun findCommandByCode(code: String): AppleScriptCommand?

    fun addProperty(property: AppleScriptPropertyDefinition): Boolean

    fun addEnumeration(enumeration: DictionaryEnumeration): Boolean

    fun addRecord(record: DictionaryRecord)

    fun addCommand(command: AppleScriptCommand): Boolean

    fun isHidden(): Boolean
}
