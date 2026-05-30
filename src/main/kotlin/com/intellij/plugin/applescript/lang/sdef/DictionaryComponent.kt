package com.intellij.plugin.applescript.lang.sdef

import com.intellij.plugin.applescript.psi.AppleScriptComponent

/**
 * GROUP A (0 gen-implementer) SDEF supertype — Phase 5 (v1.4) property conversion (PSI-03). Every
 * GROUP A interface extends this, so it governs the `getName`/`getSuite` override lockstep across the
 * whole SDEF family.
 *
 * Pure no-arg getters became `val` properties; the Java-visible names are preserved by **property-name
 * choice** (NOT `@get:JvmName`, which is uncompilable on abstract interface accessors per 05-01) and
 * locked by the reflective `PsiGetterJvmSignatureTest`.
 *
 * Override-seam decisions (NON-NEGOTIABLE — verified by compile):
 *  - `getName()` stays `override fun getName()`. A Kotlin `override val name` on this INTERFACE
 *    "overrides nothing" (Kotlin 2.3.21): the platform `PsiNamedElement.getName()` Java getter is not
 *    surfaced as an overridable `val name` from an interface-supertype position, unlike at a concrete
 *    class. So per the 05-04 override-lockstep rule it is KEPT as `fun`. JVM name `getName()` unchanged.
 *  - [suite] is nullable here (`Suite?`); `AppleScriptCommand`/`AppleScriptClass` narrow it to non-null
 *    `override val suite: Suite` in lockstep this same wave.
 *  - [description] / [setDescription] → `var description` (setDescription returns Unit).
 *  - [setDictionaryDoc] stays `fun` — no matching getter, so it is not a property.
 */
sealed interface DictionaryComponent : AppleScriptComponent {

    /** JVM-visible as `getDocumentation()`. */
    val documentation: String

    /** JVM-visible as `getCode()`. */
    val code: String?

    /** JVM-visible as `getCocoaClassName()`. */
    val cocoaClassName: String?

    /** JVM-visible as `getName()` — overrides `PsiNamedElement.getName()`; kept `fun` (see KDoc). */
    override fun getName(): String

    /** List of psi-element identifiers for components with multi-word names. JVM-visible as `getNameIdentifiers()`. */
    val nameIdentifiers: List<String>

    /** JVM-visible as `getQualifiedPath()`. */
    val qualifiedPath: String

    /** Name, starting with suite code name and including component code (not necessarily unique). JVM-visible as `getQualifiedName()`. */
    val qualifiedName: String

    /** JVM-visible as `getDescription()` / `setDescription(String)`. */
    var description: String?

    /** JVM-visible as `getSuite()`; narrowed to non-null `Suite` by AppleScriptCommand/AppleScriptClass. */
    val suite: Suite?

    /** JVM-visible as `getDictionaryParentComponent()`. */
    val dictionaryParentComponent: DictionaryComponent?

    /** JVM-visible as `getType()`. */
    val type: String

    fun setDictionaryDoc(documentation: String?)

    /** JVM-visible as `getDictionary()`. */
    val dictionary: ApplicationDictionary
}
