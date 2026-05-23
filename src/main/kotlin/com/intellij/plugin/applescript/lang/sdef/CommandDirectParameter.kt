package com.intellij.plugin.applescript.lang.sdef

/**
 * SDEF `<direct-parameter>` leaf — the implicit first parameter that follows
 * the verb in `tell` / direct-object syntax (e.g. `play "song name"`).
 *
 * Plan 02-03 (D-01 leaves) converts this to a `data class` for the same
 * reasons as `CommandResult` (see KDoc there). PITFALLS §1.1 SAFE because
 * `CommandDirectParameter` has no PSI inheritance.
 *
 * The `myCommand` back-reference is part of the structural equality contract
 * by reference identity (interface type); two direct parameters attached to
 * different commands are correctly distinct in caches. The pre-existing
 * positional constructor signature
 * `(AppleScriptCommand, String, String?, Boolean)` is preserved verbatim so
 * the sole call site at `SDEF_Parser.parseCommandTag` continues to compile
 * without modification.
 */
@ConsistentCopyVisibility
data class CommandDirectParameter internal constructor(
    val myCommand: AppleScriptCommand,
    val typeSpecifier: String,
    val description: String?,
    val optional: Boolean = false,
) {
    /**
     * The four data-class fields synthesise standard `getMyCommand()`,
     * `getTypeSpecifier()`, `getDescription()`, `getOptional()` accessors
     * for Java callers. Kotlin call sites already migrated to property
     * syntax in plan 02-03 (see `AppleScriptCommandImpl.getDocFooter`).
     *
     * Two name-stability forwarders are kept until plan 02-04 lands the
     * full Hybrid wiring:
     * - `getCommand()` — pre-Phase 2 accessor name (the property is
     *   `myCommand` to mirror the original private-field name; the public
     *   getter was historically `getCommand`).
     * - `isOptional()` — the Boolean property `optional` would synthesise
     *   `getOptional()` rather than `isOptional()` (Kotlin's `is`-prefix
     *   rule only fires for properties whose own name starts with `is`).
     *   Keep the `isOptional()` forwarder so the codebase's existing call
     *   convention is preserved during the v1.1/v1.4 transition.
     */
    fun getCommand(): AppleScriptCommand = myCommand

    fun isOptional(): Boolean = optional
}
