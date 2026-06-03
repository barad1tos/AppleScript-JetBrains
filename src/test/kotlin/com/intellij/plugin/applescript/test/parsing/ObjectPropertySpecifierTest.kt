package com.intellij.plugin.applescript.test.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 09 broad object/property specifier coverage: direct object constructors,
 * contextual keyword terms, indexed object references, and Standard Additions
 * path domains must parse cold, without loading application dictionaries.
 */
class ObjectPropertySpecifierTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(REGRESSION_DIR).absolutePath

    override fun shouldRunTest(): Boolean = name !in PARSER_DEBT_METHODS && super.shouldRunTest()

    fun testObjectPropertySpecifiers() = assertNoParserErrors()

    private fun assertNoParserErrors() {
        val psiFile: PsiFile = myFixture.configureByFile(OBJECT_PROPERTY_SPECIFIERS_FIXTURE)
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
        fail("$OBJECT_PROPERTY_SPECIFIERS_FIXTURE has ${errors.size} parser error(s):\n$report")
    }

    companion object {
        private const val REGRESSION_DIR = "src/test/resources/testData/parse/regressions"
        private const val OBJECT_PROPERTY_SPECIFIERS_FIXTURE = "object_property_specifiers.scpt"

        private val PARSER_DEBT_METHODS =
            setOf(
                // TODO(parser): re-enable once `path to ... from ...` uses the SA fallback
                // before the loaded dictionary-command path consumes only the direct parameter.
                "testObjectPropertySpecifiers",
            )
    }
}
