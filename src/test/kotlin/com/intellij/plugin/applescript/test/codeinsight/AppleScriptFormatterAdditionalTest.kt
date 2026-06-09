package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptFormatterAdditionalTest : BasePlatformTestCase() {
    fun testFormatterIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "lang.formatter",
            "com.intellij.plugin.applescript.lang.formatter.AppleScriptFormattingModelBuilder",
        )
        assertNotNull("Formatter must be registered", element)
    }

    fun testFormatterDoesNotCrashOnTellBlock() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                activate
            end tell
            """.trimIndent(),
        )
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        assertNotNull("Formatted file must not be null", myFixture.file)
    }

    fun testFormatterDoesNotCrashOnIfBlock() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            if true then
                set x to 1
            end if
            """.trimIndent(),
        )
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        assertNotNull("Formatted file must not be null", myFixture.file)
    }

    fun testFormatterDoesNotCrashOnHandler() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on greet(name)
                return "Hello " & name
            end greet
            """.trimIndent(),
        )
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        assertNotNull("Formatted file must not be null", myFixture.file)
    }

    fun testFormatterDoesNotCrashOnNestedBlocks() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                if true then
                    activate
                end if
            end tell
            """.trimIndent(),
        )
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        assertNotNull("Formatted file must not be null", myFixture.file)
    }

    fun testFormatterDoesNotCrashOnTryBlock() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            try
                set x to 1 / 0
            on error errMsg
                display dialog errMsg
            end try
            """.trimIndent(),
        )
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        assertNotNull("Formatted file must not be null", myFixture.file)
    }

    fun testFormatterDoesNotCrashOnRepeatBlock() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            repeat with i from 1 to 10
                log i
            end repeat
            """.trimIndent(),
        )
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        assertNotNull("Formatted file must not be null", myFixture.file)
    }
}
