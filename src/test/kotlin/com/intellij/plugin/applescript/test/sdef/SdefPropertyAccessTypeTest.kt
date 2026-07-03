package com.intellij.plugin.applescript.test.sdef

import com.intellij.plugin.applescript.lang.sdef.AccessType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private val ACCESS_DICTIONARY_XML =
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
    """.trimIndent()

class SdefPropertyAccessTypeTest : BasePlatformTestCase() {
    fun testParserPreservesReadWriteAndWriteOnlyPropertyAccess() {
        val dictionary =
            buildTestDictionary(
                project = project,
                xmlText = ACCESS_DICTIONARY_XML,
                applicationName = "AccessTest",
                fileName = "access.sdef",
            )

        assertEquals(AccessType.R, dictionary.findProperty("read only")?.accessType)
        assertEquals(AccessType.W, dictionary.findProperty("write only")?.accessType)
        assertEquals(AccessType.RW, dictionary.findProperty("read write")?.accessType)
        assertEquals(AccessType.RW, dictionary.findProperty("default access")?.accessType)
    }
}
