package com.intellij.plugin.applescript.lang.sdef

/**
 * GROUP A (0 gen-implementer) SDEF interface — Phase 5 (v1.4) property conversion (PSI-03), PARSER
 * HOT PATH. [parameters] and [directParameter] are read from `AppleScriptGeneratedParserUtil.java`
 * (lines 366-370, 719-862) at parse time, so their Java-visible names `getParameters()` /
 * `getDirectParameter()` are load-bearing — a divergence would be a runtime `NoSuchMethodError`.
 *
 * The Java names are preserved by **property-name choice** (NOT `@get:JvmName`, which is uncompilable
 * on abstract interface accessors per 05-01) and locked by the reflective `PsiGetterJvmSignatureTest`.
 *
 * Conversion caveats (NON-NEGOTIABLE):
 *  - [getParameterByName] takes an argument → NOT a property; stays `fun`.
 *  - [setResult] RETURNS `CommandResult?` (not Unit) → cannot be a property setter; [result] is a
 *    read-only `val` and `setResult` stays a separate `fun`.
 *  - [parameters] / [directParameter] became `var` because their setters (`setParameters` /
 *    `setDirectParameter`) return `Unit` — Kotlin synthesizes `setParameters(...)` /
 *    `setDirectParameter(...)` for the matching `var`, preserving the existing mutation API.
 *  - [suite] narrows `DictionaryComponent.suite: Suite?` to non-null `Suite` — converted in lockstep
 *    with the supertype this wave (05-04). JVM-visible as `getSuite()`.
 *  - [code] narrows `DictionaryComponent.code: String?` to non-null `String`; SDEF commands always have
 *    a dictionary code, while top-level application dictionaries may still report `null`.
 */
interface AppleScriptCommand : DictionaryComponent {
    fun getParameterByName(name: String): CommandParameter?

    /** JVM-visible as `getParameterNames()`. */
    val parameterNames: List<String>

    /** JVM-visible as `getParameters()` / `setParameters(List)` — parser hot path. */
    var parameters: List<@JvmSuppressWildcards CommandParameter>

    /** JVM-visible as `getDirectParameter()` / `setDirectParameter(...)` — parser hot path. */
    var directParameter: CommandDirectParameter?

    /** JVM-visible as `getResult()`; paired mutator [setResult] stays `fun` (returns a value). */
    val result: CommandResult?

    fun setResult(result: CommandResult?): CommandResult?

    /** JVM-visible as `getMandatoryParameters()` — parser hot path. */
    val mandatoryParameters: List<CommandParameter>

    /** JVM-visible as `getSuite()`; narrows `DictionaryComponent.suite` to non-null. */
    override val suite: Suite

    /** JVM-visible as `getCode()`; narrows `DictionaryComponent.code` to non-null. */
    override val code: String
}
