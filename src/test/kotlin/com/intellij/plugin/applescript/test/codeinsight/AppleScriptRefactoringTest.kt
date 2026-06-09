package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.refactoring.AppleScriptElementRefactoringSupportProvider
import com.intellij.plugin.applescript.lang.ide.refactoring.AppleScriptRefactoringSupportProvider
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptRefactoringTest : BasePlatformTestCase() {
    private val elementProvider = AppleScriptElementRefactoringSupportProvider()
    private val memberProvider = AppleScriptRefactoringSupportProvider()

    fun testSafeDeleteAvailableForNamedElement() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on run
                set myVar to 123
            end run
            """.trimIndent(),
        )
        val variable = PsiTreeUtil.findChildrenOfType(myFixture.file, AppleScriptTargetVariable::class.java)
            .first { it.name == "myVar" }

        assertTrue("Safe delete must be available for named element", elementProvider.isSafeDeleteAvailable(variable))
    }

    fun testSafeDeleteNotAvailableForNonNamedElement() {
        myFixture.configureByText(
            AppleScriptFileType,
            """tell application "Finder" to activate""",
        )
        val tellKeyword = myFixture.file.findElementAt(0)
        assertNotNull(tellKeyword)
        assertFalse("Safe delete must not be available for non-named element", elementProvider.isSafeDeleteAvailable(tellKeyword!!))
    }

    fun testElementProviderIsNotNull() {
        assertNotNull("Provider must not be null", elementProvider)
    }

    fun testMemberProviderIsNotNull() {
        assertNotNull("Provider must not be null", memberProvider)
    }

    fun testElementProviderSupportsSafeDelete() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on run
                set myVar to 123
            end run
            """.trimIndent(),
        )
        val variable = PsiTreeUtil.findChildrenOfType(myFixture.file, AppleScriptTargetVariable::class.java)
            .first { it.name == "myVar" }

        assertTrue("Element provider must support safe delete for variables", elementProvider.isSafeDeleteAvailable(variable))
    }
}
