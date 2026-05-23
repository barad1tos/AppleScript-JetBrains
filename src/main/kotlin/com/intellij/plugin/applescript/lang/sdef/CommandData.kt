package com.intellij.plugin.applescript.lang.sdef

/**
 * Immutable value-type backing for `AppleScriptCommand`.
 *
 * Plan 02-03 introduces this leaf alongside `CommandParameterData` and
 * `AppleScriptCommandBuilder`; plan 02-04 will wire it into the
 * `AppleScriptCommandImpl` PSI wrapper as `private val data: CommandData`
 * (D-01 Hybrid). Until then no PSI class references `CommandData`.
 *
 * Equality contract (closes the TODO at `ApplicationDictionaryImpl:177` —
 * D-02): the synthesised `equals` / `hashCode` covers every primary-
 * constructor field including `parameters`. Two commands with the same
 * name / code but different parameter signatures (overloads) are correctly
 * distinct, which lets `findAllCommandsWithName` return the full N-element
 * list without a name-only collision.
 *
 * Guards:
 * - PITFALLS §1.1 (SAFE): pure value type with no PSI inheritance.
 * - PITFALLS §1.2: `parameters` is `List<CommandParameterData>` (read-only),
 *   never `MutableList`. The builder defensive-copies the caller's list via
 *   `.toList()` before handing it to this constructor — see
 *   `AppleScriptCommandBuilder.parameters`.
 * - PITFALLS §1.3: `@ConsistentCopyVisibility` keeps `copy()` aligned with
 *   the internal primary constructor.
 * - PITFALLS §1.4: because every field is `val` and the parameters list
 *   reference is a frozen snapshot, `hashCode` is stable for the lifetime
 *   of the instance — safe to use as a `HashSet` element or `HashMap` key.
 *
 * Construction MUST go through `AppleScriptCommandBuilder` so the freeze
 * discipline is enforced at a single call site.
 */
@ConsistentCopyVisibility
data class CommandData internal constructor(
    val name: String,
    val code: String,
    val description: String? = null,
    val parameters: List<CommandParameterData> = emptyList(),
    val directParameter: CommandDirectParameter? = null,
    val result: CommandResult? = null,
)
