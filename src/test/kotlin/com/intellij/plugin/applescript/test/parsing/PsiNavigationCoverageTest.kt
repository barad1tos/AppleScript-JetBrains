package com.intellij.plugin.applescript.test.parsing

import com.intellij.plugin.applescript.AppleScriptFile
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PsiNavigationCoverageTest : BasePlatformTestCase() {
    fun testTellBlockPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                activate
                get name of folder 1
            end tell
            """.trimIndent(),
        )
        val file = myFixture.file
        assertNotNull(file)
        assertTrue(file is AppleScriptFile)
        val children = file.children
        assertTrue("File must have children", children.isNotEmpty())
        val tellStatements = PsiTreeUtil.findChildrenOfType(file, AppleScriptTellCompoundStatement::class.java)
        assertFalse("Must find tell statements", tellStatements.isEmpty())
    }

    fun testIfStatementPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            if true then
                set x to 1
            else if false then
                set x to 2
            else
                set x to 3
            end if
            """.trimIndent(),
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testHandlerPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on greet(theName)
                return "Hello " & theName
            end greet
            """.trimIndent(),
        )
        val file = myFixture.file
        val handlers = PsiTreeUtil.findChildrenOfType(file, AppleScriptHandler::class.java)
        assertFalse("Must find handlers", handlers.isEmpty())
    }

    fun testVariableAssignmentPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            set myVar to 123
            set myString to "hello"
            """.trimIndent(),
        )
        val file = myFixture.file
        val variables = PsiTreeUtil.findChildrenOfType(file, AppleScriptTargetVariable::class.java)
        assertFalse("Must find variables", variables.isEmpty())
    }

    fun testTryCatchPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            try
                set x to 1 / 0
            on error errMsg number errNum
                display dialog errMsg
            end try
            """.trimIndent(),
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testRepeatLoopPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            repeat with i from 1 to 10
                log i
            end repeat
            """.trimIndent(),
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testScriptObjectPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            script MyScript
                property x : 1
                property y : "hello"

                on doSomething
                    return x & y
                end doSomething
            end script
            """.trimIndent(),
        )
        val file = myFixture.file
        val scriptObjects = PsiTreeUtil.findChildrenOfType(file, AppleScriptScriptObject::class.java)
        assertFalse("Must find script objects", scriptObjects.isEmpty())
    }

    fun testComplexScriptPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            property myProp : "default"

            on run argv
                set input to item 1 of argv

                tell application "Finder"
                    set desktopFiles to every file of desktop
                    repeat with f in desktopFiles
                        log name of f
                    end repeat
                end tell

                try
                    set result to do shell script "echo " & input
                on error errMsg
                    display dialog errMsg
                end try

                return result
            end run
            """.trimIndent(),
        )
        val file = myFixture.file
        val allElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
        assertTrue("Complex script must have many PSI elements", allElements.size > 20)
    }

    fun testHandlerCallPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on add(a, b)
                return a + b
            end add

            on run
                set result to add(1, 2)
                log result
            end run
            """.trimIndent(),
        )
        val file = myFixture.file
        val handlerCalls = PsiTreeUtil.findChildrenOfType(file, AppleScriptHandlerCall::class.java)
        assertFalse("Must find handler calls", handlerCalls.isEmpty())
    }

    fun testNestedTellBlocksPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                tell folder "Desktop"
                    set fileList to every file
                    repeat with f in fileList
                        log name of f
                    end repeat
                end tell
            end tell
            """.trimIndent(),
        )
        val file = myFixture.file
        val tellStatements = PsiTreeUtil.findChildrenOfType(file, AppleScriptTellCompoundStatement::class.java)
        assertTrue("Must find nested tell statements", tellStatements.size >= 2)
    }

    fun testListLiteralPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """set myList to {1, 2, 3, "hello", true}""",
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testRecordLiteralPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """set myRecord to {name:"John", age:30, active:true}""",
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testMultiHandlerScriptPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on validate(input)
                if input is missing value then
                    return false
                end if
                return true
            end validate

            on process(input)
                if not validate(input) then
                    return "invalid"
                end if
                return "processed: " & input
            end process

            on run
                set result to process("test")
                log result
            end run
            """.trimIndent(),
        )
        val file = myFixture.file
        val handlers = PsiTreeUtil.findChildrenOfType(file, AppleScriptHandler::class.java)
        assertEquals("Must find 3 handlers", 3, handlers.size)
    }

    fun testConsideringIgnoringPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            considering case
                if "ABC" is "abc" then
                    log "equal"
                end if
            end considering
            """.trimIndent(),
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testUsingTermsFromPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            using terms from application "Finder"
                tell application "Finder"
                    activate
                end tell
            end using terms from
            """.trimIndent(),
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testAllExpressionTypesPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on run
                set a to 1 + 2
                set b to 3 - 4
                set c to 5 * 6
                set d to 7 / 8
                set e to 9 mod 10
                set f to 11 div 12
                set g to 13 ^ 14
                set h to "hello" & "world"
                set i to 1 > 2
                set j to 3 < 4
                set k to 5 >= 6
                set l to 7 <= 8
                set m to 9 is 10
                set n to 11 is not 12
                set o to true and false
                set p to true or false
                set q to not true
                set r to 1 as integer
                set s to item 1 of {1, 2, 3}
                set t to every item of {1, 2, 3}
            end run
            """.trimIndent(),
        )
        val file = myFixture.file
        val allElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
        assertTrue("Script with all expression types must have many PSI elements", allElements.size > 50)
    }

    fun testTellSimpleStatementPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """tell application "Finder" to activate""",
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testAssignmentStatementPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            set x to 1
            set y to "hello"
            set z to true
            set w to missing value
            set v to {1, 2, 3}
            """.trimIndent(),
        )
        val file = myFixture.file
        val variables = PsiTreeUtil.findChildrenOfType(file, AppleScriptTargetVariable::class.java)
        assertTrue("Must find multiple variables", variables.size >= 5)
    }

    fun testPropertyReferencePsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                get name of folder 1
                set n to name of file 1
            end tell
            """.trimIndent(),
        )
        val file = myFixture.file
        val propRefs = PsiTreeUtil.findChildrenOfType(file, AppleScriptPropertyReference::class.java)
        assertFalse("Must find property references", propRefs.isEmpty())
    }

    fun testHandlerWithLabeledParametersPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on moveItem:theItem toFolder:theFolder withOverwrite:doOverwrite
                return theItem
            end moveItem:toFolder:withOverwrite:
            """.trimIndent(),
        )
        val file = myFixture.file
        val handlers = PsiTreeUtil.findChildrenOfType(file, AppleScriptHandler::class.java)
        assertFalse("Must find handler with labeled parameters", handlers.isEmpty())
    }

    fun testWhoseClausePsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                get every file of desktop whose name ends with ".txt"
            end tell
            """.trimIndent(),
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testMultiplePropertiesPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            property name : "MyScript"
            property version : "1.0"
            property debug : false

            on run
                log name & " v" & version
            end run
            """.trimIndent(),
        )
        val file = myFixture.file
        assertTrue("File must have children", file.children.isNotEmpty())
    }

    fun testDeeplyNestedPsiNavigation() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on run
                tell application "Finder"
                    if desktop exists then
                        repeat with f in (every file of desktop)
                            try
                                log name of f
                            on error
                                log "error"
                            end try
                        end repeat
                    end if
                end tell
            end run
            """.trimIndent(),
        )
        val file = myFixture.file
        val allElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
        assertTrue("Deeply nested script must have many PSI elements", allElements.size > 30)
    }
}
