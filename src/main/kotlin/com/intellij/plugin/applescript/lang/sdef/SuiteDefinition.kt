package com.intellij.plugin.applescript.lang.sdef

/**
 * Immutable value-type backing for `SuiteImpl`.
 *
 * Plan 02-04 (D-01 Hybrid keystone) introduces this leaf as the data half of
 * the `SuiteImpl` PSI wrapper. `SuiteImpl` continues to extend
 * `AbstractDictionaryComponent` → `DictionaryComponentBase` → `FakePsiElement`
 * (the PSI hierarchy is unchanged) and holds `private var data:
 * SuiteDefinition`. Every public accessor on `SuiteImpl` reads from `data`.
 * Setters route through `SuiteBuilder` to produce a fresh frozen
 * `SuiteDefinition`, so the impl is never observable in a half-built state.
 *
 * Note: only the scalar `code` / `name` / `hidden` / `description` fields and
 * the read-only command/class snapshot live in `SuiteDefinition`. The full
 * map cluster (commandDefinitionToCodeMap, classDefinitionsMap, etc.) stays
 * inside `SuiteImpl` for v1.1 because it is a building/lookup concern (mutated
 * via `addCommand` / `addClass` during the parser's two-pass walk, not a
 * post-parse invariant). v1.3 service split will lift those into
 * `SdefIndexService`; v1.1 leaves them in `SuiteImpl` to keep the diff small.
 *
 * Guards:
 * - PITFALLS §1.1 (SAFE): pure value type with no PSI inheritance.
 * - PITFALLS §1.2: every collection field is read-only `List<T>`.
 * - PITFALLS §1.3: `@ConsistentCopyVisibility` keeps `copy()` aligned with
 *   the internal primary constructor.
 * - PITFALLS §1.4: every field is `val`, so `hashCode` is stable once built.
 *
 * Construction MUST go through `SuiteBuilder`.
 */
@ConsistentCopyVisibility
data class SuiteDefinition internal constructor(
    val name: String,
    val code: String,
    val hidden: Boolean = false,
    val description: String? = null,
)
