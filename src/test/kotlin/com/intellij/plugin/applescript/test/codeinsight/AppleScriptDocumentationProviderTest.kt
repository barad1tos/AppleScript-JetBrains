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
import com.intellij.plugin.applescript.lang.ide.AppleScriptDocumentationProvider
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.test.service.SyntheticSuiteFixtures
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppleScriptDocumentationProviderTest : BasePlatformTestCase() {
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
            AppleScriptSystemDictionaryRegistryService(testScope, testDispatcher),
            testRootDisposable,
        )
        project.replaceService(
            AppleScriptProjectDictionaryService::class.java,
            AppleScriptProjectDictionaryService(project),
            testRootDisposable,
        )
    }

    fun testQuickDocumentationResolvesDictionaryTermAtIdentifierCaret() {
        val applicationName = "SyntheticDocumentationMusic_${System.nanoTime()}"
        val dictionaryFile = writeDocumentationMusicDictionaryXml()
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
            requireNotNull(projectDictionaries.createDictionary(applicationName)) {
                "Regression setup must create the synthetic project dictionary"
            }

            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "$applicationName"
                    set selectedTrack to a reference to (every track<caret> of library playlist 1)
                end tell
                """.trimIndent(),
            )

            val element =
                requireNotNull(myFixture.file.findElementAt(myFixture.caretOffset - 1)) {
                    "Regression setup must place the caret on the dictionary term"
                }
            val provider = AppleScriptDocumentationProvider()
            val quickNavigateInfo = provider.getQuickNavigateInfo(element, element)
            val documentation = provider.generateDoc(element, element)

            assertNotNull("Quick navigate info must resolve the dictionary term at the caret", quickNavigateInfo)
            requireNotNull(quickNavigateInfo)
            assertTrue(quickNavigateInfo, quickNavigateInfo.contains("\"track\""))
            assertNotNull("Quick documentation must resolve the dictionary term at the caret", documentation)
            requireNotNull(documentation)
            assertTrue(documentation, documentation.contains("Class <b>track</b>"))
            assertTrue(documentation, documentation.contains("A track in a playlist"))
        } finally {
            dictionaryInfo.getApplicationFile()?.path?.let(persistence::removeDictionaryInfo)
        }
    }

    fun testQuickDocumentationShowsLocalVariableContent() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on run(argv)
                set minDateAdded to missing value
                if minDateAdded<caret> is not missing value then
                    set trackRef to a reference to (every track of library playlist 1 whose date added > minDateAdded)
                end if
            end run
            """.trimIndent(),
        )

        val element =
            requireNotNull(myFixture.file.findElementAt(myFixture.caretOffset - 1)) {
                "Regression setup must place the caret on the local variable reference"
            }
        val resolvedElement = element.reference?.resolve()
        val provider = AppleScriptDocumentationProvider()

        val quickNavigateInfo = provider.getQuickNavigateInfo(element, element)
        val documentation = provider.generateDoc(element, element)

        assertEquals("variable \"minDateAdded\"", quickNavigateInfo)
        assertNotNull("Local variable documentation must not be blank", documentation)
        requireNotNull(documentation)
        assertTrue(documentation, documentation.contains("<b>Variable</b> minDateAdded"))

        if (resolvedElement != null) {
            assertEquals(
                "variable \"minDateAdded\"",
                provider.getQuickNavigateInfo(resolvedElement, element),
            )
            val resolvedDocumentation = provider.generateDoc(resolvedElement, element)
            assertNotNull("Resolved local variable documentation must not be blank", resolvedDocumentation)
            requireNotNull(resolvedDocumentation)
            assertTrue(resolvedDocumentation, resolvedDocumentation.contains("<b>Variable</b> minDateAdded"))
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

    private fun writeDocumentationMusicDictionaryXml(): File {
        val file = File.createTempFile("synthetic-documentation-music-", ".xml")
        file.deleteOnExit()
        file.writeText(SyntheticSuiteFixtures.musicAppPlayCommandXml())
        return file
    }
}
