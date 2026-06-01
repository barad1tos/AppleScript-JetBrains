package com.intellij.plugin.applescript.test.parsing

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

    fun testLibraryPlaylistIndex() = assertNoParserErrors(APP_OBJECT_REF_FIXTURE)

    fun testCurrentTrack() = assertNoParserErrors(APP_OBJECT_REF_FIXTURE)

    fun testTrackOfPlaylist() = assertNoParserErrors(APP_OBJECT_REF_FIXTURE)

    /** D-07: the generic rule parses with no app dictionary loaded (default fixture, cold cache). */
    fun testColdCacheNoDictionary() = assertNoParserErrors(APP_OBJECT_REF_FIXTURE)

    private fun assertNoParserErrors(fileName: String) {
        val psiFile: PsiFile = myFixture.configureByFile(fileName)
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        if (errors.isEmpty()) return
        val text = psiFile.text
        val report = errors.joinToString("\n") { err ->
            val offset = err.textRange.startOffset
            val line = text.substring(0, offset).count { it == '\n' } + 1
            val snippet = err.text.replace("\n", "\\n").take(40)
            "  line $line offset $offset: '$snippet' — ${err.errorDescription}"
        }
        fail("$fileName has ${errors.size} parser error(s):\n$report")
    }

    companion object {
        private const val CORPUS_DIR = "src/test/resources/testData/parse/realWorld"
        private const val APP_OBJECT_REF_FIXTURE = "app_object_ref_min.applescript"
    }
}
