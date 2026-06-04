// AUDIT 2026-05-24: scanned for EDT-context assumption patterns (assertFalse/assumeFalse on
// isDispatchThread, @RunsInEdt/EdtRule, ThreadingAssertions); none found. File is compatible
// with BasePlatformTestCase's EDT-by-default threading model. Audit performed during Plan 03-11
// (DEBUG.md ADDENDUM Layer 5 sweep).
package com.intellij.plugin.applescript.test.concurrency

import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assume
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Hammers [AppleScriptSystemDictionaryRegistryService] reader methods from N background threads
 * while the service initializes. Validates that the `ConcurrentHashMap` + `CountDownLatch`
 * combination holds up under contention — no `NullPointerException` from `HashMap.get`, no
 * infinite spin from concurrent `HashMap.put` resize, no thread stuck waiting on the latch
 * indefinitely.
 *
 * Gated behind `-PincludeHeavyTests=true` per the existing heavy-test convention (see
 * `ParserRegressionTest`). Run once to validate the v1.0.1 race fix; does not need to run on
 * every PR forever (per ARCHITECTURE.md §7 pre-requisite note).
 *
 * Phase 01 / Plan 02 — TDD RED phase artifact. Pairs with [ColdStartRegressionTest] in this
 * package. Against the v1.0.0 baseline (no latch, raw `HashMap` indexes) this test is
 * statistically expected to surface NPE / unterminated reader threads under 16-way contention;
 * see PITFALLS.md §7.2 for the failure modes. Against Plan 01-01's `ConcurrentHashMap` +
 * `CountDownLatch` fix the test must observe zero `Throwable` escapes and every reader thread
 * finishing within the wall-clock budget.
 */
class StressTest : BasePlatformTestCase() {
    override fun setUp() {
        Assume.assumeTrue(
            "StressTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
    }

    fun testConcurrentReadersDoNotThrowOrDeadlock() {
        AppleScriptSystemDictionaryRegistryService.getInstance()
        val indexService = SdefIndexService.getInstance()
        val readerCount = 16
        val durationMillis = 2_000L
        val deadline = System.currentTimeMillis() + durationMillis
        val firstFailure = AtomicReference<Throwable?>(null)
        val startGate = CountDownLatch(1)
        val finishedReaders = CountDownLatch(readerCount)
        val pool = Executors.newFixedThreadPool(readerCount)

        try {
            repeat(readerCount) {
                pool.submit {
                    try {
                        startGate.await()
                        while (System.currentTimeMillis() < deadline && firstFailure.get() == null) {
                            indexService.lookupStdCommand("set")
                            indexService.lookupStdLibClass("application")
                            indexService.lookupStdCommandWithPrefixExist("se")
                            indexService.lookupStdLibClassPluralName("applications")
                        }
                    } catch (throwable: Throwable) {
                        // Reader threads run outside the JUnit EDT; any escaped Throwable
                        // (NPE from HashMap.get, AssertionError from inconsistent state,
                        // even Error subclasses surfaced by the CHM/latch contract) is a
                        // test failure. Capture the first one; later failures are noise.
                        firstFailure.compareAndSet(null, throwable)
                    } finally {
                        finishedReaders.countDown()
                    }
                }
            }
            startGate.countDown()
            val finished = finishedReaders.await(durationMillis + 5_000L, TimeUnit.MILLISECONDS)
            assertTrue(
                "At least one reader thread did not finish within $durationMillis ms + 5 s " +
                    "grace — likely stuck on latch.await or HashMap resize spin",
                finished,
            )
            val failure = firstFailure.get()
            assertNull(
                "Reader thread threw: ${failure?.javaClass?.simpleName} — ${failure?.message}",
                failure,
            )
        } finally {
            pool.shutdownNow()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }
}
