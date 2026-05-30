package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.generation.actions.CommentByLineCommentAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.plugin.applescript.AppleScriptFileType
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
        myFixture.checkHighlighting(true, false, false)
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

    companion object {
        private const val MY_TEST_DATA_DIR = "src/test/resources/testData/"

        // D-03 content anchors: public Standard Additions command names that must appear in
        // BASIC completion on the std-lib fixture. assertContains (kotlin.test) is NOT on the
        // classpath — use assertTrue(list.contains(x), msg). The set is confirmed against live
        // completion output on the first heavy run (A2 confirmation).
        private val STABLE_TERMS = listOf("do shell script", "display dialog", "say", "path to", "current date")
    }
}
