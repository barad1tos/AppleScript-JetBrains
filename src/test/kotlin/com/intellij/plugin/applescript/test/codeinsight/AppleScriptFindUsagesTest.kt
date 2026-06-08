package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerCall
import com.intellij.plugin.applescript.test.service.SyntheticSuiteFixtures
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil.findChildrenOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppleScriptFindUsagesTest : BasePlatformTestCase() {
    private lateinit var testScope: TestScope

    override fun getTestDataPath(): String = File(TEST_DATA_DIR).absolutePath

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
            AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher),
            testRootDisposable,
        )
        project.replaceService(
            AppleScriptProjectDictionaryService::class.java,
            AppleScriptProjectDictionaryService(project),
            testRootDisposable,
        )
    }

    fun testFindUsagesReturnsHandlerCallSites() {
        myFixture.configureByFile("parse/handlers/handler_interleved.scpt")
        val handler =
            findChildrenOfType(myFixture.file, AppleScriptHandler::class.java)
                .single { it.getSelector() == "areaOfRectangleWithWidth:height:" }

        val usages =
            ReferencesSearch
                .search(handler, GlobalSearchScope.fileScope(myFixture.file))
                .findAll()

        assertTrue("Find Usages must return handler call references; ${handlerCallDebug()}", usages.isNotEmpty())
    }

    fun testFindUsagesReturnsDictionaryCommandCallSites() {
        val applicationName = "SyntheticFindUsagesMusic_${System.nanoTime()}"
        val dictionaryFile =
            writeFindUsagesMusicDictionaryXml(SyntheticSuiteFixtures.musicAppPlayCommandXml())
        val dictionaryInfo = initializedDictionaryInfo(applicationName, dictionaryFile)
        val persistence = SdefPersistenceService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)

        try {
            persistence.addDictionaryInfo(dictionaryInfo)
            runBlocking {
                SdefIndexService.getInstance().ingest(applicationName, dictionaryFile)
            }
            registryService.standardReady.complete(Result.success(Unit))
            registryService.appsReady.complete(Result.success(Unit))

            val dictionary =
                requireNotNull(projectDictionaries.createDictionary(applicationName)) {
                    "Regression setup must create the synthetic project dictionary"
                }
            val playCommand = dictionary.findAllCommandsWithName("play").single()

            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "$applicationName"
                    play
                end tell
                """.trimIndent(),
            )

            val usages =
                ReferencesSearch
                    .search(playCommand, GlobalSearchScope.fileScope(myFixture.file))
                    .findAll()

            assertEquals("Find Usages must return dictionary command references", 1, usages.size)
        } finally {
            dictionaryInfo.getApplicationFile()?.path?.let(persistence::removeDictionaryInfo)
        }
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

    private fun writeFindUsagesMusicDictionaryXml(xml: String): File {
        val file = File.createTempFile("synthetic-find-usages-music-", ".xml")
        file.deleteOnExit()
        file.writeText(xml)
        return file
    }

    private fun handlerCallDebug(): String =
        findChildrenOfType(myFixture.file, AppleScriptHandlerCall::class.java)
            .joinToString(prefix = "handlerCalls=[", postfix = "]") { call ->
                val resolvedSelectors =
                    call.references.joinToString(prefix = "[", postfix = "]") { reference ->
                        (reference.resolve() as? AppleScriptHandler)?.getSelector() ?: "null"
                    }
                "${call.getHandlerSelector()} -> $resolvedSelectors"
            }

    companion object {
        private const val TEST_DATA_DIR = "src/test/resources/testData/"
    }
}
