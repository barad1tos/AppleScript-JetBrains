package com.intellij.plugin.applescript.test.sdef

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.DictionaryTermCatalog
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DictionaryTermCatalogTest : BasePlatformTestCase() {
    fun testPropertyMatchesByKind() {
        val imported = buildTestDictionary(project, IMPORTED_TERMS, "Calendar Plus", "calendar-plus.sdef")
        val cocoa = buildTestDictionary(project, COCOA_TERMS, "Standard Terminology", "cocoa-standard.sdef")

        val propertyNames =
            DictionaryTermCatalog
                .missingTerms(imported, cocoa)
                .filterIsInstance<AppleScriptPropertyDefinition>()
                .map { property -> property.getName() }

        assertEquals(listOf("shared label"), propertyNames)
    }

    fun testAllTermsKeepOrder() {
        val dictionary = buildTestDictionary(project, ORDERED_TERMS, "Calendar Plus", "ordered-terms.sdef")

        val termNames = DictionaryTermCatalog.allTerms(dictionary).map { term -> term.getName() }

        assertEquals(listOf("accepted", "summary", "event", "create event"), termNames)
    }

    fun testFilteredOrder() {
        val dictionary = buildTestDictionary(project, ORDERED_TERMS, "Calendar Plus", "ordered-terms.sdef")
        val emptyBaseline = buildTestDictionary(project, EMPTY_TERMS, "Standard Terminology", "empty-cocoa.sdef")

        val termNames = DictionaryTermCatalog.missingTerms(dictionary, emptyBaseline).map { term -> term.getName() }

        assertEquals(listOf("accepted", "event", "create event", "summary"), termNames)
    }

    fun testCompletionKeepsProperty() {
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)
        try {
            cacheCompletionDictionaries(projectDictionaries)
            myFixture.configureByText(
                AppleScriptFileType,
                """
                use application "Calendar Primer"
                use application "Calendar Plus"
                set selectedValue to <caret>
                """.trimIndent(),
            )

            myFixture.completeBasic()
            val lookupStrings = requireNotNull(myFixture.lookupElementStrings)

            assertTrue("Imported property must remain available", lookupStrings.contains("shared label"))
            assertFalse("Cocoa property must be filtered", lookupStrings.contains("standard label"))
        } finally {
            projectDictionaries.clearCachedDictionariesForTests()
        }
    }

    fun testCocoaRequired() {
        val dictionary = buildTestDictionary(project, IMPORTED_TERMS, "Calendar Plus", "calendar-plus.sdef")

        val terms = DictionaryTermCatalog.missingTerms(dictionary, null)

        assertEmpty("Filtered terms require a Cocoa baseline", terms)
    }

    fun testNoUseFallbacks() {
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)

        try {
            projectDictionaries.clearCachedDictionariesForTests()
            projectDictionaries.cacheDictionaryForTests(
                ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY,
                buildTestDictionary(
                    project,
                    ADDITIONS_TERMS,
                    ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY,
                    "additions.sdef",
                ),
            )
            projectDictionaries.cacheDictionaryForTests(
                ApplicationDictionary.COCOA_STANDARD_LIBRARY,
                buildTestDictionary(
                    project,
                    COCOA_TERMS,
                    ApplicationDictionary.COCOA_STANDARD_LIBRARY,
                    "cocoa.sdef",
                ),
            )
            myFixture.configureByText(AppleScriptFileType, "set selectedValue to <caret>")

            myFixture.completeBasic()
            val lookupStrings = requireNotNull(myFixture.lookupElementStrings)

            assertTrue(
                "No-use completion must include Scripting Additions",
                lookupStrings.contains("addition fallback"),
            )
            assertTrue(
                "No-use completion must include Cocoa terms",
                lookupStrings.contains("standard label"),
            )
        } finally {
            projectDictionaries.clearCachedDictionariesForTests()
        }
    }

    private fun cacheCompletionDictionaries(projectDictionaries: AppleScriptProjectDictionaryService) {
        projectDictionaries.clearCachedDictionariesForTests()
        projectDictionaries.cacheDictionaryForTests(
            "Calendar Primer",
            buildTestDictionary(project, PRIMER_TERMS, "Calendar Primer", "calendar-primer.sdef"),
        )
        projectDictionaries.cacheDictionaryForTests(
            "Calendar Plus",
            buildTestDictionary(project, IMPORTED_TERMS, "Calendar Plus", "calendar-plus.sdef"),
        )
        projectDictionaries.cacheDictionaryForTests(
            ApplicationDictionary.COCOA_STANDARD_LIBRARY,
            buildTestDictionary(project, COCOA_TERMS, ApplicationDictionary.COCOA_STANDARD_LIBRARY, "cocoa.sdef"),
        )
    }

    companion object {
        private val IMPORTED_TERMS =
            """
            <dictionary title="Calendar Plus Terminology">
                <suite name="Calendar Plus Suite" code="calp">
                    <class name="event" code="cevt">
                        <property name="shared label" code="pshl" type="text"/>
                        <property name="standard label" code="pstl" type="text"/>
                    </class>
                </suite>
            </dictionary>
            """.trimIndent()

        private val PRIMER_TERMS =
            """
            <dictionary title="Calendar Primer Terminology">
                <suite name="Calendar Primer Suite" code="calr">
                    <class name="calendar source" code="csrc"/>
                </suite>
            </dictionary>
            """.trimIndent()

        private val COCOA_TERMS =
            """
            <dictionary title="Standard Terminology">
                <suite name="Standard Suite" code="cstd">
                    <command name="shared label" code="shlb"/>
                    <class name="application" code="capp">
                        <property name="standard label" code="stlb" type="text"/>
                    </class>
                </suite>
            </dictionary>
            """.trimIndent()

        private val ORDERED_TERMS =
            """
            <dictionary title="Calendar Plus Terminology">
                <suite name="Calendar Plus Suite" code="calp">
                    <command name="create event" code="crev"/>
                    <class name="event" code="cevt">
                        <property name="summary" code="psmm" type="text"/>
                    </class>
                    <enumeration name="participation status" code="psts">
                        <enumerator name="accepted" code="acpt"/>
                    </enumeration>
                </suite>
            </dictionary>
            """.trimIndent()

        private val EMPTY_TERMS =
            """
            <dictionary title="Empty Terminology">
                <suite name="Empty Suite" code="empt"/>
            </dictionary>
            """.trimIndent()

        private val ADDITIONS_TERMS =
            """
            <dictionary title="Scripting Additions Terminology">
                <suite name="Scripting Additions Suite" code="ascr">
                    <command name="addition fallback" code="adfb"/>
                </suite>
            </dictionary>
            """.trimIndent()
    }
}
