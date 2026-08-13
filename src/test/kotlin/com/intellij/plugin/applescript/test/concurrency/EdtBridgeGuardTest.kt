package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.readiness.DictionaryReadinessTracker
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assume
import kotlin.system.measureTimeMillis

/**
 * Proves the index lookup guards return immediately with `emptyList()` on the EDT.
 * Without them, a future EDT caller would block the IDE for 2 seconds on the bounded
 * readiness bridge.
 *
 * Heavy-gated because it exercises IntelliJ threading behavior.
 */
class EdtBridgeGuardTest : BasePlatformTestCase() {
    private lateinit var testScope: TestScope

    override fun setUp() {
        Assume.assumeTrue(
            "EdtBridgeGuardTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        testScope = TestScope()
        val readiness = DictionaryReadinessTracker()
        Disposer.register(testRootDisposable) { testScope.cancel() }
        ApplicationManager.getApplication().replaceService(
            DictionaryReadinessTracker::class.java,
            readiness,
            testRootDisposable,
        )
        ApplicationManager.getApplication().replaceService(
            AppleScriptSystemDictionaryRegistryService::class.java,
            AppleScriptSystemDictionaryRegistryService(
                testScope,
                StandardTestDispatcher(testScope.testScheduler),
                readiness = readiness,
            ),
            testRootDisposable,
        )
    }

    fun testStdLookupReturnsOnEdt() {
        var resultFromEdt: Collection<*>? = null
        val elapsedMillis =
            measureTimeMillis {
                ApplicationManager.getApplication().invokeAndWait {
                    assertTrue(
                        "Pre-check: must be on EDT here",
                        ApplicationManager.getApplication().isDispatchThread,
                    )
                    resultFromEdt = SdefIndexService.getInstance().findStdCommands(project, "anything")
                }
            }
        assertTrue(
            "EDT standard lookup must return before the 2s readiness timeout; elapsed=${elapsedMillis}ms",
            elapsedMillis < MAX_EDT_LOOKUP_MILLIS,
        )
        assertNotNull(resultFromEdt)
        assertTrue(
            "EDT guard must return emptyList() to avoid 2s freeze",
            resultFromEdt!!.isEmpty(),
        )
    }

    fun testAppLookupReturnsOnEdt() {
        var resultFromEdt: List<*>? = null
        val elapsedMillis =
            measureTimeMillis {
                ApplicationManager.getApplication().invokeAndWait {
                    assertTrue(
                        "Pre-check: must be on EDT here",
                        ApplicationManager.getApplication().isDispatchThread,
                    )
                    resultFromEdt = SdefIndexService.getInstance().findApplicationCommands(project, "Music", "play")
                }
            }
        assertTrue(
            "EDT application lookup must return before the 2s readiness timeout; elapsed=${elapsedMillis}ms",
            elapsedMillis < MAX_EDT_LOOKUP_MILLIS,
        )
        assertNotNull(resultFromEdt)
        assertTrue(
            "EDT guard must return emptyList() to avoid 2s freeze",
            resultFromEdt!!.isEmpty(),
        )
    }

    companion object {
        private const val MAX_EDT_LOOKUP_MILLIS = 1_000L
    }
}
