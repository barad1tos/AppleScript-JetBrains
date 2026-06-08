package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.structure.AppleScriptStructureViewModel
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptStructureViewTest : BasePlatformTestCase() {
    fun testStructureViewShowsTopLevelScriptDeclarations() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            property scriptName : "smoke"

            on run argv
                return argv
            end run

            script Worker
                property enabled : true
            end script
            """.trimIndent(),
        )

        val model = AppleScriptStructureViewModel(myFixture.file, myFixture.editor)
        val childNames = model.root.children.mapNotNull { child -> child.presentation.presentableText }

        assertTrue(
            "Structure View must expose top-level declarations, got $childNames",
            childNames.any { childName -> childName.startsWith("scriptName") } &&
                childNames.any { childName -> childName.startsWith("run") } &&
                childNames.contains("Worker"),
        )
    }
}
