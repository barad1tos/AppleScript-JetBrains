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
import com.intellij.plugin.applescript.psi.AppleScriptDirectParameterDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerCall
import com.intellij.plugin.applescript.psi.AppleScriptScriptObject
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
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

        assertEquals("Find Usages must return handler call references; ${handlerCallDebug()}", 2, usages.size)
    }

    fun testFindUsagesIgnoresHandlerArgumentWordsMatchingSelector() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on foo:f bar:r bazzz:z
                return f
            end foo:bar:baz:

            tell it to foo:foo bar:bar bazzz:bazzz
            """.trimIndent(),
        )
        val handler =
            findChildrenOfType(myFixture.file, AppleScriptHandler::class.java)
                .single { it.getSelector() == "foo:bar:bazzz:" }
        val handlerCalls = findChildrenOfType(myFixture.file, AppleScriptHandlerCall::class.java)

        assertEquals("Regression setup must parse one handler call", 1, handlerCalls.size)

        val usages =
            ReferencesSearch
                .search(handler, GlobalSearchScope.fileScope(myFixture.file))
                .findAll()

        assertEquals("Find Usages must ignore argument occurrences matching the selector", 1, usages.size)
    }

    fun testFindUsagesReturnsDictionaryCommandCallSites() {
        val applicationName = "SyntheticFindUsagesMusic_${System.nanoTime()}"
        val dictionaryFile = writeFindUsagesMusicDictionaryXml()
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

    fun testFindUsagesReturnsLocalVariableReferencesInsideTellFilter() {
        val usageInfos = myFixture.testFindUsages("codeinsight/fetch_tracks_min_date_usages.scpt")
        val usageLines =
            usageInfos
                .map { usageInfo ->
                    val element = requireNotNull(usageInfo.element) { "Usage info must have a PSI element" }
                    myFixture.editor.document.getLineNumber(element.textRange.startOffset) + 1
                }.sorted()

        assertEquals("Find Usages must include local variable references inside tell filters", 3, usageInfos.size)
        assertEquals(
            "Find Usages must include declaration, condition, and tell-filter references",
            listOf(2, 5, 6),
            usageLines,
        )
    }

    fun testFindUsagesReturnsHandlerParameterReferencesInsideTellFilter() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on run argv
                tell application "Music"
                    if argv is not "" then
                        set trackRef to a reference to (every track of library playlist 1 whose comment is argv)
                    end if
                end tell
            end run
            """.trimIndent(),
        )
        val parameter =
            findChildrenOfType(myFixture.file, AppleScriptDirectParameterDeclaration::class.java)
                .single { it.name == "argv" }

        val usages =
            ReferencesSearch
                .search(parameter, GlobalSearchScope.fileScope(myFixture.file))
                .findAll()
        val usageLines =
            usages
                .map { usage -> myFixture.editor.document.getLineNumber(usage.element.textRange.startOffset) + 1 }
                .sorted()

        assertEquals(
            "Find Usages must include handler parameter references inside tell filters",
            listOf(3, 4),
            usageLines,
        )
    }

    fun testFindUsagesDoesNotResolveUnloadedDictionaryPropertySelectorToSameNamedVariable() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            set name to "local"
            set output to name

            tell application "Music"
                set trackRef to a reference to (every track of library playlist 1 whose name is "target")
            end tell
            """.trimIndent(),
        )
        val variable =
            findChildrenOfType(myFixture.file, AppleScriptTargetVariable::class.java)
                .single { it.name == "name" }

        val usages =
            ReferencesSearch
                .search(variable, GlobalSearchScope.fileScope(myFixture.file))
                .findAll()
        val usageLines =
            usages
                .map { usage -> myFixture.editor.document.getLineNumber(usage.element.textRange.startOffset) + 1 }
                .sorted()

        assertEquals(
            "Find Usages must not include unloaded dictionary property selectors",
            listOf(1, 2),
            usageLines,
        )
    }

    fun testFindUsagesDoesNotResolveDictionaryPropertyTermToSameNamedVariable() {
        val applicationName = "SyntheticFindUsagesMusic_${System.nanoTime()}"
        val dictionaryFile = writeFindUsagesMusicDictionaryXml()
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
                set name to "local"
                set output to name

                tell application "$applicationName"
                    set trackRef to a reference to (every track of library playlist 1 whose name is "target")
                end tell
                """.trimIndent(),
            )
            val variable =
                findChildrenOfType(myFixture.file, AppleScriptTargetVariable::class.java)
                    .single { it.name == "name" }

            val usages =
                ReferencesSearch
                    .search(variable, GlobalSearchScope.fileScope(myFixture.file))
                    .findAll()
            val usageLines =
                usages
                    .map { usage -> myFixture.editor.document.getLineNumber(usage.element.textRange.startOffset) + 1 }
                    .sorted()

            assertEquals(
                "Find Usages must not include same-named dictionary property references",
                listOf(1, 2),
                usageLines,
            )
        } finally {
            dictionaryInfo.getApplicationFile()?.path?.let(persistence::removeDictionaryInfo)
        }
    }

    fun testFindUsagesDoesNotResolveDictionaryTermToSameNamedScriptObject() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            script bogusTerm
                property title : "local script object"
            end script

            tell application "Music"
                set trackRef to a reference to (every bogusTerm of library playlist 1)
            end tell
            """.trimIndent(),
        )
        val scriptObject =
            findChildrenOfType(myFixture.file, AppleScriptScriptObject::class.java)
                .single { it.name == "bogusTerm" }

        val usages =
            ReferencesSearch
                .search(scriptObject, GlobalSearchScope.fileScope(myFixture.file))
                .findAll()

        assertEquals(
            "Unresolved dictionary-shaped terms must not resolve to same-named script objects",
            0,
            usages.size,
        )
    }

    fun testFindUsagesDoesNotResolveDictionaryClassTermToSameNamedVariable() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            set bogusTerm to 1

            tell application "Music"
                set trackRef to a reference to (every bogusTerm of library playlist 1)
            end tell
            """.trimIndent(),
        )
        val variable =
            findChildrenOfType(myFixture.file, AppleScriptTargetVariable::class.java)
                .single { it.name == "bogusTerm" }

        val usages =
            ReferencesSearch
                .search(variable, GlobalSearchScope.fileScope(myFixture.file))
                .findAll()
        val usageLines =
            usages
                .map { usage -> myFixture.editor.document.getLineNumber(usage.element.textRange.startOffset) + 1 }
                .sorted()

        assertEquals(
            "Unresolved dictionary class terms must not resolve to same-named variables; " +
                usages.joinToString { usage -> "${usage.element.javaClass.simpleName}:${usage.element.text}" },
            listOf(1),
            usageLines,
        )
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

    private fun writeFindUsagesMusicDictionaryXml(): File {
        val file = File.createTempFile("synthetic-find-usages-music-", ".xml")
        file.deleteOnExit()
        file.writeText(SyntheticSuiteFixtures.musicAppPlayCommandXml())
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
