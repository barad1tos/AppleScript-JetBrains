package com.intellij.plugin.applescript.test.sdef

import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.lang.sdef.CommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * SDEF-01 regression fence for the leaf value types.
 *
 * Covers PITFALLS §1.1 (the leaves SAFELY become `data class` because they have
 * no PSI inheritance), §1.2 (no `MutableList` field in primary constructor; both
 * leaves are scalar-only anyway) and §1.3 (`@ConsistentCopyVisibility` blocks
 * `copy()` from bypassing the internal constructor).
 *
 * The `testCommandResultDescriptionAffectsEquality` test in particular catches
 * the latent bug in the pre-Phase 2 `CommandResult` shape, where
 * `getDescription()` returned a hard-coded `null` regardless of the constructor
 * argument; converting to `data class` synthesises a real property getter and
 * the equality contract immediately exercises both fields.
 */
class LeafDataClassTest {

    @Test
    fun testCommandResultEquality() {
        val a = CommandResult(type = "text", description = "d")
        val b = CommandResult(type = "text", description = "d")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testCommandResultDescriptionAffectsEquality() {
        // SDEF-01 negative: catches accidental omission of `description` from the
        // primary constructor / accidental return of a constant from the getter
        // (latent bug in the pre-Phase 2 plain-class shape).
        val a = CommandResult(type = "text", description = "alpha")
        val b = CommandResult(type = "text", description = "beta")
        assertNotEquals(a, b)
    }

    @Test
    fun testCommandResultHashCodeStableInHashSet() {
        // PITFALLS §1.4 mini-regression lock for the leaf — same instance contents
        // must hash to the same bucket on re-insert.
        val a = CommandResult(type = "text", description = "d")
        val b = CommandResult(type = "text", description = "d")
        val set = hashSetOf(a, b)
        assertEquals(1, set.size)
    }

    @Test
    fun testCommandDirectParameterEquality() {
        // Use a JDK dynamic proxy as a zero-behaviour `AppleScriptCommand` stub:
        // `data class CommandDirectParameter` synthesises `equals` from all
        // primary-constructor properties including `myCommand`. Passing the
        // SAME proxy reference to both instances keeps equality grounded in
        // reference identity for that field, while every other field exercises
        // structural equality.
        val command = stubCommand()
        val a = CommandDirectParameter(
            myCommand = command,
            typeSpecifier = "text",
            description = "d",
            optional = false,
        )
        val b = CommandDirectParameter(
            myCommand = command,
            typeSpecifier = "text",
            description = "d",
            optional = false,
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testCommandDirectParameterOptionalAffectsEquality() {
        val command = stubCommand()
        val a = CommandDirectParameter(
            myCommand = command,
            typeSpecifier = "text",
            description = "d",
            optional = false,
        )
        val b = CommandDirectParameter(
            myCommand = command,
            typeSpecifier = "text",
            description = "d",
            optional = true,
        )
        assertNotEquals(a, b)
        assertFalse(a.isOptional())
        assertTrue(b.isOptional())
    }

    private fun stubCommand(): AppleScriptCommand =
        Proxy.newProxyInstance(
            AppleScriptCommand::class.java.classLoader,
            arrayOf(AppleScriptCommand::class.java),
        ) { _, _, _ -> null } as AppleScriptCommand
}
