package com.intellij.plugin.applescript.lang.sdef

import com.intellij.plugin.applescript.psi.AppleScriptComponent

interface DictionaryComponent : AppleScriptComponent {

    fun getDocumentation(): String

    fun getCode(): String?

    fun getCocoaClassName(): String?

    override fun getName(): String

    /** List of psi-element identifiers for components with multi-word names. */
    fun getNameIdentifiers(): List<String>

    fun getQualifiedPath(): String

    /** Name, starting with suite code name and including component code (not necessarily unique). */
    fun getQualifiedName(): String

    fun getDescription(): String?

    fun getSuite(): Suite?

    fun getDictionaryParentComponent(): DictionaryComponent?

    fun getType(): String

    fun setDescription(description: String?)

    fun setDictionaryDoc(documentation: String?)

    fun getDictionary(): ApplicationDictionary
}
