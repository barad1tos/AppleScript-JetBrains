package com.intellij.plugin.applescript.test.sdef

import com.intellij.lang.xml.XMLLanguage
import com.intellij.plugin.applescript.lang.sdef.AccessType
import com.intellij.plugin.applescript.psi.sdef.impl.ApplicationDictionaryImpl
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SdefPropertyAccessTypeTest : BasePlatformTestCase() {
    fun testParserPreservesReadWriteAndWriteOnlyPropertyAccess() {
        val dictionary = buildDictionary()

        assertEquals(AccessType.R, dictionary.findProperty("read only")?.accessType)
        assertEquals(AccessType.W, dictionary.findProperty("write only")?.accessType)
        assertEquals(AccessType.RW, dictionary.findProperty("read write")?.accessType)
        assertEquals(AccessType.RW, dictionary.findProperty("default access")?.accessType)
    }

    private fun buildDictionary(): ApplicationDictionaryImpl {
        val xmlFile =
            PsiFileFactory
                .getInstance(project)
                .createFileFromText(
                    "access.sdef",
                    XMLLanguage.INSTANCE,
                    """
                    <dictionary title="AccessTest">
                        <suite name="Test Suite" code="Test">
                            <class name="item" code="cobj">
                                <property name="read only" code="rdon" type="text" access="r"/>
                                <property name="write only" code="wron" type="text" access="w"/>
                                <property name="read write" code="rwrt" type="text" access="rw"/>
                                <property name="default access" code="dflt" type="text"/>
                            </class>
                        </suite>
                    </dictionary>
                    """.trimIndent(),
                ) as XmlFile
        return ApplicationDictionaryImpl(
            project = project,
            dictionaryXmlFile = xmlFile,
            applicationName = "AccessTest",
            applicationBundleFile = null,
        )
    }
}
