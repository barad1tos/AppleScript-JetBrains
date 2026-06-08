// AUDIT 2026-05-24: scanned for EDT-context pre-check assumptions. Found `isDispatchThread`
// references at lines 44 and 61 — both are POSITIVE assertions inside `invokeAndWait { ... }`
// blocks (asserting "we ARE on the EDT now", which IS true after invokeAndWait jumps to EDT).
// These are correct usage, NOT the defective pattern fixed in Plan 03-11. File is compatible
// with BasePlatformTestCase's EDT-by-default threading model.
package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assume

/**
 * Proves the EDT guard at facade entry returns `emptyList()` when called from the EDT.
 * Without the guard, a future EDT caller would block the IDE for 2 seconds on the
 * `runBlockingCancellable` bridge.
 *
 * Heavy-gated because it exercises IntelliJ threading behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EdtBridgeGuardTest : BasePlatformTestCase() {
    private lateinit var testScope: TestScope

    override fun setUp() {
        Assume.assumeTrue(
            "EdtBridgeGuardTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        testScope = TestScope()
        Disposer.register(testRootDisposable) { testScope.cancel() }
        ApplicationManager.getApplication().replaceService(
            AppleScriptSystemDictionaryRegistryService::class.java,
            AppleScriptSystemDictionaryRegistryService(testScope, StandardTestDispatcher(testScope.testScheduler)),
            testRootDisposable,
        )
    }

    fun testFindStdCommandsReturnsEmptyWhenCalledFromEdt() {
        var resultFromEdt: Collection<*>? = null
        ApplicationManager.getApplication().invokeAndWait {
            assertTrue(
                "Pre-check: must be on EDT here",
                ApplicationManager.getApplication().isDispatchThread,
            )
            resultFromEdt = SdefIndexService.getInstance().findStdCommands(project, "anything")
        }
        assertNotNull(resultFromEdt)
        assertTrue(
            "EDT guard must return emptyList() to avoid 2s freeze",
            resultFromEdt!!.isEmpty(),
        )
    }

    fun testFindApplicationCommandsReturnsEmptyWhenCalledFromEdt() {
        var resultFromEdt: List<*>? = null
        ApplicationManager.getApplication().invokeAndWait {
            assertTrue(
                "Pre-check: must be on EDT here",
                ApplicationManager.getApplication().isDispatchThread,
            )
            resultFromEdt = SdefIndexService.getInstance().findApplicationCommands(project, "Music", "play")
        }
        assertNotNull(resultFromEdt)
        assertTrue(
            "EDT guard must return emptyList() to avoid 2s freeze",
            resultFromEdt!!.isEmpty(),
        )
    }
}
