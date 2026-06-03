package com.intellij.plugin.applescript.test.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 8 (v2.0) PARSE-04: the Apple-shipped Standard Additions object tokens —
 * `current date`, `ASCII character N`, `ASCII number C`, and `path to <constant>` —
 * must parse as primary expressions with zero `PsiErrorElement` nodes, WITHOUT a
 * loaded application dictionary (D-07: the constructs consume only tokens the
 * unchanged lexer already emits, so resolution never enters the SDEF path).
 *
 * All assertions run against `standard_additions_tokens_min.applescript`, a minimal
 * hand-authored fixture isolating the SA constructs on separate top-level statements.
 * RED before the 08-05 BNF productions land — the lexer emits `current date` as the
 * discrete tokens CURRENT + DATE with no production consuming the pair, so at least
 * one error surfaces; GREEN after the additive productions + gen regen.
 *
 * Heavy-by-default suite (BasePlatformTestCase boots a ~30s fixture); opt OUT with
 * `-PskipHeavyTests=true`. Mirrors the RealWorldCorpusTest error-count harness verbatim.
 */
class StandardAdditionsTokensTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(CORPUS_DIR).absolutePath

    fun testAsciiCharacter() = assertNoParserErrors()

    fun testAsciiNumber() = assertNoParserErrors()

    fun testCurrentDate() = assertNoParserErrors()

    fun testPathToConstant() = assertNoParserErrors()

    /** D-07: the SA tokens parse with no app dictionary loaded (default fixture, cold cache). */
    fun testColdCacheSdefIndependent() = assertNoParserErrors()

    private fun assertNoParserErrors() {
        val psiFile: PsiFile = myFixture.configureByFile(SA_TOKENS_FIXTURE)
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
        fail("$SA_TOKENS_FIXTURE has ${errors.size} parser error(s):\n$report")
    }

    companion object {
        private const val CORPUS_DIR = "src/test/resources/testData/parse/realWorld"
        private const val SA_TOKENS_FIXTURE = "standard_additions_tokens_min.applescript"
    }
}
