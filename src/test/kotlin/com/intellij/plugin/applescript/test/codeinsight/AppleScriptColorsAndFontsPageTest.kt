package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptColorsAndFontsPage
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptColorsAndFontsPageTest : BasePlatformTestCase() {
    private val page = AppleScriptColorsAndFontsPage()

    fun testDisplayName() {
        assertEquals("AppleScript", page.displayName)
    }

    fun testIconIsNotNull() {
        assertNotNull("Icon must not be null", page.icon)
    }

    fun testHighlighterIsNotNull() {
        assertNotNull("Highlighter must not be null", page.highlighter)
    }

    fun testDemoTextIsNotEmpty() {
        assertTrue("Demo text must not be empty", page.demoText.isNotEmpty())
    }

    fun testAttributeDescriptorsCount() {
        val descriptors = page.attributeDescriptors
        assertEquals("Must have 14 attribute descriptors", 14, descriptors.size)
    }

    fun testAttributeDescriptorsContainExpectedEntries() {
        val descriptors = page.attributeDescriptors
        val displayNames = descriptors.map { it.displayName }.toSet()

        assertTrue(displayNames.contains("Keyword"))
        assertTrue(displayNames.contains("Logical operator"))
        assertTrue(displayNames.contains("Comparison operator"))
        assertTrue(displayNames.contains("Language literal"))
        assertTrue(displayNames.contains("Built-in type"))
        assertTrue(displayNames.contains("Handler call"))
        assertTrue(displayNames.contains("String"))
        assertTrue(displayNames.contains("Operator"))
        assertTrue(displayNames.contains("Comment"))
        assertTrue(displayNames.contains("Dictionary command"))
        assertTrue(displayNames.contains("Command parameter name"))
        assertTrue(displayNames.contains("Dictionary class"))
        assertTrue(displayNames.contains("Dictionary property"))
        assertTrue(displayNames.contains("Dictionary constant"))
    }

    fun testTagMapContainsExpectedKeys() {
        val tags = page.additionalHighlightingTagToDescriptorMap
        assertNotNull("Tag map must not be null", tags)

        val expectedKeys =
            setOf(
                "keyword",
                "logical operator",
                "comparison operator",
                "language literal",
                "built-in type",
                "handler call",
                "string",
                "operator",
                "comment",
                "command",
                "command parameter",
                "dictionary class",
                "dictionary property",
                "dictionary constant",
            )
        assertEquals(expectedKeys, tags.keys)
    }

    fun testTagMapValuesPointToValidKeys() {
        val tags = page.additionalHighlightingTagToDescriptorMap
        for ((tag, key) in tags) {
            assertNotNull("Tag '$tag' must map to a non-null key", key)
            assertTrue("Tag '$tag' key must have an external name", key.externalName.isNotEmpty())
        }
    }

    fun testColorDescriptorsIsEmpty() {
        val descriptors = page.colorDescriptors
        assertNotNull(descriptors)
        assertEquals("Color descriptors must be empty", 0, descriptors.size)
    }

    fun testDemoTextContainsAppleScriptConstructs() {
        val demo = page.demoText
        assertTrue("Demo text must contain tell block", demo.contains("tell application"))
        assertTrue("Demo text must contain if block", demo.contains("if"))
        assertTrue("Demo text must contain comment", demo.contains("--"))
    }
}
