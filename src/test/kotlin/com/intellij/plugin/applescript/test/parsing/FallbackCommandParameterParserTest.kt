package com.intellij.plugin.applescript.test.parsing

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.psi.AppleScriptCommandParameterSelector
import com.intellij.plugin.applescript.psi.AppleScriptDirectParameterVal
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FallbackCommandParameterParserTest : BasePlatformTestCase() {
    fun testStandardAdditionsFallbackParametersExposePsiNodes() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                display dialog "Hello" default answer "Name" with title "Prompt" giving up after 3
                do shell script "printf test" without altering line endings
                read file "sample.txt"
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)

        val selectors =
            PsiTreeUtil.findChildrenOfType(
                psiFile,
                AppleScriptCommandParameterSelector::class.java,
            )
        val selectorTexts = selectors.map { it.text }
        val hasNamedParameterSelectors =
            selectorTexts.containsAll(
                listOf(
                    "default answer",
                    "with title",
                    "giving up after",
                ),
            )
        val hasBooleanParameterSelector =
            selectorTexts.any { it == "without" || it == "altering line endings" }

        assertTrue(
            "fallback command parameters should expose selector PSI nodes, got: $selectorTexts",
            hasNamedParameterSelectors && hasBooleanParameterSelector,
        )

        val directParameters =
            PsiTreeUtil.findChildrenOfType(
                psiFile,
                AppleScriptDirectParameterVal::class.java,
            )

        assertTrue(
            "fallback command parameters should preserve direct parameter PSI nodes",
            directParameters.size >= 3,
        )
    }

    private fun assertNoParserErrors(psiFile: PsiFile) {
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        if (errors.isEmpty()) return

        val text = psiFile.text
        val report =
            errors.joinToString("\n") { error ->
                val offset = error.textRange.startOffset
                val line = text.substring(0, offset).count { it == '\n' } + 1
                val snippet = error.text.replace("\n", "\\n").take(40)
                "  line $line offset $offset: '$snippet' - ${error.errorDescription}"
            }
        fail("fallback command parameter fixture has ${errors.size} parser error(s):\n$report")
    }
}
