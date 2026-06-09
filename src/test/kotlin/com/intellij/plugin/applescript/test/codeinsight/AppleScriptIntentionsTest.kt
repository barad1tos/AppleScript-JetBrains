package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.intentions.AddApplicationDictionaryQuickFix
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptIntentionsTest : BasePlatformTestCase() {
    fun testAddDictionaryQuickFixAvailableOnAppleScriptFile() {
        myFixture.configureByText(AppleScriptFileType, """tell application "Finder" to activate""")
        val quickFix = AddApplicationDictionaryQuickFix("Finder")

        assertTrue("Quick fix must be available on AppleScript file", quickFix.isAvailable(project, myFixture.editor, myFixture.file))
    }

    fun testAddDictionaryQuickFixTextAndFamily() {
        val quickFix = AddApplicationDictionaryQuickFix("TestApp")
        assertEquals("Add dictionary for application", quickFix.text)
        assertEquals("AppleScript", quickFix.familyName)
    }

    fun testAddDictionaryQuickFixStartsNotInWriteAction() {
        val quickFix = AddApplicationDictionaryQuickFix("TestApp")
        assertFalse("Quick fix must not start in write action", quickFix.startInWriteAction())
    }
}
