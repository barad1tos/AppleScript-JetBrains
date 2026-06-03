package com.intellij.plugin.applescript.test.parsing

import com.intellij.plugin.applescript.psi.AppleScriptApplicationObjectReference
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 8 (v2.0) PARSE-03: generic application-object references — `library playlist N`,
 * `current track`, `track N of library playlist M` — must parse as expressions with a
 * queryable PSI node for ANY scriptable app, with zero `PsiErrorElement` and WITHOUT a
 * loaded application dictionary (D-07: the generic rule is syntactic, not dictionary-resolved).
 *
 * All assertions run against `app_object_ref_min.applescript`, a minimal top-level fixture
 * (no tell block, no dictionary). RED before the 08-06 `application_object_reference` rule
 * lands; GREEN after the rule + PSI mixin + gen regen.
 *
 * Per the 08-06 single-bareword checkpoint (defer-folder-half), the standalone `folder 1`
 * index case is owned by 08-07 and is intentionally NOT covered here; `testTracksWhose`
 * therefore stays partially red until 08-07.
 *
 * Heavy-by-default suite (BasePlatformTestCase boots a ~30s fixture); opt OUT with
 * `-PskipHeavyTests=true`. Mirrors the RealWorldCorpusTest error-count harness verbatim.
 */
class ApplicationObjectReferenceTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = File(CORPUS_DIR).absolutePath

    fun testLibraryPlaylistIndex() = assertNoParserErrors()

    fun testCurrentTrack() = assertNoParserErrors()

    fun testTrackOfPlaylist() = assertNoParserErrors()

    /** D-07: the generic rule parses with no app dictionary loaded (default fixture, cold cache). */
    fun testColdCacheNoDictionary() = assertNoParserErrors()

    fun testGeneratedAccessorsExposeApplicationObjectReferenceTerms() {
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

        val objectTermTexts = references.mapNotNull { it.varIdentifier?.text }
        val integerLiteralTexts = references.mapNotNull { it.integerLiteralExpression?.text }

        assertTrue(
            "application object references should expose their object term token",
            objectTermTexts.isNotEmpty(),
        )
        assertTrue(
            "application object reference integer terms should be valid when present",
            integerLiteralTexts.all { it.isNotBlank() },
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
