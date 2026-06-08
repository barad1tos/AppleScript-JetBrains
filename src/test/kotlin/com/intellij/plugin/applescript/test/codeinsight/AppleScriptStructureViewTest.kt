package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.structure.AppleScriptStructureViewModel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class AppleScriptStructureViewTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(TEST_DATA_DIR).absolutePath

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
        val childNames =
            rootChildren
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

    fun testStructureViewShowsRunHandlerForRealWorldFetchTracksFixture() {
        myFixture.configureByFile("parse/realWorld/fetch_tracks_sanitized.applescript")

        val model = AppleScriptStructureViewModel(myFixture.file, myFixture.editor)
        val childNames =
            model.root.children
                .mapNotNull { child -> child.presentation.presentableText }

        assertTrue(
            "Structure View must expose the top-level run handler for realistic Music scripts, got $childNames",
            childNames.any { childName -> childName.startsWith("run") },
        )
    }

    companion object {
        private const val TEST_DATA_DIR = "src/test/resources/testData/"
    }
}
