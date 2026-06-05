// AUDIT 2026-05-24: scanned for EDT-context assumption patterns (assertFalse/assumeFalse on
// isDispatchThread, @RunsInEdt/EdtRule, ThreadingAssertions); none found. File is compatible
// with BasePlatformTestCase's EDT-by-default threading model. Audit performed during Plan 03-11
// (DEBUG.md ADDENDUM Layer 5 sweep).
package com.intellij.plugin.applescript.test.concurrency

import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assume

/**
 * Locks the latch contract: immediately after the service is instantiated, every index reader returns a
 * deterministic answer — either the fully-populated result (init already complete) OR the
 * empty-fallback (init not yet complete, `initLatch.count > 0L`). NEVER a `NullPointerException`, NEVER a
 * half-populated index hit where `containsKey` reports true but the inner `Set` value is not
 * yet visible.
 *
 * Motivation: `PITFALLS.md` §7.3 — `myFixture.complete()` tests serialize, production does not.
 * Without this regression lock, a future refactor could re-introduce read-before-init races
 * that test fixtures would not catch.
 *
 * Gated behind `-PincludeHeavyTests=true` per D-09.
 *
 * Phase 01 / Plan 02 — TDD GREEN-phase target. Pairs with [StressTest] in this package.
 * Against the v1.0.0 baseline (raw `HashMap`, no latch) a sufficiently aggressive cold-start
 * invocation can observe `containsKey == true` for a term whose inner `HashSet` value is
 * not yet fully published, manifesting as a downstream `NullPointerException` on
 * `findStdCommands`. The test EXISTS to lock that behaviour; Plan 01-01's latch + CHM make
 * it pass.
 */
class ColdStartRegressionTest : BasePlatformTestCase() {
    override fun setUp() {
        Assume.assumeTrue(
            "ColdStartRegressionTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
    }

    fun testReadersReturnDeterministicAnswerImmediatelyAfterInstantiation() {
        val indexService = SdefIndexService.getInstance()

        // Predicate: must be Boolean, never throw. Both `true` and `false` are valid —
        // `true` means init completed before this call; `false` means init still in flight
        // and the latch-gated reader returned the empty fallback. The fact that this line
        // executes without throwing is the assertion.
        val predicate = indexService.commandLookup.lookupStdCommand("set")

        // Resolver: must be a non-null Collection (possibly empty). The latch-gated reader
        // returns `emptyList()` while init is in flight; the populated reader returns the
        // matching command(s).
        val resolved = indexService.findStdCommands(project, "set")
        assertNotNull(
            "findStdCommands returned null — must always return a (possibly empty) Collection",
            resolved,
        )

        // Half-broken-state lock: if the predicate reports `true` ("set" is a known std
        // command), the resolver must NOT silently return an empty Collection. The two
        // observations come from indexes that the latch gates atomically — under Plan
        // 01-01 they are either BOTH populated (init done) or BOTH empty (init in flight).
        // A `predicate == true` AND `resolved.isEmpty()` outcome would prove the inner
        // `HashSet` value was visible to `containsKey` but not yet published as a value
        // reference — exactly the failure mode PITFALLS.md §7.3 calls out.
        if (predicate) {
            assertFalse(
                "Predicate reports 'set' is a std command but resolver returned empty — " +
                    "this is the half-broken state the latch contract forbids " +
                    "(see PITFALLS.md §7.3)",
                resolved.isEmpty(),
            )
        }
    }
}
