package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
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

    fun testDictionaryRolesUseThemeAwareFallbacks() {
        assertFallback(
            AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
            DefaultLanguageHighlighterColors.FUNCTION_CALL,
        )
        assertFallback(
            AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
            DefaultLanguageHighlighterColors.KEYWORD,
        )
        assertFallback(
            AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR,
            DefaultLanguageHighlighterColors.CLASS_NAME,
        )
        assertFallback(
            AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR,
            DefaultLanguageHighlighterColors.INSTANCE_FIELD,
        )
        assertFallback(
            AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR,
            DefaultLanguageHighlighterColors.CONSTANT,
        )
    }

    fun testCoreRolesUseThemeAwareFallbacks() {
        val expectedFallbacks =
            mapOf(
                AppleScriptSyntaxHighlighterColors.KEYWORD to DefaultLanguageHighlighterColors.KEYWORD,
                AppleScriptSyntaxHighlighterColors.LOGICAL_OPERATOR to DefaultLanguageHighlighterColors.OPERATION_SIGN,
                AppleScriptSyntaxHighlighterColors.COMPARISON_OPERATOR to
                    DefaultLanguageHighlighterColors.OPERATION_SIGN,
                AppleScriptSyntaxHighlighterColors.LANGUAGE_LITERAL to DefaultLanguageHighlighterColors.CONSTANT,
                AppleScriptSyntaxHighlighterColors.BUILT_IN_TYPE to DefaultLanguageHighlighterColors.CLASS_NAME,
                AppleScriptSyntaxHighlighterColors.HANDLER_CALL to DefaultLanguageHighlighterColors.FUNCTION_CALL,
                AppleScriptSyntaxHighlighterColors.VARIABLE to DefaultLanguageHighlighterColors.LOCAL_VARIABLE,
                AppleScriptSyntaxHighlighterColors.STRING to DefaultLanguageHighlighterColors.STRING,
                AppleScriptSyntaxHighlighterColors.NUMBER to DefaultLanguageHighlighterColors.NUMBER,
                AppleScriptSyntaxHighlighterColors.OPERATION_SIGN to DefaultLanguageHighlighterColors.OPERATION_SIGN,
                AppleScriptSyntaxHighlighterColors.COMMENT to DefaultLanguageHighlighterColors.LINE_COMMENT,
            )

        for ((key, fallbackKey) in expectedFallbacks) {
            assertFallback(key, fallbackKey)
        }
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

    private fun assertFallback(
        key: TextAttributesKey,
        expectedFallback: TextAttributesKey,
    ) {
        assertTrue(
            "${key.externalName} must inherit from ${expectedFallback.externalName}",
            key.fallbackAttributeKey === expectedFallback,
        )
    }
}
