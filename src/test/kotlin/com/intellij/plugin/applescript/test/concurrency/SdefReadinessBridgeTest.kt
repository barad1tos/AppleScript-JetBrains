package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.readiness.DictionaryReadinessTracker
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assume
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SdefReadinessBridgeTest : BasePlatformTestCase() {
    private lateinit var readiness: DictionaryReadinessTracker
    private lateinit var serviceScope: CoroutineScope

    override fun setUp() {
        Assume.assumeTrue(
            "SdefReadinessBridgeTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        readiness = DictionaryReadinessTracker()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        Disposer.register(testRootDisposable) { serviceScope.cancel() }
        ApplicationManager.getApplication().replaceService(
            DictionaryReadinessTracker::class.java,
            readiness,
            testRootDisposable,
        )
        ApplicationManager.getApplication().replaceService(
            SdefIndexService::class.java,
            SdefIndexService(serviceScope),
            testRootDisposable,
        )
    }

    fun testStandardGateUnblocks() {
        val lookup = backgroundLookup { SdefIndexService.getInstance().findStdCommands(project, "set") }
        assertStillWaiting(lookup)

        readiness.completeStandardReady()

        assertTrue(lookup.get(RELEASE_TIMEOUT_SECONDS, TimeUnit.SECONDS).isEmpty())
    }

    fun testLookupWaitsForAppGate() {
        val applicationName = "ReadinessBridgeMissingApp_${System.nanoTime()}"
        val lookup =
            backgroundLookup {
                SdefIndexService.getInstance().findApplicationCommands(project, applicationName, "play")
            }
        assertStillWaiting(lookup)

        readiness.completeStandardReady()
        assertStillWaiting(lookup)
        readiness.completeFailures()

        assertTrue(lookup.get(RELEASE_TIMEOUT_SECONDS, TimeUnit.SECONDS).isEmpty())
    }

    fun testFailureReleasesLookup() {
        val lookup = backgroundLookup { SdefIndexService.getInstance().findStdCommands(project, "set") }
        assertStillWaiting(lookup)

        readiness.completeFailures()

        assertTrue(lookup.get(RELEASE_TIMEOUT_SECONDS, TimeUnit.SECONDS).isEmpty())
    }

    fun testColdLookupTimesOut() {
        val lookup = backgroundLookup { SdefIndexService.getInstance().findStdCommands(project, "set") }
        assertStillWaiting(lookup)

        assertTrue(lookup.get(COLD_TIMEOUT_SECONDS, TimeUnit.SECONDS).isEmpty())
    }

    private fun <T> backgroundLookup(action: () -> T): Future<T> {
        val started = CountDownLatch(1)
        val lookup =
            ApplicationManager.getApplication().executeOnPooledThread<T> {
                assertFalse(
                    "Regression must exercise the non-EDT bridge",
                    ApplicationManager.getApplication().isDispatchThread,
                )
                ApplicationManager.getApplication().runReadAction(
                    Computable {
                        started.countDown()
                        action()
                    },
                )
            }
        assertTrue(
            "Background lookup did not start",
            started.await(WORKER_START_TIMEOUT_SECONDS, TimeUnit.SECONDS),
        )
        return lookup
    }

    private fun assertStillWaiting(lookup: Future<*>) {
        assertThrows(TimeoutException::class.java) {
            lookup.get(PENDING_ASSERT_MILLIS, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        private const val PENDING_ASSERT_MILLIS = 250L
        private const val WORKER_START_TIMEOUT_SECONDS = 5L
        private const val RELEASE_TIMEOUT_SECONDS = 1L
        private const val COLD_TIMEOUT_SECONDS = 3L
    }
}
