package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.findUsages.AppleScriptReadWriteAccessDetector
import com.intellij.plugin.applescript.lang.ide.findUsages.AppleScriptUsageTypeProvider
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptFindUsagesProvidersTest : BasePlatformTestCase() {
    private val detector = AppleScriptReadWriteAccessDetector()
    private val usageTypeProvider = AppleScriptUsageTypeProvider()

    fun testVariableDeclarationIsWriteAccess() {
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

        assertTrue("Variable declaration must be read-write accessible", detector.isReadWriteAccessible(variable))
        assertTrue("Variable declaration must be write access", detector.isDeclarationWriteAccess(variable))
    }

    fun testVariableDeclarationExpressionAccessIsWrite() {
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

        val access = detector.getExpressionAccess(variable)
        assertEquals(
            "Declaration must be write access",
            com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access.Write,
            access,
        )
    }

    fun testReferenceAccessReturnsReadForNonDeclaration() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                activate
            end tell
            """.trimIndent(),
        )
        val activateElement = myFixture.file.findElementAt(
            myFixture.editor.document.text.indexOf("activate"),
        )
        assertNotNull(activateElement)
        val access = detector.getExpressionAccess(activateElement!!)
        assertEquals(
            "Non-declaration must be read access",
            com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access.Read,
            access,
        )
    }

    fun testNonReadWriteAccessibleElement() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                activate
            end tell
            """.trimIndent(),
        )
        val tellKeyword = myFixture.file.findElementAt(0)
        assertNotNull(tellKeyword)
        assertFalse("tell keyword must not be read-write accessible", detector.isReadWriteAccessible(tellKeyword!!))
    }

    fun testUsageTypeProviderReturnsNullForNonAppleScriptElement() {
        myFixture.configureByText(PlainTextFileType.INSTANCE, "just plain text")
        val element = myFixture.file.findElementAt(0)
        assertNotNull(element)
        assertNull("Non-AppleScript element must return null usage type", usageTypeProvider.getUsageType(element!!))
    }

    fun testUsageTypeProviderReturnsNullForTellKeyword() {
        myFixture.configureByText(
            AppleScriptFileType,
            """tell application "Finder" to activate""",
        )
        val tellElement = myFixture.file.findElementAt(0)
        assertNotNull(tellElement)
        assertNull("tell keyword must return null usage type", usageTypeProvider.getUsageType(tellElement!!))
    }

    fun testDetectorReadWriteAccessibleForReferenceElement() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            tell application "Finder"
                set x to name of folder 1
            end tell
            """.trimIndent(),
        )
        val nameRef = myFixture.file.findElementAt(myFixture.editor.document.text.indexOf("name"))
        assertNotNull(nameRef)
        val parent = nameRef!!.parent
        assertNotNull("Parent must not be null", parent)
        val access = detector.getExpressionAccess(parent)
        assertNotNull("Access must not be null", access)
    }

    fun testReadWriteAccessDetectorIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "readWriteAccessDetector",
            "com.intellij.plugin.applescript.lang.ide.findUsages.AppleScriptReadWriteAccessDetector",
        )
        assertNotNull("ReadWriteAccessDetector must be registered", element)
    }

    fun testUsageTypeProviderIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "usageTypeProvider",
            "com.intellij.plugin.applescript.lang.ide.findUsages.AppleScriptUsageTypeProvider",
        )
        assertNotNull("UsageTypeProvider must be registered", element)
    }

    fun testElementDescriptionProviderIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "elementDescriptionProvider",
            "com.intellij.plugin.applescript.lang.ide.findUsages.AppleScriptElementDescriptionProvider",
        )
        assertNotNull("ElementDescriptionProvider must be registered", element)
    }
}
