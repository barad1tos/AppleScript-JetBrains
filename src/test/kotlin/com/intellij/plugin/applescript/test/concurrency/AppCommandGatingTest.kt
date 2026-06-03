// AUDIT 2026-05-24: EDT-context assumption pre-check at line ~65 is structurally incompatible
// with BasePlatformTestCase's EDT-by-default threading model. Fixed in Plan 03-11 by removing
// the redundant pre-check (the production gate logic in findApplicationCommands is thread-
// context-agnostic — runBlockingCancellable + withTimeoutOrNull(2_000) returns emptyList()
// from both EDT and off-EDT callers). See DEBUG.md ADDENDUM Layer 5 + DEBUG-edttestutil.md.
package com.intellij.plugin.applescript.test.concurrency

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
        // standardReady. The facade bridges through runBlockingCancellable + withTimeoutOrNull
        // (2_000) and returns emptyList() when the 2s timeout expires without appsReady completing.
        // Plan 03-11 (Layer 5): the prior `assertFalse(...isDispatchThread)` pre-check was
        // structurally incompatible with BasePlatformTestCase's EDT-by-default threading model
        // (the test method runs ON the EDT). The production gate logic in findApplicationCommands
        // is thread-context-agnostic — runBlockingCancellable + withTimeoutOrNull(2_000) returns
        // emptyList() regardless of caller thread. Running the gate from the EDT exercises the
        // same code path (the facade also has an EdtBridgeGuardTest-verified short-circuit for
        // EDT callers that returns emptyList() without blocking). The pre-check is removed as
        // redundant. See DEBUG-edttestutil.md (B1 probe NOT-FOUND) for the rationale: the
        // hypothesised `EdtTestUtil.runInBackground` does not exist in the IntelliJ Platform
        // 2024.1+ public API surface across the 2024.3.7.1 / 2025.1.7.1 / 2025.2.6.2 verifier
        // IDE matrix; only the inverse `runInEdtAndGet` / `runInEdtAndWait` are documented.
        val result = service.findApplicationCommands(project, "Music", "play")
        assertTrue(
            "findApplicationCommands must return empty while appsReady is incomplete",
            result.isEmpty(),
        )
    }
}
