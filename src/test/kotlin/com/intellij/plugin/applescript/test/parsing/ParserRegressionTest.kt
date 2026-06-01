package com.intellij.plugin.applescript.test.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 8 regression suite: parses real-world snippets that triggered false-positive
 * errors in 1.0.0 and asserts the PSI tree contains zero PsiErrorElement nodes.
 *
 * Each .scpt under src/test/resources/testData/parse/regressions/ produces one assertion.
 * On failure the message lists every error node with offset + text + reason so the
 * fix loop has a 30 s feedback cycle without an IDE roundtrip.
 *
 * Only runs when -PincludeHeavyTests=true is passed because BasePlatformTestCase
 * boots a full fixture (~30 s) and AppleScriptSystemDictionaryRegistryService
 * scans /Applications.
 */
class ParserRegressionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(REGRESSION_DIR).absolutePath

    fun testTryMinimal() = assertNoParserErrors("try_minimal.scpt")

    fun testTellEndtell() = assertNoParserErrors("tell_endtell.scpt")

    fun testTracksWhose() = assertNoParserErrors("tracks_whose.scpt")

    fun testMusicScript() = assertNoParserErrors("music_script.scpt")

    fun testCountOfArgv() = assertNoParserErrors("count_of_argv.scpt")

    fun testDoShellScript() = assertNoParserErrors("do_shell_script.scpt")

    fun testDateProperties() = assertNoParserErrors("date_properties.scpt")

    private fun assertNoParserErrors(fileName: String) {
        val psiFile: PsiFile = myFixture.configureByFile(fileName)
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        if (errors.isEmpty()) return
        val text = psiFile.text
        val report = errors.joinToString("\n") { err ->
            val offset = err.textRange.startOffset
            val line = text.substring(0, offset).count { it == '\n' } + 1
            val snippet = err.text.replace("\n", "\\n").take(40)
            "  line $line offset $offset: '$snippet' — ${err.errorDescription}"
        }
        fail("$fileName has ${errors.size} parser error(s):\n$report")
    }

    companion object {
        private const val REGRESSION_DIR = "src/test/resources/testData/parse/regressions"
    }
}
