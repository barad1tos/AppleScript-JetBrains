package com.intellij.plugin.applescript.lang.sdef

/**
 * Immutable value-type backing for `DictionaryClass`.
 *
 * Plan 02-04 (D-01 Hybrid keystone) introduces this leaf as the data half of
 * the `DictionaryClass` PSI wrapper. `DictionaryClass` continues to extend
 * `AbstractDictionaryComponent` → `DictionaryComponentBase` → `FakePsiElement`
 * (the PSI hierarchy is unchanged) and holds `private val data: ClassDefinition`.
 * Every public accessor reads from `data`; the `elements` / `respondingCommands`
 * resolution that previously used a mutable `initialized: Boolean` flag is
 * now `by lazy(LazyThreadSafetyMode.SYNCHRONIZED)` (D-04).
 *
 * `properties` is a `var` here NOT because the data class itself is mutable
 * (it is not — `properties` becomes a `val` once stored, the var modifier
 * applies only to the `DictionaryClass.data` field reference) but because the
 * `setProperties` setter on the PSI impl needs to swap in a fresh
 * `ClassDefinition.copy(properties = …)` after the two-pass parser fills in
 * property tags in a second pass. See `DictionaryClass.setProperties` for the
 * routing pattern.
 *
 * Guards:
 * - PITFALLS §1.1 (SAFE): pure value type with no PSI inheritance.
 * - PITFALLS §1.2: every collection field is read-only `List<T>`.
 * - PITFALLS §1.3: `@ConsistentCopyVisibility` keeps `copy()` aligned with
 *   the internal primary constructor.
 * - PITFALLS §1.4: every field is `val`, so `hashCode` is stable for the
 *   lifetime of the instance.
 */
@ConsistentCopyVisibility
data class ClassDefinition internal constructor(
    val name: String,
    val code: String,
    val description: String? = null,
    val parentClassName: String? = null,
    val pluralClassName: String,
    val elementNames: List<String> = emptyList(),
    val respondingCommandNames: List<String> = emptyList(),
    val properties: List<AppleScriptPropertyDefinition> = emptyList(),
)
