package com.intellij.plugin.applescript.test.psi

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.plugin.applescript.psi.AppleScriptScriptObject
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptPsiFactoryTest : BasePlatformTestCase() {
    fun testCreateFileFromText() {
        val file = AppleScriptPsiElementFactory.createFile(project, "set x to 1")
        assertNotNull("Factory must create file", file)
        assertEquals(AppleScriptFileType, file.fileType)
    }

    fun testCreateExpressionFromText() {
        val file = AppleScriptPsiElementFactory.createFile(project, "set x to 123")
        assertNotNull(file)
        val children = file.children
        assertTrue("File must have children", children.isNotEmpty())
    }

    fun testCreateTellStatement() {
        val file =
            AppleScriptPsiElementFactory.createFile(
                project,
                """tell application "Finder"
    activate
end tell""",
            )
        assertNotNull(file)
        val children = file.children
        assertTrue("File must have children", children.isNotEmpty())
    }

    fun testCreateVariableAssignment() {
        val file = AppleScriptPsiElementFactory.createFile(project, "set myVar to 42")
        assertNotNull(file)
        val variable = PsiTreeUtil.findChildOfType(file, AppleScriptTargetVariable::class.java)
        assertNotNull("Must find variable in created file", variable)
        assertEquals("myVar", variable!!.name)
    }

    fun testCreateMultipleStatements() {
        val file =
            AppleScriptPsiElementFactory.createFile(
                project,
                """
                set a to 1
                set b to 2
                set c to a + b
                """.trimIndent(),
            )
        assertNotNull(file)
        val variables = PsiTreeUtil.findChildrenOfType(file, AppleScriptTargetVariable::class.java)
        assertEquals("Must find 3 variables", 3, variables.size)
    }

    fun testCreateScriptObject() {
        val file =
            AppleScriptPsiElementFactory.createFile(
                project,
                """
                script MyScript
                    property x : 1
                end script
                """.trimIndent(),
            )
        assertNotNull(file)
        val scriptObj =
            PsiTreeUtil.findChildOfType(
                file,
                AppleScriptScriptObject::class.java,
            )
        assertNotNull("Must find script object", scriptObj)
    }

    fun testCreateIfStatement() {
        val file =
            AppleScriptPsiElementFactory.createFile(
                project,
                """
                if true then
                    set x to 1
                end if
                """.trimIndent(),
            )
        assertNotNull(file)
        val children = file.children
        assertTrue("File must have children", children.isNotEmpty())
    }

    fun testCreateTryCatch() {
        val file =
            AppleScriptPsiElementFactory.createFile(
                project,
                """
                try
                    set x to 1
                on error
                    set x to 0
                end try
                """.trimIndent(),
            )
        assertNotNull(file)
        val children = file.children
        assertTrue("File must have children", children.isNotEmpty())
    }

    fun testCreateFileWithProperty() {
        val file =
            AppleScriptPsiElementFactory.createFile(
                project,
                """property myProp : "default"""",
            )
        assertNotNull(file)
        val children = file.children
        assertTrue("File must have children", children.isNotEmpty())
    }

    fun testCreateFileWithList() {
        val file =
            AppleScriptPsiElementFactory.createFile(
                project,
                """set myList to {1, 2, 3}""",
            )
        assertNotNull(file)
        val children = file.children
        assertTrue("File must have children", children.isNotEmpty())
    }

    fun testCreateFileWithRecord() {
        val file =
            AppleScriptPsiElementFactory.createFile(
                project,
                """set myRecord to {name:"John", age:30}""",
            )
        assertNotNull(file)
        val children = file.children
        assertTrue("File must have children", children.isNotEmpty())
    }
}
