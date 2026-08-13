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
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assume

/**
 * Proves that completing `standardReady` alone does not unblock `findApplicationCommands` on the
 * EDT. The application lookup must remain gated by `appsReady`; otherwise completion and
 * resolution could read a partial application catalog before discovery completes.
 *
 * The background bounded-wait contract is covered by [SdefReadinessBridgeTest].
 */
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
        val readiness = DictionaryReadinessTracker()
        ApplicationManager.getApplication().replaceService(
            DictionaryReadinessTracker::class.java,
            readiness,
            testRootDisposable,
        )
        val service =
            AppleScriptSystemDictionaryRegistryService(
                testScope,
                testDispatcher,
                readiness = readiness,
            )
        ApplicationManager.getApplication().replaceService(
            AppleScriptSystemDictionaryRegistryService::class.java,
            service,
            testRootDisposable,
        )
        // Hand-complete standardReady only — simulate "standard suite parsed but app discovery
        // still pending". Bypasses startup entirely (TestDispatcher never advances).
        service.standardReady.complete(Result.success(Unit))

        assertTrue(
            "standardReady should be reported as ready",
            service.isInitialized(),
        )
        assertFalse(
            "appsReady NOT completed; areAppDictionariesIndexed must be false",
            service.areAppDictionariesIndexed(),
        )

        // BasePlatformTestCase invokes this method on the EDT, so the cold app gate must return
        // immediately without exposing standard-only contents.
        val result = SdefIndexService.getInstance().findApplicationCommands(project, "Music", "play")
        assertTrue(
            "findApplicationCommands must return empty while appsReady is incomplete",
            result.isEmpty(),
        )
    }
}
