package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighter
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTEGER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LAND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MISSING_VALUE
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptSyntaxHighlighterTest : BasePlatformTestCase() {

    fun testLanguagePrimitiveTokensUseDedicatedHighlighting() {
        val highlighter = AppleScriptSyntaxHighlighter()

        assertHighlightName(highlighter, LAND, "APPLE_SCRIPT_LOGICAL_OPERATOR")
        assertHighlightName(highlighter, GT, "APPLE_SCRIPT_COMPARISON_OPERATOR")
        assertHighlightName(highlighter, MISSING_VALUE, "APPLE_SCRIPT_LANGUAGE_LITERAL")
        assertHighlightName(highlighter, INTEGER, "APPLE_SCRIPT_BUILT_IN_TYPE")
    }

    private fun assertHighlightName(
        highlighter: AppleScriptSyntaxHighlighter,
        tokenType: IElementType,
        expectedExternalName: String,
    ) {
        val actualNames = highlighter
            .getTokenHighlights(tokenType)
            .mapTo(mutableSetOf()) { key -> key.externalName }

        assertTrue(
            "$tokenType should include $expectedExternalName; actual keys=$actualNames",
            expectedExternalName in actualNames,
        )
    }
}
