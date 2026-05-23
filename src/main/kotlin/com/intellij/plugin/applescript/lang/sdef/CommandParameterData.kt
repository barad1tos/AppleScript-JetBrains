package com.intellij.plugin.applescript.lang.sdef

/**
 * Immutable value-type backing for `CommandParameter`.
 *
 * Plan 02-03 introduces this leaf as the Hybrid-pattern data half (D-01); the
 * existing `CommandParameter` interface and its `CommandParameterImpl` PSI
 * wrapper are NOT touched in this plan — wiring happens in plan 02-04.
 *
 * Guards:
 * - PITFALLS §1.1 (SAFE): pure value type with no PSI inheritance.
 * - PITFALLS §1.3: `@ConsistentCopyVisibility` keeps `copy()` aligned with
 *   the internal primary constructor so external callers must go through
 *   the parser-driven construction path.
 */
@ConsistentCopyVisibility
data class CommandParameterData internal constructor(
    val name: String,
    val code: String,
    val type: String,
    val optional: Boolean = false,
    val description: String? = null,
)
