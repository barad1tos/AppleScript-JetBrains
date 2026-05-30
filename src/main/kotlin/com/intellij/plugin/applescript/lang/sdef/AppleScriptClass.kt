package com.intellij.plugin.applescript.lang.sdef

/**
 * GROUP A (0 gen-implementer) SDEF interface — Phase 5 (v1.4) property conversion (PSI-03).
 *
 * Pure no-arg getters became `val` properties (Java names preserved by property-name choice, locked by
 * `PsiGetterJvmSignatureTest`). The [properties] accessor is `var` because its setter `setProperties`
 * returns `Unit` (Kotlin synthesizes `setProperties(List)` for the matching `var`).
 *
 * Conversion caveats (NON-NEGOTIABLE):
 *  - [setPluralClassName] RETURNS `DictionaryClass` (not Unit) → cannot be a property setter; it stays
 *    `fun` while [pluralClassName] is the read-only `val` getter.
 *  - [suite] narrows `DictionaryComponent.suite: Suite?` to non-null `Suite` — converted in lockstep
 *    with the supertype this wave (05-04). JVM-visible as `getSuite()`.
 */
interface AppleScriptClass : DictionaryComponent {

    /** JVM-visible as `getContents()`. */
    val contents: List<AppleScriptClass>

    /** JVM-visible as `getProperties()` / `setProperties(List)`. */
    var properties: List<@JvmSuppressWildcards AppleScriptPropertyDefinition>

    /** JVM-visible as `getSuite()`; narrows `DictionaryComponent.suite` to non-null. */
    override val suite: Suite

    /** JVM-visible as `getParentClassName()`. */
    val parentClassName: String?

    /** JVM-visible as `getParentClass()`. */
    val parentClass: AppleScriptClass?

    /** JVM-visible as `getElementNames()`. */
    val elementNames: List<String>?

    /** JVM-visible as `getElements()`. */
    val elements: List<AppleScriptClass>

    /** JVM-visible as `getRespondingCommands()`. */
    val respondingCommands: List<AppleScriptCommand>

    /** JVM-visible as `getPluralClassName()`; paired mutator [setPluralClassName] stays `fun`. */
    val pluralClassName: String

    fun setPluralClassName(pluralClassName: String): DictionaryClass
}
