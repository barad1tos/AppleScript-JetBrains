package com.intellij.plugin.applescript.test.concurrency

import com.intellij.plugin.applescript.lang.ide.sdef.DictionaryReadinessTracker
import com.intellij.plugin.applescript.lang.ide.sdef.DictionaryStartupActions
import com.intellij.plugin.applescript.lang.ide.sdef.DictionaryStartupPipeline
import com.intellij.plugin.applescript.lang.ide.sdef.DictionaryStartupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class DictionaryStartupPipelineTest {
    @Test
    fun runReturnsCompletedAfterPublishingReadinessAndRestartingDaemons() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val readiness = DictionaryReadinessTracker()
            val events = mutableListOf<String>()
            val pipeline =
                DictionaryStartupPipeline(
                    ioDispatcher = dispatcher,
                    readiness = readiness,
                    actions =
                        DictionaryStartupActions(
                            registerFileTypes = { events += "registerFileTypes" },
                            loadCachedDictionaries = { events += "loadCachedDictionaries" },
                            initializeStandardDictionaries = { events += "initializeStandardDictionaries" },
                            discoverInstalledApplicationNames = { events += "discoverInstalledApplicationNames" },
                            restartOpenProjectDaemons = { events += "restartOpenProjectDaemons" },
                        ),
                    reportRuntimeFailure = { failure -> fail("Unexpected startup failure: $failure") },
                )

            val result = pipeline.run()

            assertEquals(DictionaryStartupResult.Completed, result)
            assertEquals(
                listOf(
                    "registerFileTypes",
                    "loadCachedDictionaries",
                    "initializeStandardDictionaries",
                    "discoverInstalledApplicationNames",
                    "restartOpenProjectDaemons",
                ),
                events,
            )
            assertTrue(readiness.isStandardReady(), "standard dictionaries should be ready after startup")
            assertTrue(readiness.areAppsReady(), "application dictionaries should be ready after startup")
        }

    @Test
    fun runReturnsFailedAndPublishesReadinessFailuresForRuntimeException() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val readiness = DictionaryReadinessTracker()
            val failure = IllegalStateException("cache load failed")
            var reportedFailure: RuntimeException? = null
            var daemonRestarted = false
            val pipeline =
                DictionaryStartupPipeline(
                    ioDispatcher = dispatcher,
                    readiness = readiness,
                    actions =
                        DictionaryStartupActions(
                            registerFileTypes = {},
                            loadCachedDictionaries = { throw failure },
                            initializeStandardDictionaries = { fail("Standard dictionaries should not initialize") },
                            discoverInstalledApplicationNames = { fail("Application discovery should not run") },
                            restartOpenProjectDaemons = { daemonRestarted = true },
                        ),
                    reportRuntimeFailure = { reportedFailure = it },
                )

            val result = pipeline.run()

            val failed = assertInstanceOf(DictionaryStartupResult.Failed::class.java, result)
            assertSame(failure, failed.failure)
            assertSame(failure, reportedFailure)
            assertFalse(daemonRestarted, "daemon restart should not run after startup failure")
            assertTrue(readiness.awaitStandardReady().isFailure, "standard readiness should complete as failed")
            assertTrue(readiness.awaitAppsReady().isFailure, "app readiness should complete as failed")
        }

    @Test
    fun runPropagatesCancellationWithoutReportingStartupFailure() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val readiness = DictionaryReadinessTracker()
            val cancellation = CancellationException("startup cancelled")
            var failureReported = false
            val pipeline =
                DictionaryStartupPipeline(
                    ioDispatcher = dispatcher,
                    readiness = readiness,
                    actions =
                        DictionaryStartupActions(
                            registerFileTypes = { throw cancellation },
                            loadCachedDictionaries = { fail("Cached dictionaries should not load") },
                            initializeStandardDictionaries = { fail("Standard dictionaries should not initialize") },
                            discoverInstalledApplicationNames = { fail("Application discovery should not run") },
                            restartOpenProjectDaemons = { fail("Daemon restart should not run") },
                        ),
                    reportRuntimeFailure = { failureReported = true },
                )

            try {
                pipeline.run()
                fail("Cancellation should propagate out of the startup pipeline")
            } catch (actual: CancellationException) {
                assertEquals(cancellation.message, actual.message)
            }

            assertFalse(failureReported, "cancellation should not be reported as startup failure")
            assertFalse(readiness.isStandardReady(), "standard readiness should remain pending after cancellation")
            assertFalse(readiness.areAppsReady(), "app readiness should remain pending after cancellation")
        }

    @Test
    fun runPreservesStandardReadinessWhenApplicationDiscoveryFails() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val readiness = DictionaryReadinessTracker()
            val failure = IllegalStateException("application discovery failed")
            var reportedFailure: RuntimeException? = null
            var daemonRestarted = false
            val pipeline =
                DictionaryStartupPipeline(
                    ioDispatcher = dispatcher,
                    readiness = readiness,
                    actions =
                        DictionaryStartupActions(
                            registerFileTypes = {},
                            loadCachedDictionaries = {},
                            initializeStandardDictionaries = {},
                            discoverInstalledApplicationNames = { throw failure },
                            restartOpenProjectDaemons = { daemonRestarted = true },
                        ),
                    reportRuntimeFailure = { reportedFailure = it },
                )

            val result = pipeline.run()

            val failed = assertInstanceOf(DictionaryStartupResult.Failed::class.java, result)
            assertSame(failure, failed.failure)
            assertSame(failure, reportedFailure)
            assertFalse(daemonRestarted, "daemon restart should not run after application discovery failure")
            assertTrue(readiness.awaitStandardReady().isSuccess, "standard readiness should stay successful")
            assertTrue(readiness.awaitAppsReady().isFailure, "app readiness should complete as failed")
        }
}
