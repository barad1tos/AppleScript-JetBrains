package com.intellij.plugin.applescript.test.sdef

import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandBuilder
import com.intellij.plugin.applescript.lang.sdef.CommandData
import com.intellij.plugin.applescript.lang.sdef.CommandParameterData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * SDEF-02 partial regression fence — `CommandData` structural equality and
 * builder freeze semantics.
 *
 * Fences the now-closed `AppleScriptCommand` equals/hashCode contract ("redefine
 * equals and hashCode for AppleScriptCommand"). The actual wiring of
 * `CommandData` into the PSI impl lives in plan 02-04; here we only assert
 * that the data class itself honours the structural contract and that
 * `AppleScriptCommandBuilder` produces a frozen instance:
 *
 * - `testCommandDataIncludesParameters` — D-02 root invariant: two commands
 *   with the same `name + code` but different parameter lists must NOT be
 *   equal. Closes the "overloaded commands collapsed by name-only equals" bug.
 * - `testBuilderProducesEqualCommandData` — round-trip: same inputs through
 *   the builder produce equal `CommandData`.
 * - `testBuilderFreezeDefensiveCopy` — PITFALLS §1.4 + §1.2: the builder MUST
 *   take a defensive `.toList()` snapshot of `parameters` so that post-build
 *   mutation of the caller's `MutableList` does NOT leak into the frozen
 *   `CommandData.parameters`.
 * - `testHashCodeStableInHashSet` — PITFALLS §1.4 regression lock: two
 *   independently-built same-content `CommandData` instances must collapse to
 *   one entry in a `HashSet`, proving stable structural `hashCode`.
 */
class CommandDataEqualsTest {
    private fun paramA() = CommandParameterData(name = "a", code = "----", type = "text", optional = false)

    private fun paramB() = CommandParameterData(name = "b", code = "----", type = "text", optional = false)

    @Test
    fun testCommandDataIncludesParameters() {
        val cmd1 =
            CommandData(
                name = "play",
                code = "play",
                description = null,
                parameters = listOf(paramA()),
                directParameter = null,
                result = null,
            )
        val cmd2 =
            CommandData(
                name = "play",
                code = "play",
                description = null,
                parameters = listOf(paramA(), paramB()),
                directParameter = null,
                result = null,
            )
        assertNotEquals(cmd1, cmd2)
        assertNotEquals(cmd1.hashCode(), cmd2.hashCode())
    }

    @Test
    fun testBuilderProducesEqualCommandData() {
        val a =
            AppleScriptCommandBuilder(name = "play", code = "play")
                .parameters(listOf(paramA(), paramB()))
                .build()
        val b =
            AppleScriptCommandBuilder(name = "play", code = "play")
                .parameters(listOf(paramA(), paramB()))
                .build()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testBuilderFreezeDefensiveCopy() {
        // PITFALLS §1.4 + §1.2: the builder must `.toList()` the incoming list
        // so subsequent mutation of the source `MutableList` does not corrupt
        // the frozen `CommandData.parameters` (which would in turn destabilise
        // `hashCode` and break the `HashSet`/`HashMap` contract).
        val params = mutableListOf(paramA())
        val cmd =
            AppleScriptCommandBuilder(name = "play", code = "play")
                .parameters(params)
                .build()
        params.add(paramB())
        assertEquals(
            "Builder did not freeze parameters (defensive .toList() missing)",
            1,
            cmd.parameters.size,
        )
    }

    @Test
    fun testHashCodeStableInHashSet() {
        val a =
            AppleScriptCommandBuilder(name = "play", code = "play")
                .parameters(listOf(paramA()))
                .build()
        val b =
            AppleScriptCommandBuilder(name = "play", code = "play")
                .parameters(listOf(paramA()))
                .build()
        val set = hashSetOf(a, b)
        assertEquals(1, set.size)
    }
}
