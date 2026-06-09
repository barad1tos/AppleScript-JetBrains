package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptPairedBraceMatcher
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterColors
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptHighlightingFactoryTest : BasePlatformTestCase() {
    fun testSyntaxHighlighterFactoryIsRegistered() {
        val element =
            PluginDescriptorTestSupport.findElement(
                "lang.syntaxHighlighterFactory",
                "com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterFactory",
            )
        assertNotNull("SyntaxHighlighterFactory must be registered", element)
    }

    fun testFactoryCreatesHighlighter() {
        val factory = AppleScriptSyntaxHighlighterFactory()
        val highlighter = factory.getSyntaxHighlighter(project, null)
        assertNotNull("Factory must create a highlighter", highlighter)
    }

    fun testPairedBraceMatcherIsRegistered() {
        val element =
            PluginDescriptorTestSupport.findElement(
                "lang.braceMatcher",
                "com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptPairedBraceMatcher",
            )
        assertNotNull("PairedBraceMatcher must be registered", element)
    }

    fun testPairedBraceMatcherPairs() {
        val matcher = AppleScriptPairedBraceMatcher()
        val pairs = matcher.pairs
        assertNotNull("Brace pairs must not be null", pairs)
        assertTrue("Must have brace pairs", pairs.isNotEmpty())
        assertEquals("Must have 10 brace pairs", 10, pairs.size)
    }

    fun testSyntaxHighlighterColorsHaveExternalNames() {
        val colors =
            listOf(
                AppleScriptSyntaxHighlighterColors.KEYWORD,
                AppleScriptSyntaxHighlighterColors.LOGICAL_OPERATOR,
                AppleScriptSyntaxHighlighterColors.COMPARISON_OPERATOR,
                AppleScriptSyntaxHighlighterColors.LANGUAGE_LITERAL,
                AppleScriptSyntaxHighlighterColors.BUILT_IN_TYPE,
                AppleScriptSyntaxHighlighterColors.HANDLER_CALL,
                AppleScriptSyntaxHighlighterColors.STRING,
                AppleScriptSyntaxHighlighterColors.OPERATION_SIGN,
                AppleScriptSyntaxHighlighterColors.COMMENT,
                AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
                AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
                AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR,
                AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR,
                AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR,
            )
        for (color in colors) {
            assertTrue(
                "Color ${color.externalName} must have a non-empty external name",
                color.externalName.isNotEmpty(),
            )
        }
    }

    fun testSyntaxHighlighterColorsAreDistinct() {
        val colors =
            listOf(
                AppleScriptSyntaxHighlighterColors.KEYWORD,
                AppleScriptSyntaxHighlighterColors.LOGICAL_OPERATOR,
                AppleScriptSyntaxHighlighterColors.COMPARISON_OPERATOR,
                AppleScriptSyntaxHighlighterColors.LANGUAGE_LITERAL,
                AppleScriptSyntaxHighlighterColors.BUILT_IN_TYPE,
                AppleScriptSyntaxHighlighterColors.HANDLER_CALL,
                AppleScriptSyntaxHighlighterColors.STRING,
                AppleScriptSyntaxHighlighterColors.OPERATION_SIGN,
                AppleScriptSyntaxHighlighterColors.COMMENT,
            )
        val externalNames = colors.map { it.externalName }.toSet()
        assertEquals("All colors must have distinct external names", colors.size, externalNames.size)
    }
}
