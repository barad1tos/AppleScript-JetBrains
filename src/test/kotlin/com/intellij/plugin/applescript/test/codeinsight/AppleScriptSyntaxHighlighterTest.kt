package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighter
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterColors
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

    fun testDictionarySelectorAndConstantUseDistinctFallbackHighlighting() {
        val selectorFallback = AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR.fallbackAttributeKey
        val constantFallback = AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR.fallbackAttributeKey

        assertTrue(
            "dictionary command selector should inherit keyword highlighting by default",
            selectorFallback === DefaultLanguageHighlighterColors.KEYWORD,
        )
        assertTrue(
            "dictionary constant should keep constant highlighting by default",
            constantFallback === DefaultLanguageHighlighterColors.CONSTANT,
        )
        assertTrue(
            "dictionary selector and constant fallbacks must stay visually distinguishable by default",
            selectorFallback !== constantFallback,
        )
    }

    private fun assertHighlightName(
        highlighter: AppleScriptSyntaxHighlighter,
        tokenType: IElementType,
        expectedExternalName: String,
    ) {
        val actualNames =
            highlighter
                .getTokenHighlights(tokenType)
                .mapTo(mutableSetOf()) { key -> key.externalName }

        assertTrue(
            "$tokenType should include $expectedExternalName; actual keys=$actualNames",
            expectedExternalName in actualNames,
        )
    }
}
