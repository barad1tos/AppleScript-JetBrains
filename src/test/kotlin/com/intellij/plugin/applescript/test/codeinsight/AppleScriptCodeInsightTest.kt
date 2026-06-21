package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.generation.actions.CommentByLineCommentAction
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.AppleScriptIcons
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.files.serializeDictionaryPathForApplication
import com.intellij.plugin.applescript.lang.dictionary.icons.DictionaryIconLoader
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterColors
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.plugin.applescript.psi.sdef.impl.ApplicationDictionaryImpl
import com.intellij.plugin.applescript.test.service.SyntheticSuiteFixtures
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Icon

/**
 * Code-insight smoke suite (completion, annotator, file-type, formatter, rename,
 * find-usages, commenter, reference) over BasePlatformTestCase fixtures.
 *
 * Heavy: boots a full fixture and the completion path scans the bundled
 * StandardAdditions / CocoaStandard SDEF dictionaries. Runs only under
 * -PincludeHeavyTests=true via the codeinsight.* matcher (it ran in NO CI
 * filter before Phase 6 — D-03).
 */
class AppleScriptCodeInsightTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(MY_TEST_DATA_DIR).absolutePath

    fun testCompletion() {
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        PlatformTestUtil.waitWithEventsDispatching(
            "Standard dictionaries were not initialized",
            { registryService.isInitialized() },
            10,
        )
        myFixture.configureByFile("complete/complete_std_lib_test.scpt")
        myFixture.complete(CompletionType.BASIC, 1)
        val strings = myFixture.lookupElementStrings
        val elements = myFixture.lookupElements
        assertNotNull(strings)
        assertNotNull(elements)
        requireNotNull(strings)
        requireNotNull(elements)
        assertEquals(strings.size, elements.size)
        // D-03 redesign: the previous assertEquals(394/257/45) golden counts were brittle —
        // any bundled-SDEF addition or dictionary-component churn flipped them RED without a
        // real regression. Replace with (1) a sanity lower bound that catches a collapsed
        // completion list and (2) content anchors on stable Standard Additions commands.
        assertTrue("completion shrank unexpectedly: ${strings.size}", strings.size >= 350)
        for (term in STABLE_TERMS) {
            assertTrue("completion missing stable term '$term'", strings.contains(term))
        }
    }

    fun testAnnotator() {
        myFixture.configureByFile("annotator/not_found_dic.scpt")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testUnknownApplicationHighlightIsWeakWarning() {
        val script = """tell application "NoSuchApp_xyz" to activate"""
        myFixture.configureByText(AppleScriptFileType, script)

        val highlights = myFixture.doHighlighting()
        val range = textRangeFor(myFixture.editor.document, "NoSuchApp_xyz")
        val severities =
            highlights
                .filter { highlight -> range.intersects(highlight.startOffset, highlight.endOffset) }
                .mapTo(mutableSetOf()) { highlight -> highlight.severity }

        assertTrue(
            "unknown app must be a weak warning; severities=$severities",
            severities.contains(HighlightSeverity.WEAK_WARNING),
        )
        assertFalse("unknown app must not enter Problems as WARNING", severities.contains(HighlightSeverity.WARNING))
        assertFalse("unknown app must not be an ERROR", severities.contains(HighlightSeverity.ERROR))
    }

    fun testApplicationReferenceHighlightingDoesNotCreateProjectDictionary() {
        val applicationName = "SyntheticAnnotatorApp_${System.nanoTime()}"
        val dictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "annotator-read-only",
                SyntheticSuiteFixtures.musicAppPlayCommandXml(),
            )
        val applicationFile = File(dictionaryFile.parentFile, "$applicationName.app")
        val dictionaryInfo = DictionaryInfo(applicationName, dictionaryFile, applicationFile)
        val persistence = SdefPersistenceService.getInstance()
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        try {
            persistence.addDictionaryInfo(dictionaryInfo)
            PlatformTestUtil.waitWithEventsDispatching(
                "Application dictionaries were not indexed",
                { registryService.areAppDictionariesIndexed() },
                10,
            )
            assertTrue(
                "Synthetic dictionary must be known through discovery",
                ApplicationDiscoveryService.getInstance().isKnownApplication(applicationName),
            )
            assertFalse(
                "Synthetic dictionary must stay uninitialized so the test covers the discovered-app branch",
                persistence
                    .dictionaryInfoSnapshot
                    .any { it.getApplicationName() == applicationName && it.initialized },
            )
            assertNull(projectDictionaries.getDictionary(applicationName))

            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "$applicationName"
                end tell
                """.trimIndent(),
            )
            val highlights = myFixture.doHighlighting()
            val applicationNameRange = textRangeFor(myFixture.editor.document, applicationName)
            val applicationReferenceDescriptions =
                highlights
                    .filter { highlight ->
                        applicationNameRange.intersects(highlight.startOffset, highlight.endOffset)
                    }.mapNotNull { highlight -> highlight.description }

            assertFalse(
                "Discovered app must not be highlighted as unknown; descriptions=$applicationReferenceDescriptions",
                applicationReferenceDescriptions.any { description -> description.contains("Unknown app") },
            )

            assertNull(
                "Highlighting must not create a project dictionary; explicit load paths own that side effect",
                projectDictionaries.getDictionary(applicationName),
            )
        } finally {
            persistence.removeDictionaryInfo(applicationFile.path)
        }
    }

    fun testApplicationReferenceLineMarkerUsesGeneratedDictionaryCache() {
        val applicationName = "SyntheticMarkerApp_${System.nanoTime()}"
        val generatedDictionaryFile = File(serializeDictionaryPathForApplication(applicationName))
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)
        generatedDictionaryFile.parentFile.mkdirs()
        generatedDictionaryFile.writeText(SyntheticSuiteFixtures.musicAppPlayCommandXml())

        try {
            assertNull(projectDictionaries.getDictionary(applicationName))

            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "$applicationName"
                end tell
                """.trimIndent(),
            )
            val markers = myFixture.findAllGutters()

            assertEquals("Generated dictionary cache should create one gutter marker", 1, markers.size)
            assertNull(
                "Line markers must not create a project dictionary; explicit load paths own that side effect",
                projectDictionaries.getDictionary(applicationName),
            )
        } finally {
            projectDictionaries.clearCachedDictionariesForTests()
            generatedDictionaryFile.delete()
        }
    }

    fun testApplicationReferenceLineMarkerUsesGeneratedCacheApplicationBundleIcon() {
        val applicationName = "Things3"
        val applicationBundle = File("/Applications/Things3.app")
        if (!applicationBundle.isDirectory) return

        val generatedDictionaryFile = File(serializeDictionaryPathForApplication(applicationName))
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)
        generatedDictionaryFile.parentFile.mkdirs()
        generatedDictionaryFile.writeText(SyntheticSuiteFixtures.musicAppPlayCommandXml())
        val applicationIcon = DictionaryIconLoader.loadFromBundle(applicationBundle, applicationName)
        assertNotNull(applicationIcon)
        requireNotNull(applicationIcon)

        try {
            assertNull(projectDictionaries.getDictionary(applicationName))

            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "$applicationName"
                end tell
                """.trimIndent(),
            )
            val markers = myFixture.findAllGutters()

            assertEquals("Generated cache application reference should create one gutter marker", 1, markers.size)
            assertTrue(
                "Generated cache gutter marker should use the application bundle icon",
                markers.single().icon.renderedPixelsEqual(applicationIcon),
            )
        } finally {
            projectDictionaries.clearCachedDictionariesForTests()
            generatedDictionaryFile.delete()
        }
    }

    fun testApplicationReferenceLineMarkerRefreshesCachedDictionaryWithoutApplicationBundleIcon() {
        val applicationName = "Things3"
        val applicationBundle = File("/Applications/Things3.app")
        if (!applicationBundle.isDirectory) return

        val generatedDictionaryFile = File(serializeDictionaryPathForApplication(applicationName))
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)
        generatedDictionaryFile.parentFile.mkdirs()
        generatedDictionaryFile.writeText(SyntheticSuiteFixtures.musicAppPlayCommandXml())
        val staleDictionaryFile =
            File
                .createTempFile("things3-stale", ".xml")
                .also { file -> file.writeText(SyntheticSuiteFixtures.musicAppPlayCommandXml()) }
        val dictionaryXmlFile =
            LocalFileSystem
                .getInstance()
                .refreshAndFindFileByIoFile(staleDictionaryFile)
                ?.let { virtualFile -> PsiManager.getInstance(project).findFile(virtualFile) as? XmlFile }
        assertNotNull(dictionaryXmlFile)
        requireNotNull(dictionaryXmlFile)
        val applicationIcon = DictionaryIconLoader.loadFromBundle(applicationBundle, applicationName)
        assertNotNull(applicationIcon)
        requireNotNull(applicationIcon)

        try {
            projectDictionaries.cacheDictionaryForTest(
                applicationName,
                ApplicationDictionaryImpl(project, dictionaryXmlFile, applicationName, null),
            )

            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "$applicationName"
                end tell
                """.trimIndent(),
            )
            val markers = myFixture.findAllGutters()

            assertEquals("Application reference should create one gutter marker", 1, markers.size)
            assertTrue(
                "Stale project dictionary cache should not force a generic gutter marker icon",
                markers.single().icon.renderedPixelsEqual(applicationIcon),
            )
        } finally {
            projectDictionaries.clearCachedDictionariesForTests()
            generatedDictionaryFile.delete()
            staleDictionaryFile.delete()
        }
    }

    fun testApplicationReferenceLineMarkerUsesApplicationBundleIcon() {
        val applicationBundle = File("/System/Applications/Music.app")
        if (!applicationBundle.isDirectory) return

        val applicationName = "SyntheticMarkerIconApp_${System.nanoTime()}"
        val dictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "marker-icon",
                SyntheticSuiteFixtures.musicAppPlayCommandXml(),
            )
        val dictionaryInfo =
            DictionaryInfo(applicationName, dictionaryFile, applicationBundle)
                .also { info -> info.setInitialized(true) }
        val persistence = SdefPersistenceService.getInstance()
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)

        try {
            persistence.addDictionaryInfo(dictionaryInfo)
            val applicationIcon = DictionaryIconLoader.loadFromBundle(applicationBundle, applicationName)
            assertNotNull(applicationIcon)
            requireNotNull(applicationIcon)
            assertFalse(
                "Test setup must load a non-generic application icon before checking the gutter marker",
                applicationIcon.renderedPixelsEqual(AppleScriptIcons.OPEN_DICTIONARY),
            )

            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "$applicationName"
                end tell
                """.trimIndent(),
            )
            val markers = myFixture.findAllGutters()

            assertEquals("Application reference should create one gutter marker", 1, markers.size)
            assertTrue(
                "Application dictionary gutter marker should use the application bundle icon",
                markers.single().icon.renderedPixelsEqual(applicationIcon),
            )
        } finally {
            projectDictionaries.clearCachedDictionariesForTests()
            persistence.removeDictionaryInfo(applicationBundle.path)
            dictionaryFile.delete()
        }
    }

    fun testApplicationReferenceWarningReasonWinsForKnownApplication() {
        val applicationName = "SyntheticKnownWarningApp_${System.nanoTime()}"
        val dictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "annotator-known-warning",
                SyntheticSuiteFixtures.musicAppPlayCommandXml(),
            )
        val applicationFile = File(dictionaryFile.parentFile, "$applicationName.app")
        val dictionaryInfo = DictionaryInfo(applicationName, dictionaryFile, applicationFile)
        val persistence = SdefPersistenceService.getInstance()
        val discovery = ApplicationDiscoveryService.getInstance()

        try {
            persistence.addDictionaryInfo(dictionaryInfo)
            discovery.addToNotFoundList(applicationName)

            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "$applicationName"
                end tell
                """.trimIndent(),
            )
            val highlights = myFixture.doHighlighting()
            val applicationNameRange = textRangeFor(myFixture.editor.document, applicationName)
            val descriptions =
                highlights
                    .filter { highlight ->
                        applicationNameRange.intersects(highlight.startOffset, highlight.endOffset)
                    }.mapNotNull { highlight -> highlight.description }

            assertTrue(
                "Known app warning reason must not be masked; descriptions=$descriptions",
                descriptions.contains("Application \"$applicationName\" not found"),
            )
        } finally {
            discovery.removeFromNotFoundList(applicationName)
            persistence.removeDictionaryInfo(applicationFile.path)
        }
    }

    fun testUnknownSystemEventsProcessReferenceIsWeakWarning() {
        val unknownProcessName = "SyntheticMissingProcess_${System.nanoTime()}"
        val discovery = ApplicationDiscoveryService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        discovery.addDiscoveredApplicationName("System Events")
        PlatformTestUtil.waitWithEventsDispatching(
            "Application dictionaries were not indexed",
            { registryService.areAppDictionariesIndexed() },
            10,
        )

        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "system events"
                set value of slider 1 of group 1 of tab group 1 of window 1 of process "$unknownProcessName" to 1
            end tell
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        val processNameRange = textRangeFor(myFixture.editor.document, unknownProcessName)
        val severities =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapTo(mutableSetOf()) { highlight -> highlight.severity }
        val descriptions =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapNotNull { highlight -> highlight.description }

        assertTrue(
            "unknown System Events process must be a weak warning; severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.WEAK_WARNING),
        )
        assertFalse("unknown process must not be an ERROR", severities.contains(HighlightSeverity.ERROR))
        assertEquals(
            "warning must appear exactly once; descriptions=$descriptions",
            1,
            descriptions.count { description ->
                description == "Process \"$unknownProcessName\" is not known on this macOS installation"
            },
        )
        assertTrue(
            "warning must explain the local-discovery basis; descriptions=$descriptions",
            descriptions.contains("Process \"$unknownProcessName\" is not known on this macOS installation"),
        )
    }

    fun testUnknownSystemEventsProcessReferenceInsideSimpleTellIsWeakWarning() {
        val unknownProcessName = "SyntheticSimpleTellMissingProcess_${System.nanoTime()}"
        val discovery = ApplicationDiscoveryService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        discovery.addDiscoveredApplicationName("System Events")
        PlatformTestUtil.waitWithEventsDispatching(
            "Application dictionaries were not indexed",
            { registryService.areAppDictionariesIndexed() },
            10,
        )

        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "System Events" to set frontmost of process "$unknownProcessName" to true
            """.trimIndent(),
        )

        val highlights = myFixture.doHighlighting()
        val processNameRange = textRangeFor(myFixture.editor.document, unknownProcessName)
        val severities =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapTo(mutableSetOf()) { highlight -> highlight.severity }
        val descriptions =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapNotNull { highlight -> highlight.description }

        assertTrue(
            "unknown System Events process in simple tell must be a weak warning; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.WEAK_WARNING),
        )
        assertFalse(
            "unknown process in simple tell must not be an ERROR; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.ERROR),
        )
        assertEquals(
            "warning must appear exactly once; descriptions=$descriptions",
            1,
            descriptions.count { description ->
                description == "Process \"$unknownProcessName\" is not known on this macOS installation"
            },
        )
        assertTrue(
            "warning must explain the local-discovery basis; descriptions=$descriptions",
            descriptions.contains("Process \"$unknownProcessName\" is not known on this macOS installation"),
        )
    }

    fun testUnknownSystemEventsProcessReferenceInsideNestedObjectTellIsWeakWarning() {
        val unknownProcessName = "SyntheticNestedObjectMissingProcess_${System.nanoTime()}"
        val discovery = ApplicationDiscoveryService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        discovery.addDiscoveredApplicationName("System Events")
        discovery.addDiscoveredApplicationName("Finder")
        PlatformTestUtil.waitWithEventsDispatching(
            "Application dictionaries were not indexed",
            { registryService.areAppDictionariesIndexed() },
            10,
        )

        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "System Events"
                tell process "Finder"
                    set value of slider 1 of group 1 of tab group 1 of window 1 of process "$unknownProcessName" to 1
                end tell
            end tell
            """.trimIndent(),
        )

        val highlights = myFixture.doHighlighting()
        val processNameRange = textRangeFor(myFixture.editor.document, unknownProcessName)
        val severities =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapTo(mutableSetOf()) { highlight -> highlight.severity }
        val descriptions =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapNotNull { highlight -> highlight.description }

        assertTrue(
            "unknown process inside nested object tell must still see outer System Events; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.WEAK_WARNING),
        )
        assertFalse(
            "unknown process inside nested object tell must not be an ERROR; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.ERROR),
        )
        assertEquals(
            "warning must appear exactly once; descriptions=$descriptions",
            1,
            descriptions.count { description ->
                description == "Process \"$unknownProcessName\" is not known on this macOS installation"
            },
        )
        assertTrue(
            "warning must explain the local-discovery basis; descriptions=$descriptions",
            descriptions.contains("Process \"$unknownProcessName\" is not known on this macOS installation"),
        )
    }

    fun testUnknownSystemEventsProcessReferenceInsideNestedApplicationTellIsNotHighlighted() {
        val unknownProcessName = "SyntheticNestedApplicationMissingProcess_${System.nanoTime()}"
        val discovery = ApplicationDiscoveryService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        discovery.addDiscoveredApplicationName("System Events")
        discovery.addDiscoveredApplicationName("Finder")
        PlatformTestUtil.waitWithEventsDispatching(
            "Application dictionaries were not indexed",
            { registryService.areAppDictionariesIndexed() },
            10,
        )

        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "System Events"
                tell application "Finder"
                    set value of slider 1 of group 1 of tab group 1 of window 1 of process "$unknownProcessName" to 1
                end tell
            end tell
            """.trimIndent(),
        )

        val highlights = myFixture.doHighlighting()
        val processNameRange = textRangeFor(myFixture.editor.document, unknownProcessName)
        val severities =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapTo(mutableSetOf()) { highlight -> highlight.severity }
        val descriptions =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapNotNull { highlight -> highlight.description }

        assertFalse(
            "nested tell application \"Finder\" must override outer System Events; " +
                "descriptions=$descriptions",
            descriptions.any { description -> description.contains("not known on this macOS installation") },
        )
        assertFalse(
            "nested tell application \"Finder\" must not get a weak warning; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.WEAK_WARNING),
        )
        assertFalse(
            "nested tell application \"Finder\" must not get an error; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.ERROR),
        )
    }

    fun testKnownSystemEventsProcessReferenceIsNotHighlighted() {
        val knownProcessName = "SyntheticKnownProcess_${System.nanoTime()}"
        val discovery = ApplicationDiscoveryService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        discovery.addDiscoveredApplicationName("System Events")
        discovery.addDiscoveredApplicationName(knownProcessName)
        PlatformTestUtil.waitWithEventsDispatching(
            "Application dictionaries were not indexed",
            { registryService.areAppDictionariesIndexed() },
            10,
        )

        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "System Events"
                set value of slider 1 of group 1 of tab group 1 of window 1 of process "$knownProcessName" to 1
            end tell
            """.trimIndent(),
        )

        val highlights = myFixture.doHighlighting()
        val processNameRange = textRangeFor(myFixture.editor.document, knownProcessName)
        val severities =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapTo(mutableSetOf()) { highlight -> highlight.severity }
        val descriptions =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapNotNull { highlight -> highlight.description }

        assertFalse(
            "known System Events process must not be highlighted as unknown; descriptions=$descriptions",
            descriptions.any { description -> description.contains("not known on this macOS installation") },
        )
        assertFalse(
            "known System Events process must not get a weak warning; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.WEAK_WARNING),
        )
        assertFalse(
            "known System Events process must not get an error; severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.ERROR),
        )
    }

    fun testSystemEventsProcessInspectionIgnoresDynamicNames() {
        val dynamicProcessName = "SyntheticDynamicMissingProcess_${System.nanoTime()}"
        val discovery = ApplicationDiscoveryService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        discovery.addDiscoveredApplicationName("System Events")
        PlatformTestUtil.waitWithEventsDispatching(
            "Application dictionaries were not indexed",
            { registryService.areAppDictionariesIndexed() },
            10,
        )

        myFixture.configureByText(
            AppleScriptFileType,
            """
            set processReferenceName to "$dynamicProcessName"
            tell application "System Events"
                set frontmost of process processReferenceName to true
            end tell
            """.trimIndent(),
        )

        val highlights = myFixture.doHighlighting()
        val processReference = "process processReferenceName"
        val processReferenceStart =
            myFixture.editor.document.charsSequence
                .indexOf(processReference)
        assertTrue("expected to find '$processReference'", processReferenceStart >= 0)
        val dynamicNameRange =
            TextRange(
                processReferenceStart + "process ".length,
                processReferenceStart + processReference.length,
            )
        val severities =
            highlights
                .filter { highlight -> dynamicNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapTo(mutableSetOf()) { highlight -> highlight.severity }
        val descriptions =
            highlights
                .filter { highlight -> dynamicNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapNotNull { highlight -> highlight.description }

        assertFalse(
            "dynamic process names must not be validated as literal process references; descriptions=$descriptions",
            descriptions.any { description -> description.contains("not known on this macOS installation") },
        )
        assertFalse(
            "dynamic process names must not get a weak warning; severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.WEAK_WARNING),
        )
        assertFalse(
            "dynamic process names must not get an error; severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.ERROR),
        )
    }

    fun testProcessReferenceOutsideSystemEventsIsNotHighlighted() {
        val unknownProcessName = "SyntheticOutsideSystemEvents_${System.nanoTime()}"
        val discovery = ApplicationDiscoveryService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        discovery.addDiscoveredApplicationName("Finder")
        PlatformTestUtil.waitWithEventsDispatching(
            "Application dictionaries were not indexed",
            { registryService.areAppDictionariesIndexed() },
            10,
        )

        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                set targetProcess to process "$unknownProcessName"
            end tell
            """.trimIndent(),
        )

        val highlights = myFixture.doHighlighting()
        val processNameRange = textRangeFor(myFixture.editor.document, unknownProcessName)
        val severities =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapTo(mutableSetOf()) { highlight -> highlight.severity }
        val descriptions =
            highlights
                .filter { highlight -> processNameRange.intersects(highlight.startOffset, highlight.endOffset) }
                .mapNotNull { highlight -> highlight.description }

        assertFalse(
            "process-like references outside System Events must not be validated; descriptions=$descriptions",
            descriptions.any { description -> description.contains("not known on this macOS installation") },
        )
        assertFalse(
            "process-like references outside System Events must not get a weak warning; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.WEAK_WARNING),
        )
        assertFalse(
            "process-like references outside System Events must not get an error; " +
                "severities=$severities descriptions=$descriptions",
            severities.contains(HighlightSeverity.ERROR),
        )
    }

    fun testDatePropertyReferencesUsePropertyHighlighting() {
        val script =
            """
            on formatDate(theDate)
                if class of theDate is date then
                    set y to year of theDate
                    set mInt to month of theDate
                    set dInt to day of theDate
                    set hhInt to hours of theDate
                    set mmInt to minutes of theDate
                    set ssInt to seconds of theDate
                end if
            end formatDate
            """.trimIndent()

        myFixture.configureByText(AppleScriptFileType, script)
        val highlights = myFixture.doHighlighting()

        val document = myFixture.editor.document
        for (term in DATE_PROPERTY_TERMS) {
            val keys = highlightingKeysFor(highlights, textRangeFor(document, term))
            assertTrue(
                "$term must use dictionary property highlighting; keys=$keys",
                keys.contains(AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR),
            )
            assertFalse(
                "$term must not use dictionary constant highlighting; keys=$keys",
                keys.contains(AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR),
            )
        }
    }

    fun testWriteStartingAtEofHighlightsSelectorAndConstant() {
        val script =
            """
            on appendLine(logFilePosix, lineText)
                set logAlias to POSIX file logFilePosix
                set fh to open for access logAlias with write permission
                write (lineText & linefeed) to fh starting at eof
                close access fh
            end appendLine
            """.trimIndent()

        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        PlatformTestUtil.waitWithEventsDispatching(
            "Standard dictionaries were not initialized",
            { registryService.isInitialized() },
            10,
        )
        myFixture.configureByText(AppleScriptFileType, script)
        val highlights = myFixture.doHighlighting()
        val document = myFixture.editor.document

        val selectorKeys = highlightingKeysFor(highlights, textRangeFor(document, "starting at"))
        assertTrue(
            "starting at must use command-parameter selector highlighting; keys=$selectorKeys",
            selectorKeys.contains(AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR),
        )

        val constantKeys = highlightingKeysFor(highlights, textRangeFor(document, "eof"))
        assertTrue(
            "eof must use dictionary constant highlighting; keys=$constantKeys",
            constantKeys.contains(AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR),
        )
    }

    fun testClipboardFileCommandsUseSemanticHighlighting() {
        val script =
            """
            set d to the clipboard as «class utf8»
            set fn to choose file name
            set fid to open for access fn with write permission
            write d to fid
            close access fid
            """.trimIndent()

        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        PlatformTestUtil.waitWithEventsDispatching(
            "Standard dictionaries were not initialized",
            { registryService.isInitialized() },
            10,
        )
        myFixture.configureByText(AppleScriptFileType, script)
        val highlights = myFixture.doHighlighting()
        val document = myFixture.editor.document

        assertHighlightingKeysContain(
            highlights,
            document,
            "the clipboard",
            AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "choose file name",
            AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "open for access",
            AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "write d to fid",
            AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "close access",
            AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "with write",
            AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "«class utf8»",
            AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "set d",
            AppleScriptSyntaxHighlighterColors.VARIABLE,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "set fn",
            AppleScriptSyntaxHighlighterColors.VARIABLE,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "set fid",
            AppleScriptSyntaxHighlighterColors.VARIABLE,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "write d",
            AppleScriptSyntaxHighlighterColors.VARIABLE,
        )
        assertHighlightingKeysContain(
            highlights,
            document,
            "close access fid",
            AppleScriptSyntaxHighlighterColors.VARIABLE,
        )
    }

    fun testApplicationDictionaryTermsUseSemanticHighlighting() {
        val applicationName = "SyntheticTaskListApp_${System.nanoTime()}"
        val dictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "task-list-codeinsight",
                SyntheticSuiteFixtures.taskListAppXml(),
            )
        val applicationFile = File(dictionaryFile.parentFile, "$applicationName.app")
        val dictionaryInfo = DictionaryInfo(applicationName, dictionaryFile, applicationFile)
        val persistence = SdefPersistenceService.getInstance()
        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()

        try {
            persistence.addDictionaryInfo(dictionaryInfo)
            PlatformTestUtil.waitWithEventsDispatching(
                "Application dictionaries were not indexed",
                { registryService.areAppDictionariesIndexed() },
                10,
            )
            val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)
            val dictionary = projectDictionaries.createDictionary(applicationName)
            val makeCommand = dictionary?.findAllCommandsWithName("make")?.singleOrNull()
            assertNotNull("synthetic dictionary must expose make command", makeCommand)
            assertEquals("type", makeCommand?.getParameterByName("new")?.typeSpecifier)
            val indexedClassNames =
                SdefIndexService
                    .getInstance()
                    .snapshot()
                    .applicationNameToClassNameSet[applicationName]
                    .orEmpty()
            assertTrue("synthetic dictionary must index to do class", indexedClassNames.contains("to do"))

            val script =
                """
                tell application "$applicationName"
                    activate
                    show list "Inbox"
                    repeat with currentLine in reverse of fileContents
                        set newToDo to make new to do ¬
                            with properties {name:currentLine} ¬
                            at beginning of list "Inbox"
                    end repeat
                end tell
                """.trimIndent()

            myFixture.configureByText(AppleScriptFileType, script)
            val highlights = myFixture.doHighlighting()
            val document = myFixture.editor.document

            assertHighlightingKeysContain(
                highlights,
                document,
                "show",
                AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
            )
            assertHighlightingKeysContain(
                highlights,
                document,
                "make",
                AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
            )
            assertHighlightingKeysContain(
                highlights,
                document,
                "to do",
                AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR,
            )
            assertHighlightingKeysContain(
                highlights,
                document,
                "with properties",
                AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
            )
            assertHighlightingKeysContain(
                highlights,
                document,
                "at beginning",
                AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
            )
        } finally {
            persistence.removeDictionaryInfo(applicationFile.path)
        }
    }

    fun testMyHandlerCallUsesFunctionHighlighting() {
        val script =
            """
            on run
                set rawStatus to "matched"
                set statusText to my normalize_cloud_status(rawStatus)
            end run

            on normalize_cloud_status(statusValue)
                return statusValue
            end normalize_cloud_status
            """.trimIndent()

        myFixture.configureByText(AppleScriptFileType, script)
        val highlights = myFixture.doHighlighting()

        val document = myFixture.editor.document
        val keys = highlightingKeyNamesFor(highlights, textRangeFor(document, "normalize_cloud_status"))
        assertTrue(
            "handler call must use function-call highlighting; keys=$keys",
            keys.contains(HANDLER_CALL_KEY),
        )
    }

    fun testFileType() {
        val file = myFixture.configureByFile("annotator/not_found_dic.scpt")
        assertTrue(file.fileType === AppleScriptFileType)
    }

    fun testFormatter() {
        myFixture.configureByFiles("format/test_block_indent.scpt")
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        myFixture.checkResultByFile("format/test_block_indent_result.scpt")
    }

    fun testRename() {
        myFixture.configureByFiles("codeinsight/set_var.scpt")
        myFixture.renameElementAtCaret("myVarNewName")
        myFixture.checkResultByFile("codeinsight/set_var_result.scpt")
    }

    fun testRenameLocalVariableInsideTellFilter() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on run argv
                set minDateAdded<caret> to missing value

                tell application "Music"
                    if minDateAdded is not missing value then
                        set trackRef to a reference to (every track of library playlist 1 whose date added > minDateAdded)
                    end if
                end tell
            end run
            """.trimIndent(),
        )
        myFixture.renameElementAtCaret("minimumDateAdded")
        val expected =
            """
            on run argv
                set minimumDateAdded to missing value

                tell application "Music"
                    if minimumDateAdded is not missing value then
                        set trackRef to a reference to (every track of library playlist 1 whose date added > minimumDateAdded)
                    end if
                end tell
            end run
            """.trimIndent()
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun testRenameLocalVariableDoesNotRewriteUnresolvedDictionaryPropertySelector() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            set name<caret> to "local"

            tell application "Music"
                set trackRef to a reference to (every track of library playlist 1 whose name is "target")
            end tell
            """.trimIndent(),
        )
        myFixture.renameElementAtCaret("localName")
        val expected =
            """
            set localName to "local"

            tell application "Music"
                set trackRef to a reference to (every track of library playlist 1 whose name is "target")
            end tell
            """.trimIndent()
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun testUsages() {
        val usageInfos = myFixture.testFindUsages("codeinsight/set_var.scpt")
        assertEquals(2, usageInfos.size)
    }

    fun testCommenter() {
        myFixture.configureByText(AppleScriptFileType, "<caret>set myVar to 123")
        val commentAction = CommentByLineCommentAction()
        commentAction.actionPerformedImpl(project, myFixture.editor)
        myFixture.checkResult("--set myVar to 123")
        commentAction.actionPerformedImpl(project, myFixture.editor)
        myFixture.checkResult("set myVar to 123")
    }

    fun testReference() {
        myFixture.configureByFile("codeinsight/set_var_ref.scpt")
        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val parent = element?.parent
        val superParent = parent?.parent
        assertNotNull(superParent)
        requireNotNull(superParent)
        val resolveResult = superParent.references[0].resolve() as AppleScriptTargetVariable
        assertNotNull(resolveResult)
        assertEquals("myVar", resolveResult.text)
    }

    private fun highlightingKeysFor(
        highlights: List<HighlightInfo>,
        textRange: TextRange,
    ): Set<TextAttributesKey> =
        highlights
            .filter { highlight -> textRange.intersects(highlight.startOffset, highlight.endOffset) }
            .mapNotNullTo(mutableSetOf()) { highlight -> highlight.forcedTextAttributesKey }

    private fun highlightingKeyNamesFor(
        highlights: List<HighlightInfo>,
        textRange: TextRange,
    ): Set<String> =
        highlightingKeysFor(highlights, textRange)
            .mapTo(mutableSetOf()) { key -> key.externalName }

    private fun assertHighlightingKeysContain(
        highlights: List<HighlightInfo>,
        document: Document,
        text: String,
        expectedKey: TextAttributesKey,
    ) {
        val keys = highlightingKeysFor(highlights, textRangeFor(document, text))
        assertTrue(
            "$text must use ${expectedKey.externalName}; keys=$keys",
            keys.contains(expectedKey),
        )
    }

    private fun textRangeFor(
        document: Document,
        text: String,
    ): TextRange {
        val startOffset = document.charsSequence.indexOf(text)
        assertTrue("expected to find '$text'", startOffset >= 0)
        return TextRange(startOffset, startOffset + text.length)
    }

    private fun Icon.renderedPixelsEqual(other: Icon): Boolean {
        val left = renderIcon()
        val right = other.renderIcon()
        if (left.width != right.width || left.height != right.height) return false

        for (x in 0 until left.width) {
            for (y in 0 until left.height) {
                if (left.getRGB(x, y) != right.getRGB(x, y)) return false
            }
        }
        return true
    }

    private fun Icon.renderIcon(): BufferedImage {
        val image =
            BufferedImage(
                iconWidth.coerceAtLeast(1),
                iconHeight.coerceAtLeast(1),
                BufferedImage.TYPE_INT_ARGB,
            )
        val graphics = image.createGraphics()
        paintIcon(null, graphics, 0, 0)
        graphics.dispose()
        return image
    }

    @Suppress("UNCHECKED_CAST")
    private fun AppleScriptProjectDictionaryService.cacheDictionaryForTest(
        applicationName: String,
        dictionary: ApplicationDictionary,
    ) {
        val field = javaClass.getDeclaredField("dictionaryMap")
        field.isAccessible = true
        val dictionaryMap = field.get(this) as MutableMap<String, ApplicationDictionary>
        dictionaryMap[applicationName] = dictionary
    }

    companion object {
        private const val MY_TEST_DATA_DIR = "src/test/resources/testData/"
        private const val HANDLER_CALL_KEY = "APPLE_SCRIPT_HANDLER_CALL"

        // D-03 content anchors: public Standard Additions command names that must appear in
        // BASIC completion on the std-lib fixture. assertContains (kotlin.test) is NOT on the
        // classpath — use assertTrue(list.contains(x), msg). The set is confirmed against live
        // completion output on the first heavy run (A2 confirmation).
        private val STABLE_TERMS =
            listOf(
                "do shell script",
                "display dialog",
                "say",
                "path to",
                "current date",
            )

        private val DATE_PROPERTY_TERMS =
            listOf(
                "class",
                "year",
                "month",
                "day",
                "hours",
                "minutes",
                "seconds",
            )
    }
}
