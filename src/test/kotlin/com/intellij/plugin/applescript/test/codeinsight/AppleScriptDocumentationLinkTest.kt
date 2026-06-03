package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.lang.ide.AppleScriptDocHelper
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptDocumentationLinkTest : BasePlatformTestCase() {
    fun testDictionaryLinkWithoutSuiteDoesNotThrow() {
        val element =
            AppleScriptDocHelper.getDocumentationElementForLink(
                PsiManager.getInstance(project),
                "dictionary#Finder.xml",
            )

        assertNull("Unloaded dictionary links should resolve to null without throwing", element)
    }
}
