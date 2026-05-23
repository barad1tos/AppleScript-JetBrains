package com.intellij.plugin.applescript.lang.sdef

/**
 * Mutable builder that produces a frozen `SuiteDefinition` via `build()`.
 *
 * Plan 02-04 ships this as a standalone top-level class (D-05) in the same
 * package as `SuiteImpl.kt` — no nested-class form, no `builder/` sub-package.
 * The parser route is: `SuiteImpl(dictionary, code, name, hidden, description,
 * xmlTagSuite)` continues to be the constructor call shape used by
 * `SDEF_Parser.parseSuiteTag` (D-06 façade). Internally that constructor uses
 * this builder to produce the initial `data: SuiteDefinition`.
 *
 * Setter methods returning `this` keep the fluent style consistent with
 * `AppleScriptCommandBuilder` from plan 02-03.
 *
 * Guards:
 * - RECURRING_PITFALLS Pattern K: the primary constructor takes two adjacent
 *   `String` parameters (`name`, `code`); callers SHOULD use named arguments
 *   (`SuiteBuilder(name = …, code = …)`) to seal the value-class swap hole.
 *   The fluent setters are unambiguous because each takes a single parameter
 *   of a distinct type / role.
 */
class SuiteBuilder(
    private val name: String,
    private val code: String,
) {
    private var hidden: Boolean = false
    private var description: String? = null

    fun hidden(h: Boolean): SuiteBuilder = apply { this.hidden = h }

    fun description(d: String?): SuiteBuilder = apply { this.description = d }

    fun build(): SuiteDefinition = SuiteDefinition(
        name = name,
        code = code,
        hidden = hidden,
        description = description,
    )
}
