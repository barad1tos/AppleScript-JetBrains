package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.generation.actions.CommentByLineCommentAction
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterColors
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.plugin.applescript.test.service.SyntheticSuiteFixtures
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

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

    private fun textRangeFor(
        document: Document,
        text: String,
    ): TextRange {
        val startOffset = document.charsSequence.indexOf(text)
        assertTrue("expected to find '$text'", startOffset >= 0)
        return TextRange(startOffset, startOffset + text.length)
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
