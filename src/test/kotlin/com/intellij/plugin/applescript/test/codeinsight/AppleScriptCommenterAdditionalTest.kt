package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.AppleScriptCommenter
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptCommenterAdditionalTest : BasePlatformTestCase() {
    fun testCommenterIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "lang.commenter",
            "com.intellij.plugin.applescript.lang.ide.AppleScriptCommenter",
        )
        assertNotNull("Commenter must be registered", element)
    }

    fun testCommenterLineCommentPrefix() {
        val commenter = AppleScriptCommenter()
        assertEquals("--", commenter.lineCommentPrefix)
    }

    fun testToggleCommentOnSimpleStatement() {
        myFixture.configureByText(AppleScriptFileType, "<caret>set x to 1")
        val action = com.intellij.codeInsight.generation.actions.CommentByLineCommentAction()
        action.actionPerformedImpl(project, myFixture.editor)
        myFixture.checkResult("--set x to 1")
    }

    fun testToggleCommentRestoresOriginal() {
        myFixture.configureByText(AppleScriptFileType, "<caret>set x to 1")
        val action = com.intellij.codeInsight.generation.actions.CommentByLineCommentAction()
        action.actionPerformedImpl(project, myFixture.editor)
        myFixture.checkResult("--set x to 1")
        action.actionPerformedImpl(project, myFixture.editor)
        myFixture.checkResult("set x to 1")
    }
}
