package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.plugin.applescript.lang.dictionary.index.LookupResult
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

    fun testParserRegistryReturnsCommandsFromAlreadyCachedReadyProjectDictionaries() {
        val applicationName = "SyntheticCachedParserLookupApp_${System.nanoTime()}"
        val applicationDictionaryFile =
            writeDictionaryXmlToTempFile(
                "parser-cached-app",
                SyntheticSuiteFixtures.musicAppPlayCommandXml(),
            )
        val standardDictionaryFile =
            writeDictionaryXmlToTempFile(
                "parser-cached-standard-additions",
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
            registryService.standardReady.complete(Result.success(Unit))
            registryService.appsReady.complete(Result.success(Unit))

            val applicationDictionary = projectDictionaries.createDictionary(applicationName)
            val standardDictionary =
                projectDictionaries.createDictionary(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
            assertNotNull("Regression setup must cache the synthetic app dictionary", applicationDictionary)
            assertNotNull("Regression setup must cache the synthetic standard dictionary", standardDictionary)

            assertTrue(
                "Parser-facing app lookup must return commands from an already cached dictionary",
                DictionaryCommandRegistry.findApplicationCommands(project, applicationName, "play").isNotEmpty(),
            )
            assertTrue(
                "Parser-facing standard lookup must return commands from an already cached dictionary",
                DictionaryCommandRegistry.findStdCommands(project, "do shell script").isNotEmpty(),
            )
            assertSame(
                "App parser lookup must reuse the existing project dictionary cache entry",
                applicationDictionary,
                projectDictionaries.getDictionary(applicationName),
            )
            assertSame(
                "Std parser lookup must reuse the existing project dictionary cache entry",
                standardDictionary,
                projectDictionaries.getDictionary(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY),
            )
        } finally {
            applicationInfo.getApplicationFile()?.path?.let(persistence::removeDictionaryInfo)
            standardInfo.getApplicationFile()?.path?.let(persistence::removeDictionaryInfo)
        }
    }

    fun testCommandLookupResultSeparatesColdMissAndHit() {
        val applicationName = "SyntheticCommandLookupResultApp_${System.nanoTime()}"
        val applicationDictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "command-lookup-result-app",
                SyntheticSuiteFixtures.musicAppPlayCommandXml(),
            )
        val standardDictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "command-lookup-result-standard-additions",
                SyntheticSuiteFixtures.standardAdditionsMinimalXml(),
            )
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        val indexService = SdefIndexService.getInstance()

        runBlocking {
            indexService.ingest(applicationName, applicationDictionaryFile)
            indexService.ingest(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY, standardDictionaryFile)
        }

        assertEquals(
            "Standard command lookup must report cold readiness before standard dictionaries are ready",
            LookupResult.Stale,
            indexService.commandLookup.lookupStdCommandResult("do shell script"),
        )
        assertEquals(
            "Standard command prefix lookup must report cold readiness before standard dictionaries are ready",
            LookupResult.Stale,
            indexService.commandLookup.lookupStdCommandWithPrefixResult("do shell"),
        )
        assertEquals(
            "App command lookup must report cold readiness before app dictionaries are ready",
            LookupResult.Stale,
            indexService.commandLookup.lookupApplicationCommandResult(applicationName, "play"),
        )
        assertEquals(
            "App command prefix lookup must report cold readiness before app dictionaries are ready",
            LookupResult.Stale,
            indexService.commandLookup.lookupCommandWithPrefixResult(applicationName, "play"),
        )

        registryService.standardReady.complete(Result.success(Unit))
        assertEquals(
            "Standard command lookup must report hit once standard dictionaries are ready",
            LookupResult.Hit,
            indexService.commandLookup.lookupStdCommandResult("do shell script"),
        )
        assertEquals(
            "Standard command prefix lookup must report hit once standard dictionaries are ready",
            LookupResult.Hit,
            indexService.commandLookup.lookupStdCommandWithPrefixResult("do shell"),
        )
        assertEquals(
            "App command lookup must stay cold until app dictionaries are ready",
            LookupResult.Stale,
            indexService.commandLookup.lookupApplicationCommandResult(applicationName, "play"),
        )
        assertEquals(
            "App command prefix lookup must stay cold until app dictionaries are ready",
            LookupResult.Stale,
            indexService.commandLookup.lookupCommandWithPrefixResult(applicationName, "play"),
        )

        registryService.appsReady.complete(Result.success(Unit))
        assertEquals(
            "App command lookup must report hit once app dictionaries are ready",
            LookupResult.Hit,
            indexService.commandLookup.lookupApplicationCommandResult(applicationName, "play"),
        )
        assertEquals(
            "App command prefix lookup must report hit once app dictionaries are ready",
            LookupResult.Hit,
            indexService.commandLookup.lookupCommandWithPrefixResult(applicationName, "play"),
        )
        assertEquals(
            "Ready app command lookup must report miss for absent commands",
            LookupResult.Miss,
            indexService.commandLookup.lookupApplicationCommandResult(applicationName, "pause"),
        )
        assertEquals(
            "Ready app command prefix lookup must report miss for absent prefixes",
            LookupResult.Miss,
            indexService.commandLookup.lookupCommandWithPrefixResult(applicationName, "pa"),
        )
    }

    private fun writeDictionaryXmlToTempFile(
        name: String,
        xml: String,
    ): File {
        val file = File.createTempFile("synthetic-$name-", ".xml")
        file.deleteOnExit()
        file.writeText(xml)
        return file
    }

    private fun initializedDictionaryInfo(
        applicationName: String,
        dictionaryFile: File,
    ): DictionaryInfo {
        val canonicalDictionaryFile = dictionaryFile.canonicalFile
        VfsRootAccess.allowRootAccess(testRootDisposable, canonicalDictionaryFile.parentFile.path)
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(canonicalDictionaryFile)
        val applicationFile = File(canonicalDictionaryFile.parentFile, "$applicationName.app")
        return DictionaryInfo(applicationName, canonicalDictionaryFile, applicationFile).also {
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
