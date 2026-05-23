package com.intellij.plugin.applescript.lang.sdef

/**
 * Mutable builder that produces a frozen `CommandData` via `build()`.
 *
 * Plan 02-03 ships this as a standalone top-level class (D-05) in the same
 * package as `AppleScriptCommandImpl.kt` — no nested-class form, no
 * `builder/` sub-package. Plan 02-04 will route the existing PSI
 * mutators (`setParameters`, `setDirectParameter`, `setResult`) through
 * this builder so that `AppleScriptCommandImpl.data` is build-frozen the
 * moment the impl is first exposed externally.
 *
 * Guards:
 * - PITFALLS §1.4 + §1.2: `parameters()` defensive-copies the caller's
 *   list via `.toList()` so post-`build()` mutation of the source list
 *   cannot corrupt the frozen `CommandData.parameters`. The regression
 *   lock is `CommandDataEqualsTest.testBuilderFreezeDefensiveCopy`.
 * - RECURRING_PITFALLS Pattern K: the primary constructor takes two
 *   adjacent `String` parameters (`name`, `code`); callers SHOULD use
 *   named arguments (the test suite does — see
 *   `CommandDataEqualsTest.testBuilderProducesEqualCommandData`) to keep
 *   the value-class swap hole sealed even though the constructor itself
 *   is intentionally public for the SDEF_Parser façade.
 */
class AppleScriptCommandBuilder(
    private val name: String,
    private val code: String,
) {
    private var description: String? = null
    private var parameters: List<CommandParameterData> = emptyList()
    private var directParameter: CommandDirectParameter? = null
    private var result: CommandResult? = null

    fun description(d: String?): AppleScriptCommandBuilder = apply { this.description = d }

    /**
     * Capture the caller's parameter list as a frozen snapshot. The `.toList()`
     * call is load-bearing (PITFALLS §1.4) — without it a caller passing in a
     * `MutableList` could mutate `CommandData.parameters` after `build()` and
     * silently break the `HashSet` / `HashMap` contract for the resulting
     * `CommandData` instance.
     */
    fun parameters(p: List<CommandParameterData>): AppleScriptCommandBuilder =
        apply { this.parameters = p.toList() }

    fun directParameter(d: CommandDirectParameter?): AppleScriptCommandBuilder =
        apply { this.directParameter = d }

    fun result(r: CommandResult?): AppleScriptCommandBuilder = apply { this.result = r }

    fun build(): CommandData = CommandData(
        name = name,
        code = code,
        description = description,
        parameters = parameters,
        directParameter = directParameter,
        result = result,
    )
}
