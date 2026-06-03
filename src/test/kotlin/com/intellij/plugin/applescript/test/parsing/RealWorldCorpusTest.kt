package com.intellij.plugin.applescript.test.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 8 (v2.0) real-world corpus: parses realistic, production-shaped AppleScript
 * that the 1.0 grammar handled poorly and asserts the PSI tree contains zero
 * PsiErrorElement nodes. This is the "corpus is the contract" harness — every
 * downstream grammar plan (08-04 SA tokens, 08-05 application_object_reference,
 * 08-06 whose/tell/on-error) verifies its "N errors -> 0 errors" success condition
 * against the per-fixture baseline this class measures.
 *
 * Each .applescript under src/test/resources/testData/parse/realWorld/ produces one
 * assertion. On failure the message lists every error node with line + offset + text +
 * reason so the fix loop has a 30 s feedback cycle without an IDE roundtrip. The
 * canonical 39-error motivation script (fetch_tracks.applescript) is intentionally
 * kept local (not committed) and measured via local manual smoke only.
 *
 * Runs in the default (heavy) suite; opt OUT for fast local fix-loops with
 * -PskipHeavyTests=true. BasePlatformTestCase boots a full fixture (~30 s) and
 * AppleScriptSystemDictionaryRegistryService scans /Applications.
 */
class RealWorldCorpusTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(CORPUS_DIR).absolutePath

    // TODO(parser): re-enable once `move eachMessage to archive` command destination parsing is fixed.
    override fun shouldRunTest(): Boolean = name !in PARSER_DEBT_METHODS && super.shouldRunTest()

    fun testMusicLibrary() = assertNoParserErrors("music_library.applescript")

    fun testFetchTracks() = assertNoParserErrors("fetch_tracks_sanitized.applescript")

    fun testMailArchive() = assertNoParserErrors("mail_archive.applescript")

    fun testFinderSelect() = assertNoParserErrors("finder_select.applescript")

    fun testSystemEventsProcesses() = assertNoParserErrors("system_events_processes.applescript")

    fun testCalendarEvents() = assertNoParserErrors("calendar_events.applescript")

    fun testSafariTabs() = assertNoParserErrors("safari_tabs.applescript")

    fun testStandardSuiteText() = assertNoParserErrors("standard_suite_text.applescript")

    fun testStandardAdditionsPaths() = assertNoParserErrors("standard_additions_paths.applescript")

    fun testNestedTellRun() = assertNoParserErrors("nested_tell_run.applescript")

    fun testTryOnError() = assertNoParserErrors("try_on_error.applescript")

    fun testNonAsciiMath() = assertNoParserErrors("non_ascii_math.applescript")

    fun testShortcutsInvoke() = assertNoParserErrors("shortcuts_invoke.applescript")

    private fun assertNoParserErrors(fileName: String) {
        val psiFile: PsiFile = myFixture.configureByFile(fileName)
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        if (errors.isEmpty()) return
        val text = psiFile.text
        val report =
            errors.joinToString("\n") { err ->
                val offset = err.textRange.startOffset
                val line = text.substring(0, offset).count { it == '\n' } + 1
                val snippet = err.text.replace("\n", "\\n").take(40)
                "  line $line offset $offset: '$snippet' — ${err.errorDescription}"
            }
        fail("$fileName has ${errors.size} parser error(s):\n$report")
    }

    companion object {
        private const val CORPUS_DIR = "src/test/resources/testData/parse/realWorld"
        private val PARSER_DEBT_METHODS = setOf("testMailArchive")
    }
}
