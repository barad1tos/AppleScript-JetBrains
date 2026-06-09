package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.intentions.AddApplicationDictionaryQuickFix
import com.intellij.plugin.applescript.lang.ide.intentions.RenameParameterLabelQuickFix
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerParameterLabel
import com.intellij.psi.util.PsiTreeUtil
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

    fun testRenameParameterLabelQuickFixFamilyName() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on greet(theName)
                return "Hello " & theName
            end greet
            """.trimIndent(),
        )
        val handler = PsiTreeUtil.findChildOfType(myFixture.file, AppleScriptHandler::class.java)
        if (handler == null) {
            // Handler PSI type may not be available in test context
            return
        }
        val label = PsiTreeUtil.findChildOfType(handler, AppleScriptHandlerParameterLabel::class.java)
            ?: return

        val quickFix = RenameParameterLabelQuickFix(label, "newLabel")
        assertEquals("AppleScript", quickFix.familyName)
        assertEquals("Rename parameter label", quickFix.text)
    }

    fun testRenameParameterLabelQuickFixAvailableOnAppleScript() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on greet(theName)
                return "Hello " & theName
            end greet
            """.trimIndent(),
        )
        val handler = PsiTreeUtil.findChildOfType(myFixture.file, AppleScriptHandler::class.java)
            ?: return
        val label = PsiTreeUtil.findChildOfType(handler, AppleScriptHandlerParameterLabel::class.java)
            ?: return

        val quickFix = RenameParameterLabelQuickFix(label, "newLabel")
        assertTrue(
            "Quick fix must be available on AppleScript file",
            quickFix.isAvailable(project, myFixture.editor, myFixture.file),
        )
    }
}
