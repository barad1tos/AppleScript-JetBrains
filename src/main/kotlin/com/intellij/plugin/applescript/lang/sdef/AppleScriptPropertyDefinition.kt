package com.intellij.plugin.applescript.lang.sdef

interface AppleScriptPropertyDefinition : DictionaryComponent {

    fun getPsiType(): PsiType

    fun isClassProperty(): Boolean

    fun isRecordProperty(): Boolean

    fun getMyClass(): AppleScriptClass?

    fun getMyRecord(): DictionaryRecord?

    fun setAccessType(accessType: AccessType?)

    fun getAccessType(): AccessType?

    fun getTypeSpecifier(): String
}
