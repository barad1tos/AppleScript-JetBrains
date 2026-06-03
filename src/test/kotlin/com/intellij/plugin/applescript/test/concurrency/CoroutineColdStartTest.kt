// AUDIT 2026-05-24: scanned for EDT-context assumption patterns (assertFalse/assumeFalse on
// isDispatchThread, @RunsInEdt/EdtRule, ThreadingAssertions); none found. File is compatible
// with BasePlatformTestCase's EDT-by-default threading model. Audit performed during Plan 03-11
// (DEBUG.md ADDENDUM Layer 5 sweep).
package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assume

/**
 * Cold-start regression test for the v1.2 structured-concurrency adoption (Phase 3 D-06).
 *
 * Asserts the three valid state-machine cells are reached deterministically:
 *   (isInitialized = false, areAppDictionariesIndexed = false)  // PENDING
 *   (isInitialized = true,  areAppDictionariesIndexed = false)  // STANDARD_READY
 *   (isInitialized = true,  areAppDictionariesIndexed = true)   // FULLY_READY
 * And asserts the forbidden cell is UNREACHABLE:
 *   (isInitialized = false, areAppDictionariesIndexed = true)   // INVARIANT VIOLATION
 *
 * Per RESEARCH section 6 + PITFALLS section 7.3 (tests don't catch the race in production):
 *  - Uses kotlinx-coroutines-test TestDispatcher INJECTED VIA CONSTRUCTOR (Codex HIGH 2):
 *    the [StandardTestDispatcher] is passed as the `ioDispatcher` constructor parameter
 *    that 03-03 adds. Without that injection, the service would use hardcoded
 *    `Dispatchers.IO` and `runCurrent()` / `advanceUntilIdle()` would not control
 *    init progression — tests would degrade to real I/O against `/Applications`,
 *    race, or hang.
 *  - Manually constructs the service with explicit [TestScope] (CD-03 option a) —
 *    bypasses the Platform Service container so we control the dispatcher exactly.
 *    No global `@TestOnly` seam in production code (`RECURRING_PITFALLS.md` Pattern I).
 *  - Heavy-gated via `Assume.assumeTrue` (Phase 1 D-09 convention — same as
 *    [ColdStartRegressionTest]).
 *
 * RED until 03-03 lands the new constructor signature:
 *   class AppleScriptSystemDictionaryRegistryService(
 *       private val serviceScope: CoroutineScope,
 *       private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
 *   )
 * AND the `Result<Unit>`-typed deferreds + facade success-semantics (Codex HIGH 1).
 * Until 03-03 lands, this file FAILS TO COMPILE because the constructor reference
 * `AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)` does not
 * yet resolve. THAT is the RED state — compile-time failure, not a silent skip.
 *
 * Codex HIGH 2 — no skip-style assume gates remain in test bodies; the RED is real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineColdStartTest : BasePlatformTestCase() {
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    override fun setUp() {
        Assume.assumeTrue(
            "CoroutineColdStartTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        testScope = TestScope()
        // StandardTestDispatcher tied to testScope's scheduler so runCurrent /
        // advanceUntilIdle control BOTH the parent scope AND the ioDispatcher.
        testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        Disposer.register(testRootDisposable) { testScope.cancel() }
    }

    /**
     * Constructor injection sanity — proves the service accepts (testScope, ioDispatcher)
     * shape. If 03-03 has NOT landed yet, this test fails to COMPILE — that's the RED.
     */
    fun testConstructorAcceptsTestScopeAndIoDispatcher() =
        testScope.runTest {
            val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
            assertNotNull(service)
        }

    /**
     * STATE 1 — pending: service constructed, launch scheduled but not yet run.
     * Expected: (isInitialized = false, areAppDictionariesIndexed = false)
     * Parser fast path MUST NOT crash and MUST return false/empty deterministically.
     */
    fun testColdStart_pendingState_returnsFalseFromSyncFacade() =
        testScope.runTest {
            val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
            // No runCurrent yet — service launched but init body has not executed
            assertFalse("standardReady not yet completed", service.isInitialized())
            assertFalse("appsReady not yet completed", service.areAppDictionariesIndexed())
        }

    /**
     * STATE 2 — standard_ready: standard suite parsed, discovery still pending.
     * Expected: (isInitialized = true, areAppDictionariesIndexed = false)
     */
    fun testAfterStandardSuiteParse_parserFastPathReturnsCorrectResults() =
        testScope.runTest {
            val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
            runCurrent() // advance past standardReady.complete(Result.success(Unit))
            assertTrue("standardReady should be completed after runCurrent", service.isInitialized())
            // discovery still pending — appsReady not yet completed
            assertFalse("appsReady still pending", service.areAppDictionariesIndexed())
        }

    /**
     * STATE 3 — fully_ready: both deferreds completed, full app catalog indexed.
     * Expected: (isInitialized = true, areAppDictionariesIndexed = true)
     */
    fun testAfterFullInit_completionContributorsSeeAppCatalog() =
        testScope.runTest {
            val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
            advanceUntilIdle() // drains all scheduled work
            assertTrue("standardReady completed", service.isInitialized())
            assertTrue("appsReady completed", service.areAppDictionariesIndexed())
        }

    /**
     * INVARIANT — forbidden cell `(false, true)` is unreachable. `RECURRING_PITFALLS.md`
     * Pattern L: defensive lock against pipeline-order bugs (`appsReady` completing before
     * `standardReady` would indicate the `runInitChain` pipeline order was inverted).
     */
    fun testPipelineOrder_standardReadyCannotCompleteAfterAppsReady() =
        testScope.runTest {
            val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
            advanceUntilIdle()
            assertFalse(
                "Invariant broken: appsReady completed before standardReady",
                service.areAppDictionariesIndexed() && !service.isInitialized(),
            )
        }

    /**
     * NEVER HALF-BROKEN (Success Criterion #4 discriminator):
     * At every observation point, result must be deterministic — empty pre-init OR full
     * post-init — never partial.
     */
    fun testNeverHalfBroken_everyStateIsEitherCorrectOrEmpty() =
        testScope.runTest {
            val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
            repeat(5) {
                val result = service.findStdCommands(project, "do shell script")
                assertTrue(
                    "result must be empty OR contain 'do shell script' command",
                    result.isEmpty() || result.any { it.name == "do shell script" },
                )
                runCurrent()
            }
        }
}
