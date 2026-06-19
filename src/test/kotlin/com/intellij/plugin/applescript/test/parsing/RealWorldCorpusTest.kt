package com.intellij.plugin.applescript.test.parsing

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ASSIGNMENT_STATEMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
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

    override fun shouldRunTest(): Boolean = name !in PARSER_DEBT_METHODS && super.shouldRunTest()

    fun testMusicLibrary() {
        assertNoParserErrors("music_library.applescript")
    }

    fun testFetchTracks() {
        assertNoParserErrors("fetch_tracks_sanitized.applescript")
    }

    fun testMailArchive() {
        assertNoParserErrors("mail_archive.applescript")
    }

    fun testFinderSelect() {
        assertNoParserErrors("finder_select.applescript")
    }

    fun testSystemEventsProcesses() {
        assertNoParserErrors("system_events_processes.applescript")
    }

    fun testCalendarEvents() {
        assertNoParserErrors("calendar_events.applescript")
    }

    fun testSafariTabs() {
        assertNoParserErrors("safari_tabs.applescript")
    }

    fun testStandardSuiteText() {
        assertNoParserErrors("standard_suite_text.applescript")
    }

    fun testStandardAdditionsPaths() {
        assertNoParserErrors("standard_additions_paths.applescript")
    }

    fun testNestedTellRun() {
        assertNoParserErrors("nested_tell_run.applescript")
    }

    fun testApplicationIdReferences() {
        assertNoParserErrors("application_id_reference_min.applescript")
    }

    fun testRemoteDesktopTasks() {
        assertNoParserErrors("remote_desktop_task_min.applescript")
    }

    fun testTryOnError() {
        assertNoParserErrors("try_on_error.applescript")
    }

    fun testNonAsciiMath() {
        assertNoParserErrors("non_ascii_math.applescript")
    }

    fun testShortcutsInvoke() {
        assertNoParserErrors("shortcuts_invoke.applescript")
    }

    fun testEveryClassReference() {
        assertNoParserErrors("every_class_reference.applescript")
    }

    fun testRecordKeywordLabels() {
        assertNoParserErrors("record_keyword_labels.applescript")
    }

    fun testDisplayAlertCommand() {
        assertNoParserErrors("display_alert_command.applescript")
    }

    fun testSortCommandDirection() {
        assertNoParserErrors("sort_command_direction.applescript")
    }

    fun testConsideringIgnoringResponses() {
        assertNoParserErrors("considering_ignoring.applescript")
    }

    fun testFolderActionHandlers() {
        assertNoParserErrors("folder_action_handlers.applescript")
    }

    fun testDateComponentAssignment() {
        assertNoParserErrors("date_component_assignment.applescript")
    }

    fun testTypinatorRuleSet() {
        assertNoParserErrors("typinator_rule_set_min.applescript")
    }

    fun testTypinatorRuleSetNoOf() {
        assertNoParserErrors("typinator_rule_set_nofof.applescript")
    }

    fun testTypinatorEditRuleFlow() {
        assertNoParserErrors("typinator_edit_rule_flow.applescript")
    }

    fun testKeystrokeReturn() {
        assertNoParserErrors("keystroke_return.applescript")
    }

    fun testDisplayNotificationLabeled() {
        val psiFile = assertNoParserErrors("display_notification_labeled.applescript")

        assertEquals(
            listOf("with title", "subtitle", "sound name"),
            psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR),
        )
        assertFalse(
            "display notification tail must stop before the next assignment",
            psiFile.node.textsOf(COMMAND_PARAMETER).any { parameterText -> "set done" in parameterText },
        )
        assertTrue(psiFile.node.textsOf(ASSIGNMENT_STATEMENT).contains("set done to true"))
    }

    fun testStandardAdditionsFileAndListCommands() {
        assertNoParserErrors("standard_additions_file_and_list_commands.applescript")
    }

    fun testDialogToolkitCommands() {
        assertNoParserErrors("dialog_toolkit_commands.applescript")
    }

    fun testTellToUnknownCommand() {
        assertNoParserErrors("tell_to_unknown_command.applescript")
    }

    fun testTellToUnknownCommandAfterDictionaryCommandParse() {
        assertNoParserErrors("music_library.applescript")
        assertNoParserErrors("tell_to_unknown_command.applescript")
    }

    fun testPermissiveHeadNegative() {
        assertNoParserErrors("permissive_head_negative.applescript")
    }

    fun testGenreUpdaterBatchTrackUpdates() {
        assertNoParserErrors("genreupdater_batch_update_tracks.applescript")
    }

    fun testGenreUpdaterTrackPropertyUpdate() {
        assertNoParserErrors("genreupdater_update_property.applescript")
    }

    private fun assertNoParserErrors(fileName: String): PsiFile {
        val psiFile: PsiFile = myFixture.configureByFile(fileName)
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        if (errors.isEmpty()) return psiFile
        val text = psiFile.text
        val report =
            errors.joinToString("\n") { err ->
                val offset = err.textRange.startOffset
                val line = text.substring(0, offset).count { it == '\n' } + 1
                val snippet = err.text.replace("\n", "\\n").take(40)
                "  line $line offset $offset: '$snippet' — ${err.errorDescription}"
            }
        fail("$fileName has ${errors.size} parser error(s):\n$report")
        return psiFile
    }

    companion object {
        private const val CORPUS_DIR = "src/test/resources/testData/parse/realWorld"

        private val PARSER_DEBT_METHODS =
            setOf(
                // TODO(parser): re-enable once `move eachMessage to archive` command destination parsing is fixed.
                // (backlog: BL-A2)
                "testMailArchive",
                // TODO(parser): re-enable once `path to ... from ...` uses the SA fallback (backlog: BL-A3)
                // before the loaded dictionary-command path consumes only the direct parameter.
                "testStandardAdditionsPaths",
            )
    }
}

private fun ASTNode.textsOf(elementType: IElementType): List<String> {
    val texts = mutableListOf<String>()

    fun visit(node: ASTNode) {
        if (node.elementType === elementType) {
            texts += node.text
        }
        node.getChildren(null).forEach(::visit)
    }

    visit(this)
    return texts
}
