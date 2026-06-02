package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.generation.actions.CommentByLineCommentAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterColors
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.psi.codeStyle.CodeStyleManager
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

    fun testDatePropertyReferencesUsePropertyHighlighting() {
        val script = """
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

    fun testMyHandlerCallUsesFunctionHighlighting() {
        val script = """
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

    private fun highlightingKeysFor(highlights: List<HighlightInfo>, textRange: TextRange): Set<TextAttributesKey> =
        highlights
            .filter { highlight -> textRange.intersects(highlight.startOffset, highlight.endOffset) }
            .mapNotNullTo(mutableSetOf()) { highlight -> highlight.forcedTextAttributesKey }

    private fun highlightingKeyNamesFor(highlights: List<HighlightInfo>, textRange: TextRange): Set<String> =
        highlightingKeysFor(highlights, textRange)
            .mapTo(mutableSetOf()) { key -> key.externalName }

    private fun textRangeFor(document: Document, text: String): TextRange {
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
        private val STABLE_TERMS = listOf(
            "do shell script",
            "display dialog",
            "say",
            "path to",
            "current date",
        )

        private val DATE_PROPERTY_TERMS = listOf(
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
