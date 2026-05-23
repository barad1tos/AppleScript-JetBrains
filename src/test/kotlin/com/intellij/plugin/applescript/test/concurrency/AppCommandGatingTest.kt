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
 * Codex HIGH 5 — proves that completing `standardReady` alone does NOT unblock
 * `findApplicationCommands`. The app-scoped facade MUST wait on `appsReady`, NOT on
 * `standardReady`. Otherwise app completion/resolution would read an empty/partial
 * app catalog before discovery completes.
 *
 * Test strategy: construct the service with a TestDispatcher (so the launched
 * `runInitChain` does NOT run real I/O), hand-complete `standardReady` with
 * `Result.success(Unit)`, then invoke `findApplicationCommands` from a background
 * thread. The facade's `areAppDictionariesIndexed()` predicate returns false (because
 * `appsReady` is incomplete) and the bounded-wait expires after 2s returning
 * `emptyList()`.
 *
 * Heavy-gated per Phase 1 D-09 convention.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppCommandGatingTest : BasePlatformTestCase() {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    override fun setUp() {
        Assume.assumeTrue(
            "AppCommandGatingTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        testScope = TestScope()
        testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        Disposer.register(testRootDisposable) { testScope.cancel() }
    }

    fun testStandardReadyAloneDoesNotUnblockAppCommands() {
        val service = AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher)
        // Hand-complete standardReady only — simulate "standard suite parsed but app discovery
        // still pending". Bypasses runInitChain entirely (TestDispatcher never advances).
        service.standardReady.complete(Result.success(Unit))

        assertTrue(
            "standardReady should be reported as ready",
            service.isInitialized(),
        )
        assertFalse(
            "appsReady NOT completed; areAppDictionariesIndexed must be false",
            service.areAppDictionariesIndexed(),
        )

        // findApplicationCommands MUST NOT return contents — its gate is appsReady, not
        // standardReady. From a non-EDT context (test thread), the facade bridges through
        // runBlockingCancellable + withTimeoutOrNull(2_000) and returns emptyList() when
        // the 2s timeout expires without appsReady completing.
        assertFalse(
            "Pre-check: test thread must NOT be on the EDT",
            ApplicationManager.getApplication().isDispatchThread,
        )
        val result = service.findApplicationCommands(project, "Music", "play")
        assertTrue(
            "findApplicationCommands must return empty while appsReady is incomplete",
            result.isEmpty(),
        )
    }
}
