package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.parser.DictionaryCommandRegistry
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.test.service.SyntheticSuiteFixtures
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class ParserDictionaryLookupFreezeTest : BasePlatformTestCase() {
    private lateinit var testScope: TestScope

    override fun setUp() {
        super.setUp()
        testScope = TestScope()
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        Disposer.register(testRootDisposable) { testScope.cancel() }
        ApplicationManager.getApplication().replaceService(
            SdefIndexService::class.java,
            SdefIndexService(testScope),
            testRootDisposable,
        )
        ApplicationManager.getApplication().replaceService(
            AppleScriptSystemDictionaryRegistryService::class.java,
            AppleScriptSystemDictionaryRegistryService(
                testScope,
                testDispatcher,
            ),
            testRootDisposable,
        )
        project.replaceService(
            AppleScriptProjectDictionaryService::class.java,
            AppleScriptProjectDictionaryService(project),
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

    fun testParserRegistryDoesNotMaterializeReadyProjectDictionaries() {
        val applicationName = "SyntheticParserLookupApp_${System.nanoTime()}"
        val applicationDictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "parser-ready-app",
                SyntheticSuiteFixtures.musicAppPlayCommandXml(),
            )
        val standardDictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "parser-ready-standard-additions",
                SyntheticSuiteFixtures.standardAdditionsMinimalXml(),
            )
        val applicationInfo = initializedDictionaryInfo(applicationName, applicationDictionaryFile)
        val standardInfo =
            initializedDictionaryInfo(
                ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY,
                standardDictionaryFile,
            )
        val persistence = SdefPersistenceService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)

        try {
            persistence.addDictionaryInfo(applicationInfo)
            persistence.addDictionaryInfo(standardInfo)
            runBlocking {
                SdefIndexService.getInstance().ingest(applicationName, applicationDictionaryFile)
                SdefIndexService
                    .getInstance()
                    .ingest(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY, standardDictionaryFile)
            }

            val snapshot = SdefIndexService.getInstance().snapshot()
            assertTrue(
                "Regression setup must index the synthetic app command",
                snapshot.isApplicationCommand(applicationName, "play"),
            )
            assertTrue(
                "Regression setup must index the synthetic standard command",
                snapshot.isStdCommand("do shell script"),
            )

            registryService.standardReady.complete(Result.success(Unit))
            registryService.appsReady.complete(Result.success(Unit))
            assertTrue("standard dictionaries must be marked ready", registryService.isInitialized())
            assertTrue("application dictionaries must be marked ready", registryService.areAppDictionariesIndexed())

            assertParserLookupsDoNotMaterializeReadyDictionaries(applicationName, projectDictionaries)

            val application = ApplicationManager.getApplication()
            val future =
                application.executeOnPooledThread {
                    assertFalse("Regression must also exercise the non-EDT parser path", application.isDispatchThread)
                    assertParserLookupsDoNotMaterializeReadyDictionaries(applicationName, projectDictionaries)
                }
            future.get(MAX_NONBLOCKING_LOOKUP_MILLIS, TimeUnit.MILLISECONDS)
        } finally {
            applicationInfo.getApplicationFile()?.path?.let(persistence::removeDictionaryInfo)
            standardInfo.getApplicationFile()?.path?.let(persistence::removeDictionaryInfo)
        }
    }

    private fun initializedDictionaryInfo(
        applicationName: String,
        dictionaryFile: File,
    ): DictionaryInfo {
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dictionaryFile)
        val applicationFile = File(dictionaryFile.parentFile, "$applicationName.app")
        return DictionaryInfo(applicationName, dictionaryFile, applicationFile).also {
            it.setInitialized(true)
        }
    }

    private fun assertParserLookupsDoNotMaterializeReadyDictionaries(
        applicationName: String,
        projectDictionaries: AppleScriptProjectDictionaryService,
    ) {
        assertNull(projectDictionaries.getDictionary(applicationName))
        assertNull(projectDictionaries.getDictionary(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY))

        assertTrue(
            "Parser-facing app lookup must not create project dictionaries",
            DictionaryCommandRegistry.findApplicationCommands(project, applicationName, "play").isEmpty(),
        )
        assertTrue(
            "Parser-facing std lookup must not create project dictionaries",
            DictionaryCommandRegistry.findStdCommands(project, "do shell script").isEmpty(),
        )

        assertNull(
            "App parser lookup must leave project dictionary cache empty",
            projectDictionaries.getDictionary(applicationName),
        )
        assertNull(
            "Std parser lookup must leave project dictionary cache empty",
            projectDictionaries.getDictionary(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY),
        )
    }

    companion object {
        private const val MAX_NONBLOCKING_LOOKUP_MILLIS = 750L
    }
}
