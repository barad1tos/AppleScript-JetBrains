package com.intellij.plugin.applescript.test.parsing

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
        val psiFile: PsiFile = myFixture.configureByFile(APP_OBJECT_REF_FIXTURE)
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
        fail("$APP_OBJECT_REF_FIXTURE has ${errors.size} parser error(s):\n$report")
    }

    companion object {
        private const val CORPUS_DIR = "src/test/resources/testData/parse/realWorld"
        private const val APP_OBJECT_REF_FIXTURE = "app_object_ref_min.applescript"
    }
}
