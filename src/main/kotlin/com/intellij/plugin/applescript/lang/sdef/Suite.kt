package com.intellij.plugin.applescript.lang.sdef

/**
 * GROUP A (0 gen-implementer) SDEF interface — Phase 5 (v1.4) property conversion (PSI-03).
 *
 * Suite barely converts: only [isHidden] is a pure no-arg getter and becomes a `val` property. Every
 * `add*`/`find*`/`get*ByName` member is arg-taking or a mutator and stays `fun` — no property exists for
 * an accessor that takes an argument. The Java-visible `isHidden()` name is preserved by property-name
 * choice (the `is`-prefix is kept so the synthesized accessor stays `isHidden()`, NOT `getHidden()`),
 * locked by the reflective `PsiGetterJvmSignatureTest`.
 */
interface Suite : DictionaryComponent {

    fun addClass(appleScriptClass: AppleScriptClass): Boolean

    fun getClassByName(name: String): AppleScriptClass?

    fun findClassByCode(code: String): AppleScriptClass?

    fun findCommandByCode(code: String): AppleScriptCommand?

    fun addProperty(property: AppleScriptPropertyDefinition): Boolean

    fun addEnumeration(enumeration: DictionaryEnumeration): Boolean

    fun addRecord(record: DictionaryRecord)

    fun addCommand(command: AppleScriptCommand): Boolean

    /** JVM-visible as `isHidden()` — `is`-prefix preserved. */
    val isHidden: Boolean
}
