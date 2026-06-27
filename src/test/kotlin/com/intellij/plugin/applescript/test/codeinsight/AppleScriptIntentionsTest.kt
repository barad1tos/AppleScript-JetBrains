package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.intentions.AddApplicationDictionaryQuickFix
import com.intellij.plugin.applescript.lang.ide.intentions.RenameParameterLabelQuickFix
import com.intellij.plugin.applescript.psi.AppleScriptHandlerParameterLabel
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptIntentionsTest : BasePlatformTestCase() {
    fun testAddDictionaryQuickFixAvailableOnAppleScriptFile() {
        myFixture.configureByText(AppleScriptFileType, """tell application "Finder" to activate""")
        val quickFix = AddApplicationDictionaryQuickFix("Finder")

        assertTrue(
            "Quick fix must be available on AppleScript file",
            quickFix.isAvailable(project, myFixture.editor, myFixture.file),
        )
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

    fun testRenameParameterLabelQuickFixRenamesTheLabel() {
        // The user invokes the fix on a handler parameter label; the label text must change
        // in the editor (here "below" becomes "above"), which is what the user observes.
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on myHandler below onlyParam
            end myHandler
            """.trimIndent(),
        )
        val label =
            checkNotNull(PsiTreeUtil.findChildOfType(myFixture.file, AppleScriptHandlerParameterLabel::class.java)) {
                "Expected a handler parameter label in the configured handler signature"
            }
        assertEquals("Test setup: starting label", "below", label.text)

        RenameParameterLabelQuickFix(label, "above").invoke(project, myFixture.editor, myFixture.file)
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        assertTrue(
            "Rename fix must replace the parameter label in the editor",
            myFixture.file.text.contains("above onlyParam"),
        )
        assertFalse(
            "The old parameter label must be gone after the rename",
            myFixture.file.text.contains("below onlyParam"),
        )
    }

    fun testRenameParameterLabelQuickFixStartsNotInWriteAction() {
        // Must be false: invoke() schedules its own invokeLater { runWriteAction { ... } },
        // so the platform must not additionally wrap the action in a write action. If this
        // returned true, the platform would double-wrap the write and break the fix.
        val quickFix = RenameParameterLabelQuickFix(firstParameterLabel(), "above")
        assertFalse("Rename fix must not start in a write action", quickFix.startInWriteAction())
    }

    fun testRenameParameterLabelQuickFixTextAndFamily() {
        val quickFix = RenameParameterLabelQuickFix(firstParameterLabel(), "above")
        assertEquals("Rename parameter label", quickFix.text)
        assertEquals("AppleScript", quickFix.familyName)
    }

    private fun firstParameterLabel(): AppleScriptHandlerParameterLabel {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on myHandler below onlyParam
            end myHandler
            """.trimIndent(),
        )
        return checkNotNull(
            PsiTreeUtil.findChildOfType(myFixture.file, AppleScriptHandlerParameterLabel::class.java),
        ) { "Expected a handler parameter label in the configured handler signature" }
    }
}
