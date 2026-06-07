package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.parser.DictionaryCommandRegistry
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class ParserDictionaryLookupFreezeTest : BasePlatformTestCase() {
    private lateinit var testScope: TestScope

    override fun setUp() {
        super.setUp()
        testScope = TestScope()
        Disposer.register(testRootDisposable) { testScope.cancel() }
        ApplicationManager.getApplication().replaceService(
            AppleScriptSystemDictionaryRegistryService::class.java,
            AppleScriptSystemDictionaryRegistryService(
                testScope,
                StandardTestDispatcher(testScope.testScheduler),
            ),
            testRootDisposable,
        )
    }

    fun testParserRegistryDoesNotWaitForColdDictionaryReadinessFromBackgroundThread() {
        val application = ApplicationManager.getApplication()
        val future =
            application.executeOnPooledThread {
                assertFalse("Regression must exercise the non-EDT parser path", application.isDispatchThread)

                val elapsedMillis =
                    measureTimeMillis {
                        assertTrue(
                            DictionaryCommandRegistry
                                .findApplicationCommands(project, "Music", "play")
                                .isEmpty(),
                        )
                        assertTrue(DictionaryCommandRegistry.findStdCommands(project, "set").isEmpty())
                    }

                assertTrue(
                    "Parser dictionary lookup must not block on cold readiness; elapsed=${elapsedMillis}ms",
                    elapsedMillis < MAX_NONBLOCKING_LOOKUP_MILLIS,
                )
            }

        future.get(MAX_NONBLOCKING_LOOKUP_MILLIS, TimeUnit.MILLISECONDS)
    }

    companion object {
        private const val MAX_NONBLOCKING_LOOKUP_MILLIS = 750L
    }
}
