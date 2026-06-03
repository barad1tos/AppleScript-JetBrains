// AUDIT 2026-05-24: scanned for EDT-context assumption patterns (assertFalse/assumeFalse on
// isDispatchThread, @RunsInEdt/EdtRule, ThreadingAssertions); none found. File is compatible
// with BasePlatformTestCase's EDT-by-default threading model. Audit performed during Plan 03-11
// (DEBUG.md ADDENDUM Layer 5 sweep).
package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assume

/**
 * Review MEDIUM 2 rename of the original `ServiceScopeLifecycleTest`. Validates that the manual
 * cleanup pattern (parent-disposable cancels a manually-constructed TestScope) works for arbitrary
 * parent-disposable wiring. Distinct from the new [ServiceScopeLifecycleIntegrationTest] which
 * validates the actual `@Service(Service.Level.APP)` Platform-injected scope path.
 *
 * Heavy-gated per Phase 1 D-09 convention.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManualServiceScopeCancellationTest : BasePlatformTestCase() {
    override fun setUp() {
        Assume.assumeTrue(
            "ManualServiceScopeCancellationTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
    }

    fun testManualScopeCancelsOnDispose() =
        runTest {
            val parent = Disposer.newDisposable("test-parent")
            val testScope = TestScope()
            val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
            Disposer.register(parent) { testScope.cancel() }

            val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
            val launchedJob: Job =
                testScope.coroutineContext[Job]
                    ?: error("TestScope must expose its Job")
            assertFalse("Job should be active before dispose", launchedJob.isCancelled)

            Disposer.dispose(parent)

            assertTrue("Job MUST be cancelled after parent dispose", launchedJob.isCancelled)
            assertFalse(service.isInitialized())
            assertFalse(service.areAppDictionariesIndexed())
        }
}
