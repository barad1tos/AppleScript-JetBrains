package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assume

/**
 * Codex MEDIUM 1 + Gemini LOW 1 — proves the EDT guard at facade entry returns
 * `emptyList()` (no 2s freeze) when called from the EDT. Without the guard a future
 * EDT caller would block the IDE for 2 seconds on the `runBlockingCancellable` bridge.
 *
 * Heavy-gated per Phase 1 D-09 convention.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EdtBridgeGuardTest : BasePlatformTestCase() {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    override fun setUp() {
        Assume.assumeTrue(
            "EdtBridgeGuardTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        testScope = TestScope()
        testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        Disposer.register(testRootDisposable) { testScope.cancel() }
    }

    fun testFindStdCommandsReturnsEmptyWhenCalledFromEdt() {
        val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
        var resultFromEdt: Collection<*>? = null
        ApplicationManager.getApplication().invokeAndWait {
            assertTrue(
                "Pre-check: must be on EDT here",
                ApplicationManager.getApplication().isDispatchThread,
            )
            resultFromEdt = service.findStdCommands(project, "anything")
        }
        assertNotNull(resultFromEdt)
        assertTrue(
            "EDT guard must return emptyList() to avoid 2s freeze",
            resultFromEdt!!.isEmpty(),
        )
    }

    fun testFindApplicationCommandsReturnsEmptyWhenCalledFromEdt() {
        val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
        var resultFromEdt: List<*>? = null
        ApplicationManager.getApplication().invokeAndWait {
            assertTrue(
                "Pre-check: must be on EDT here",
                ApplicationManager.getApplication().isDispatchThread,
            )
            resultFromEdt = service.findApplicationCommands(project, "Music", "play")
        }
        assertNotNull(resultFromEdt)
        assertTrue(
            "EDT guard must return emptyList() to avoid 2s freeze",
            resultFromEdt!!.isEmpty(),
        )
    }
}
