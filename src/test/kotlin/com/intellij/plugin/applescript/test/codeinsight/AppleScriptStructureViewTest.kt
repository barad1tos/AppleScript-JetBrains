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
        val rootChildren = model.root.children
        assertEquals("Structure View root must contain one file wrapper", 1, rootChildren.size)
        val childNames =
            rootChildren
                .single()
                .children
                .mapNotNull { child -> child.presentation.presentableText }

        assertEquals(
            "Structure View must expose each top-level declaration once, got $childNames",
            1,
            childNames.count { childName -> childName.startsWith("scriptName") },
        )
        assertEquals(
            "Structure View must expose each top-level declaration once, got $childNames",
            1,
            childNames.count { childName -> childName.startsWith("run") },
        )
        assertEquals(
            "Structure View must expose each top-level declaration once, got $childNames",
            1,
            childNames.count { childName -> childName == "Worker" },
        )
    }
}
