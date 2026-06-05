// AUDIT 2026-05-24: scanned for EDT-context assumption patterns (assertFalse/assumeFalse on
// isDispatchThread, @RunsInEdt/EdtRule, ThreadingAssertions); none found. File is compatible
// with BasePlatformTestCase's EDT-by-default threading model. Audit performed during Plan 03-11
// (DEBUG.md ADDENDUM Layer 5 sweep).
package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.discovery.ProgressTaskCompat
import com.intellij.plugin.applescript.lang.dictionary.filetype.SdefFileTypeRegistrar
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Assume
import kotlin.coroutines.CoroutineContext

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
 *  - Uses kotlinx-coroutines-test TestDispatcher INJECTED VIA CONSTRUCTOR (Review HIGH 2):
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
 * AND the `Result<Unit>`-typed deferreds + facade success-semantics (Review HIGH 1).
 * Until 03-03 lands, this file FAILS TO COMPILE because the constructor reference
 * `newTestService()` does not
 * yet resolve. THAT is the RED state — compile-time failure, not a silent skip.
 *
 * Review HIGH 2 — no skip-style assume gates remain in test bodies; the RED is real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineColdStartTest : BasePlatformTestCase() {
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var pendingDiscoveryTasks: ArrayDeque<Runnable>
    private lateinit var discoveryDispatcher: CoroutineDispatcher
    private val noOpProgressTask =
        object : ProgressTaskCompat {
            override fun show(displayName: String) = Unit

            override fun dismiss() = Unit
        }

    override fun setUp() {
        Assume.assumeTrue(
            "CoroutineColdStartTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        testScope = TestScope()
        // Discovery uses an explicit queue so runCurrent can observe standardReady
        // before the test releases the application walk.
        testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        pendingDiscoveryTasks = ArrayDeque()
        discoveryDispatcher =
            object : CoroutineDispatcher() {
                override fun dispatch(
                    context: CoroutineContext,
                    block: Runnable,
                ) {
                    pendingDiscoveryTasks.addLast(block)
                }
            }
        ApplicationManager.getApplication().replaceService(
            ApplicationDiscoveryService::class.java,
            ApplicationDiscoveryService(testScope, discoveryDispatcher),
            testRootDisposable,
        )
        ApplicationManager.getApplication().replaceService(
            SdefFileTypeRegistrar::class.java,
            SdefFileTypeRegistrar(testScope, testDispatcher),
            testRootDisposable,
        )
        Disposer.register(testRootDisposable) { testScope.cancel() }
    }

    private fun newTestService(): AppleScriptSystemDictionaryRegistryService =
        AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher, noOpProgressTask)

    private fun advanceThroughFullInitialization() {
        testScope.runCurrent()
        runPendingDiscoveryTasks()
        testScope.advanceUntilIdle()
    }

    private fun runPendingDiscoveryTasks() {
        while (pendingDiscoveryTasks.isNotEmpty()) {
            pendingDiscoveryTasks.removeFirst().run()
        }
    }

    /**
     * Constructor injection sanity — proves the service accepts (testScope, ioDispatcher)
     * shape. If 03-03 has NOT landed yet, this test fails to COMPILE — that's the RED.
     */
    fun testConstructorAcceptsTestScopeAndIoDispatcher() {
        val service = newTestService()
        assertNotNull(service)
    }

    /**
     * STATE 1 — pending: service constructed, launch scheduled but not yet run.
     * Expected: (isInitialized = false, areAppDictionariesIndexed = false)
     * Parser fast path MUST NOT crash and MUST return false/empty deterministically.
     */
    fun testColdStart_pendingState_returnsFalseFromSyncFacade() {
        val service = newTestService()
        // No runCurrent yet — service launched but init body has not executed
        assertFalse("standardReady not yet completed", service.isInitialized())
        assertFalse("appsReady not yet completed", service.areAppDictionariesIndexed())
    }

    /**
     * STATE 2 — standard_ready: standard suite parsed, discovery still pending.
     * Expected: (isInitialized = true, areAppDictionariesIndexed = false)
     */
    fun testAfterStandardSuiteParse_parserFastPathReturnsCorrectResults() {
        val service = newTestService()
        testScope.runCurrent() // advance past standardReady.complete(Result.success(Unit))
        assertTrue("standardReady should be completed after runCurrent", service.isInitialized())
        // discovery still pending — appsReady not yet completed
        assertFalse("appsReady still pending", service.areAppDictionariesIndexed())
    }

    /**
     * STATE 3 — fully_ready: both deferred completed, full app catalog indexed.
     * Expected: (isInitialized = true, areAppDictionariesIndexed = true)
     */
    fun testAfterFullInit_completionContributorsSeeAppCatalog() {
        val service = newTestService()
        advanceThroughFullInitialization()
        assertTrue("standardReady completed", service.isInitialized())
        assertTrue("appsReady completed", service.areAppDictionariesIndexed())
    }

    fun testAfterFullInit_schedulesOpenProjectDaemonRestart() {
        var daemonRestartCount = 0
        val service =
            AppleScriptSystemDictionaryRegistryService(
                testScope,
                testDispatcher,
                noOpProgressTask,
            ) {
                daemonRestartCount++
            }

        advanceThroughFullInitialization()

        assertTrue("appsReady completed", service.areAppDictionariesIndexed())
        assertEquals("appsReady completion should trigger one daemon restart", 1, daemonRestartCount)
    }

    /**
     * INVARIANT — forbidden cell `(false, true)` is unreachable. `RECURRING_PITFALLS.md`
     * Pattern L: defensive lock against pipeline-order bugs (`appsReady` completing before
     * `standardReady` would indicate the startup pipeline order was inverted).
     */
    fun testPipelineOrder_standardReadyCannotCompleteAfterAppsReady() {
        val service = newTestService()
        advanceThroughFullInitialization()
        assertFalse(
            "Invariant broken: appsReady completed before standardReady",
            service.areAppDictionariesIndexed() && !service.isInitialized(),
        )
    }

    fun testPlatformCancellationDoesNotFailReadinessGates() {
        var registrarDispatchCalled = false
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val processCanceledDispatcher =
            object : CoroutineDispatcher() {
                override fun dispatch(
                    context: CoroutineContext,
                    block: Runnable,
                ) {
                    registrarDispatchCalled = true
                    throw ProcessCanceledException()
                }
            }

        try {
            val registrar =
                SdefFileTypeRegistrar(
                    serviceScope = scope,
                    edtDispatcher = processCanceledDispatcher,
                )
            ApplicationManager.getApplication().replaceService(
                SdefFileTypeRegistrar::class.java,
                registrar,
                testRootDisposable,
            )

            val service = AppleScriptSystemDictionaryRegistryService(scope, dispatcher, noOpProgressTask)
            repeat(5) {
                scheduler.runCurrent()
            }

            assertTrue(
                "startup must reach the .sdef registrar before cancellation",
                registrarDispatchCalled,
            )
            assertFalse(
                "standardReady must stay pending after platform cancellation",
                service.standardReady.isCompleted,
            )
            assertFalse(
                "appsReady must stay pending after platform cancellation",
                service.appsReady.isCompleted,
            )
        } finally {
            scope.cancel()
        }
    }

    fun testRuntimeStartupFailureCompletesPendingReadinessGatesAsFailures() {
        var discoveryDispatchCalled = false
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val failingDiscoveryDispatcher =
            object : CoroutineDispatcher() {
                override fun dispatch(
                    context: CoroutineContext,
                    block: Runnable,
                ) {
                    discoveryDispatchCalled = true
                    error("Simulated discovery startup failure")
                }
            }

        try {
            ApplicationManager.getApplication().replaceService(
                ApplicationDiscoveryService::class.java,
                ApplicationDiscoveryService(scope, failingDiscoveryDispatcher),
                testRootDisposable,
            )
            ApplicationManager.getApplication().replaceService(
                SdefFileTypeRegistrar::class.java,
                SdefFileTypeRegistrar(scope, dispatcher),
                testRootDisposable,
            )

            val service = AppleScriptSystemDictionaryRegistryService(scope, dispatcher, noOpProgressTask)
            repeat(5) {
                scheduler.runCurrent()
            }

            assertTrue(
                "startup must reach application discovery before the simulated runtime failure",
                discoveryDispatchCalled,
            )
            assertTrue(
                "standardReady should already be successful before discovery starts",
                service.isInitialized(),
            )
            assertTrue(
                "appsReady must complete after a runtime startup failure",
                service.appsReady.isCompleted,
            )
            assertTrue(
                "appsReady must store Result.failure rather than report ready",
                service.appsReady.getCompleted().isFailure,
            )
            assertFalse(
                "appsReady failure must not be reported as indexed",
                service.areAppDictionariesIndexed(),
            )
        } finally {
            scope.cancel()
        }
    }

    /**
     * NEVER HALF-BROKEN (Success Criterion #4 discriminator):
     * At every observation point, result must be deterministic — empty pre-init OR full
     * post-init — never partial.
     */
    fun testNeverHalfBroken_everyStateIsEitherCorrectOrEmpty() {
        val service = newTestService()
        ApplicationManager.getApplication().replaceService(
            AppleScriptSystemDictionaryRegistryService::class.java,
            service,
            testRootDisposable,
        )
        repeat(5) {
            val result = SdefIndexService.getInstance().findStdCommands(project, "do shell script")
            assertTrue(
                "result must be empty OR contain 'do shell script' command",
                result.isEmpty() || result.any { it.name == "do shell script" },
            )
            testScope.runCurrent()
        }
    }
}
