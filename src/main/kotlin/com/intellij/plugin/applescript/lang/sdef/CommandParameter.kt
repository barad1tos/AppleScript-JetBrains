package com.intellij.plugin.applescript.lang.sdef

/**
 * GROUP A (0 gen-implementer) SDEF leaf interface — Phase 5 (v1.4) property conversion (PSI-03).
 *
 * The three getters were converted from `fun getX()` to Kotlin `val` properties. The Java-visible
 * accessor names are preserved by **property-name choice** (NOT `@get:JvmName`): Kotlin 2.3.21 rejects
 * `@JvmName` on an abstract interface property accessor, as the pilot ([AppleScriptPropertyDefinition])
 * proved in 05-01. The name contract is locked by the reflective `PsiGetterJvmSignatureTest`, which
 * asserts the synthesized JVM names over the compiled bytecode.
 *
 * The `is`-prefix on [isOptional] is PRESERVED in the property name so the synthesized accessor stays
 * `isOptional()` (NOT `getOptional()`).
 */
sealed interface CommandParameter : DictionaryComponent {
    /** JVM-visible as `isOptional()` — `is`-prefix preserved. */
    val isOptional: Boolean

    /** JVM-visible as `getTypeSpecifier()`. */
    val typeSpecifier: String

    /** JVM-visible as `getMyCommand()`. */
    val myCommand: AppleScriptCommand
}
