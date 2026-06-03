package com.intellij.plugin.applescript.test.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 8 (v2.0) PARSE-02: a compound-boolean `whose` filter with doubled parentheses must
 * parse with zero `PsiErrorElement`. Per CD-01 this reuses the existing `filterReference`
 * rule (chained via `valueExpression ::= primaryExpression filterReference?`) — no forked
 * whose node.
 *
 * The single-bareword `folder 1` index half of testTracksWhose (`first item of folder 1
 * whose name is "x"`) is the deferred 08-06 follow-through (defer-folder-half verdict) and
 * stays tracked by ParserRegressionTest.testTracksWhose — it is intentionally NOT asserted
 * here, so this harness isolates the whose-clause capability.
 *
 * Heavy-by-default suite (BasePlatformTestCase boots a ~30s fixture); opt OUT with
 * `-PskipHeavyTests=true`. Mirrors the RealWorldCorpusTest error-count harness verbatim.
 */
class WhoseClauseTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(CORPUS_DIR).absolutePath

    fun testCompoundBooleanWhose() = assertNoParserErrors()

    private fun assertNoParserErrors() {
        val psiFile: PsiFile = myFixture.configureByFile(WHOSE_CLAUSE_FILE)
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
        fail("$WHOSE_CLAUSE_FILE has ${errors.size} parser error(s):\n$report")
    }

    companion object {
        private const val CORPUS_DIR = "src/test/resources/testData/parse/realWorld"
        private const val WHOSE_CLAUSE_FILE = "whose_clause_min.applescript"
    }
}
