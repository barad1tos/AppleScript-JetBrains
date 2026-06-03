package com.intellij.plugin.applescript.test.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 8 (v2.0) PARSE-06: a nested `tell application "name" ... end tell` block inside an
 * `on run argv ... end run` handler body must parse with zero `PsiErrorElement` and no
 * Incomplete-expression false positive. Verify-first against the migrated GK 2023.3 parser
 * (existing tellCompoundStatement rule).
 *
 * Heavy-by-default suite (BasePlatformTestCase boots a ~30s fixture); opt OUT with
 * `-PskipHeavyTests=true`. Mirrors the RealWorldCorpusTest error-count harness verbatim.
 */
class TellBlockHardeningTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(CORPUS_DIR).absolutePath

    fun testNestedTellInRunHandler() = assertNoParserErrors()

    private fun assertNoParserErrors() {
        val psiFile: PsiFile = myFixture.configureByFile(NESTED_TELL_FILE)
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
        fail("$NESTED_TELL_FILE has ${errors.size} parser error(s):\n$report")
    }

    companion object {
        private const val CORPUS_DIR = "src/test/resources/testData/parse/realWorld"
        private const val NESTED_TELL_FILE = "nested_tell_min.applescript"
    }
}
