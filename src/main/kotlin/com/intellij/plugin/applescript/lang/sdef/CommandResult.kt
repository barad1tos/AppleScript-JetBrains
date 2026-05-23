package com.intellij.plugin.applescript.lang.sdef

/**
 * SDEF `<result>` leaf — type + optional human-readable description.
 *
 * Plan 02-03 (D-01 leaves) converts this from a plain `class` with private
 * accessors to a `data class`. Two safety guards apply:
 *
 * - PITFALLS §1.1 (BLOCKER): SAFE here — `CommandResult` does NOT inherit
 *   from any PSI base (`FakePsiElement` / `AbstractDictionaryComponent`), so
 *   the synthesised `equals` / `hashCode` does not override platform identity.
 * - PITFALLS §1.3: `@ConsistentCopyVisibility` keeps the synthesised
 *   `copy()` aligned with the primary constructor's visibility, so external
 *   callers cannot bypass the parser-side construction path.
 *
 * The Java-style `getType()` / `getDescription()` accessors stay implicitly
 * available via Kotlin property synthesis (D-03 strict v1.1/v1.4 boundary —
 * no `@get:JvmName` rename in this milestone).
 *
 * Note on the pre-Phase 2 latent bug: the old plain-class shape declared
 * `fun getDescription(): String? = null`, ignoring the constructor argument
 * entirely. The `data class` synthesises a real `description` getter, fixing
 * the bug in passing and exercising both fields through the synthesised
 * `equals` contract (see `LeafDataClassTest.testCommandResultDescriptionAffectsEquality`).
 */
@ConsistentCopyVisibility
data class CommandResult internal constructor(
    val type: String,
    val description: String? = null,
)
