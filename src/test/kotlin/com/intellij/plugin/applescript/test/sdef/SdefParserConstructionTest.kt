package com.intellij.plugin.applescript.test.sdef

import com.intellij.plugin.applescript.lang.sdef.parser.SdefDictionaryConstructionResult
import com.intellij.plugin.applescript.lang.sdef.parser.SdefParser
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private val SDEF_WITH_MIXED_COMPONENTS =
    """
    <dictionary title="Task List Terminology">
        <documentation>Ignored root metadata</documentation>
        <suite name="Task List Suite" code="task" description="Task list commands">
            <command name="show" code="shwn" description="Show a list"/>
            <command name="make" code="mkni" description="Make a new item"/>
            <class name="list" code="tlst" description="A task list"/>
            <value-type name="color" code="colr" description="A label color"/>
            <class-extension extends="list">
                <property name="archived" code="arch" type="boolean"/>
            </class-extension>
            <record-type name="metadata" code="meta" description="Task metadata">
                <property name="name" code="pnam" type="text"/>
            </record-type>
            <enumeration name="status" code="stat">
                <enumerator name="active" code="actv" description="Active task"/>
            </enumeration>
        </suite>
        <suite name="Notes Suite" code="note" description="Notes commands">
            <command name="archive" code="arch" description="Archive a note"/>
            <class name="note" code="cnot" description="A note"/>
        </suite>
    </dictionary>
    """.trimIndent()

private val SDEF_WITH_DUPLICATES_AND_MALFORMED_SUITE =
    """
    <dictionary title="Duplicate Terminology">
        <documentation>Ignored root metadata</documentation>
        <suite name="Rejected Suite" description="Missing code">
            <command name="skip" code="skip" description="Ignored command"/>
        </suite>
        <suite name="Accepted Suite" code="acpt" description="Accepted commands">
            <command name="show" code="shwn" description="Show a list"/>
            <command name="show" code="shwn" description="Show a list"/>
        </suite>
    </dictionary>
    """.trimIndent()

class SdefParserConstructionTest : BasePlatformTestCase() {
    fun testParseRootTagReportsAndAppliesSuiteConstruction() {
        val dictionary = buildTestDictionary(project)
        val rootTag = requireNotNull(buildTestXmlFile(project, SDEF_WITH_MIXED_COMPONENTS).rootTag)

        val result: SdefDictionaryConstructionResult = SdefParser.parseRootTag(dictionary, rootTag)

        assertEquals(
            SdefDictionaryConstructionResult(
                suiteRegistrationAttempts = 2,
                commandRegistrationAttempts = 3,
                classRegistrationAttempts = 4,
                recordRegistrationAttempts = 1,
                enumerationRegistrationAttempts = 1,
            ),
            result,
        )
        val suite = requireNotNull(dictionary.findSuiteByName("Task List Suite"))
        assertNotNull(dictionary.findCommand("show"))
        assertNotNull(suite.findCommandByCode("shwn"))
        assertNull(suite.findCommandByCode("show"))
        assertNotNull(dictionary.findClass("list"))
        assertNotNull(suite.getClassByName("list"))
        assertNotNull(suite.findClassByCode("tlst"))
        assertNull(suite.getClassByName("tlst"))
        assertNotNull(dictionary.findClass("color"))
        assertNotNull(suite.getClassByName("color"))
        assertNotNull(dictionary.dictionaryRecordMap["metadata"])
        assertNotNull(dictionary.findEnumeration("status"))
        assertNotNull(dictionary.findEnumerator("active"))
        assertNotNull(dictionary.findSuiteByName("Notes Suite"))
        assertNotNull(dictionary.findCommand("archive"))
        assertNotNull(dictionary.findClass("note"))
    }

    fun testParseRootTagReportsConstructionAttemptsForAcceptedSuiteTags() {
        val dictionary = buildTestDictionary(project)
        val rootTag = requireNotNull(buildTestXmlFile(project, SDEF_WITH_DUPLICATES_AND_MALFORMED_SUITE).rootTag)

        val result: SdefDictionaryConstructionResult = SdefParser.parseRootTag(dictionary, rootTag)

        assertEquals(
            SdefDictionaryConstructionResult(
                suiteRegistrationAttempts = 1,
                commandRegistrationAttempts = 2,
            ),
            result,
        )
        assertNull(dictionary.findSuiteByName("Rejected Suite"))
        assertNotNull(dictionary.findSuiteByName("Accepted Suite"))
        assertEquals(1, dictionary.findAllCommandsWithName("show").size)
    }
}
