package com.intellij.plugin.applescript.test.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.parser.GeneratedParserUtilBase.TRUE_CONDITION
import com.intellij.lang.parser.GeneratedParserUtilBase._COLLAPSE_
import com.intellij.lang.parser.GeneratedParserUtilBase.adapt_builder_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.lang.parser.AppleScriptParser
import com.intellij.plugin.applescript.lang.parser.AppleScriptParserDefinition
import com.intellij.plugin.applescript.psi.AppleScriptApplicationObjectReference
import com.intellij.plugin.applescript.psi.AppleScriptExpression
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 8 (v2.0) PARSE-03: generic application-object references should parse common
 * application-object forms with zero `PsiErrorElement` and without a loaded application dictionary.
 *
 * Specific dictionary-like references such as `library playlist N` may be owned by the richer
 * referenceForm path. The generic `application_object_reference` rule is reserved for fallback
 * syntactic forms that do not have a better PSI shape, such as `current track` and `track id <expr>`.
 *
 * All assertions run against `app_object_ref_min.applescript`, a minimal top-level fixture
 * (no tell block, no dictionary).
 */
class ApplicationObjectReferenceTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(CORPUS_DIR).absolutePath

    fun testLibraryPlaylistIndex() = assertNoParserErrors()

    fun testCurrentTrack() = assertNoParserErrors()

    fun testTrackOfPlaylist() = assertNoParserErrors()

    fun testTrackByIdReference() = assertNoParserErrors()

    /** D-07: the generic rule parses with no app dictionary loaded (default fixture, cold cache). */
    fun testColdCacheNoDictionary() = assertNoParserErrors()

    fun testCurrentAndLiteralSelectorReferencesKeepApplicationObjectReferences() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                set currentTrack to current track
                set indexedTrack to track 4
                set namedTrack to track "Night Shift"
                set identifiedTrack to track id trackIdentifier
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile, "inline application-object selector fixture")

        val objectReferenceTexts = applicationObjectReferenceTexts(psiFile)
        assertTrue(
            "current-object fallback must retain `current track`: $objectReferenceTexts",
            objectReferenceTexts.contains("current track"),
        )
        assertEquals("track 4", parseApplicationObjectReference("track 4").text)
        assertEquals("track \"Night Shift\"", parseApplicationObjectReference("track \"Night Shift\"").text)
        assertTrue(
            "id selector fallback must retain variable-valued identifiers: $objectReferenceTexts",
            objectReferenceTexts.contains("track id trackIdentifier"),
        )
    }

    fun testBareMultiWordReferenceStopsAtStatementBoundary() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                set selectedRuleSet to rule set
                set done to true
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile, "inline multi-word reference boundary fixture")

        val objectReferenceTexts = applicationObjectReferenceTexts(psiFile)
        assertTrue(
            "bare multi-word object references should stop before the next statement: $objectReferenceTexts",
            objectReferenceTexts.contains("rule set"),
        )
        assertFalse(
            "application object reference must not swallow the next assignment: $objectReferenceTexts",
            objectReferenceTexts.any { referenceText -> "set done" in referenceText },
        )
    }

    fun testGenericApplicationObjectReferencesKeepFallbackTerms() {
        val psiFile = myFixture.configureByFile(APP_OBJECT_REF_FIXTURE)
        val references =
            PsiTreeUtil.findChildrenOfType(
                psiFile,
                AppleScriptApplicationObjectReference::class.java,
            )

        assertTrue(
            "fixture must contain application object references",
            references.isNotEmpty(),
        )

        val objectReferenceTexts = references.map { it.text }

        assertTrue(
            "application object references should retain current-object text: $objectReferenceTexts",
            objectReferenceTexts.any { it == "current track" },
        )
        assertTrue(
            "application object references should retain id-selector text: $objectReferenceTexts",
            objectReferenceTexts.any { it == "track id trackIdentifier" },
        )

        val trackIdReference = references.first { it.text == "track id trackIdentifier" }
        val structuredSelectorExpressions =
            PsiTreeUtil.findChildrenOfType(trackIdReference, AppleScriptExpression::class.java)
        assertTrue(
            "id-selector reference must expose a structured expression child: $objectReferenceTexts",
            structuredSelectorExpressions.isNotEmpty(),
        )
    }

    private fun assertNoParserErrors() {
        assertNoParserErrors(myFixture.configureByFile(APP_OBJECT_REF_FIXTURE), APP_OBJECT_REF_FIXTURE)
    }

    private fun assertNoParserErrors(
        psiFile: PsiFile,
        fixtureName: String,
    ) {
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        if (errors.isEmpty()) return
        val text = psiFile.text
        val report =
            errors.joinToString("\n") { err ->
                val offset = err.textRange.startOffset
                val line = text.substring(0, offset).count { it == '\n' } + 1
                val snippet = err.text.replace("\n", "\\n").take(40)
                "  line $line offset $offset: '$snippet' — ${err.errorDescription}"
            }
        fail("$fixtureName has ${errors.size} parser error(s):\n$report")
    }

    private fun applicationObjectReferenceTexts(psiFile: PsiFile): List<String> {
        val references =
            PsiTreeUtil.findChildrenOfType(
                psiFile,
                AppleScriptApplicationObjectReference::class.java,
            )
        return references.map { it.text }
    }

    private fun parseApplicationObjectReference(text: String): ASTNode {
        val parserDefinition = AppleScriptParserDefinition()
        val anchorFile = myFixture.configureByText(AppleScriptFileType, "")
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(
                project,
                anchorFile.node,
                parserDefinition.createLexer(project),
                AppleScriptLanguage,
                text,
            )
        val adaptedBuilder =
            adapt_builder_(
                parserDefinition.fileNodeType,
                builder,
                AppleScriptParser(),
                AppleScriptParser.EXTENDS_SETS_,
            )
        val marker = enter_section_(adaptedBuilder, 0, _COLLAPSE_, null)
        val result = AppleScriptParser.application_object_reference(adaptedBuilder, 0)
        assertTrue("application object reference parser should consume `$text`", adaptedBuilder.eof())
        exit_section_(
            adaptedBuilder,
            0,
            marker,
            parserDefinition.fileNodeType,
            result,
            true,
            TRUE_CONDITION,
        )
        assertTrue("application object reference parser should accept `$text`", result)
        return adaptedBuilder.treeBuilt
    }

    companion object {
        private const val CORPUS_DIR = "src/test/resources/testData/parse/realWorld"
        private const val APP_OBJECT_REF_FIXTURE = "app_object_ref_min.applescript"
    }
}
