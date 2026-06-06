package com.intellij.plugin.applescript.test.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Differential corpus scan: parses every `.applescript` under the directory named by the
 * `APPLESCRIPT_CORPUS_DIR` env var (or `corpus.dir` system property) and reports how many parse
 * with zero PsiErrorElement nodes, categorizing the rest by normalized error signature.
 *
 * The directory is expected to hold sources pre-classified as VALID_HERE by `osacompile` (see
 * scripts/corpus/classify.sh) — i.e. the ground-truth Apple compiler accepts them on this machine,
 * so any plugin parser error on them is a genuine false positive, not a missing-dictionary case.
 *
 * Skips silently when the env/property is unset, so it never runs in the normal suite — it is a
 * local/CI measurement tool, invoked explicitly. A machine-readable report is written next to the
 * corpus dir as `differential-report.txt`.
 */
class CorpusDifferentialTest : BasePlatformTestCase() {
    fun testCorpusFalsePositiveCoverage() {
        val dir = corpusDir() ?: return
        val scripts =
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "applescript" }
                .sortedBy { it.path }
                .toList()
        if (scripts.isEmpty()) return

        var clean = 0
        var filesWithErrors = 0
        val findings = mutableListOf<Finding>()
        scripts.forEachIndexed { index, script ->
            val psi: PsiFile = myFixture.configureByText("corpus_probe_$index.applescript", script.readText())
            val text = psi.text
            val errors = PsiTreeUtil.findChildrenOfType(psi, PsiErrorElement::class.java)
            if (errors.isEmpty()) {
                clean++
            } else {
                filesWithErrors++
                errors.forEach { error ->
                    val offset = error.textRange.startOffset
                    val line = text.substring(0, offset).count { it == '\n' } + 1
                    findings += Finding(
                        signature = normalizeSignature(error.errorDescription),
                        file = script.name,
                        line = line,
                        snippet = error.text.replace("\n", "\\n").take(48),
                    )
                }
            }
        }

        val coverage = if (scripts.isEmpty()) 0 else clean * 100 / scripts.size
        val byCategory =
            findings
                .groupBy { it.signature }
                .entries
                .sortedByDescending { it.value.size }
        // Root triggers: the FIRST error in each file. Downstream errors cascade from these, so
        // ranking by root trigger (not by total error count) is the true fix-by-class backlog.
        val rootTriggers =
            findings
                .groupBy { it.file }
                .values
                .mapNotNull { perFile -> perFile.minByOrNull { it.line } }
        val rootByCategory =
            rootTriggers
                .groupBy { it.signature }
                .entries
                .sortedByDescending { it.value.size }

        val report = buildString {
            appendLine("=== AppleScript plugin parser — differential corpus scan ===")
            appendLine("corpus dir : ${dir.absolutePath}")
            appendLine("scripts    : ${scripts.size} (osacompile VALID_HERE)")
            appendLine("clean      : $clean  ($coverage% false-positive-free)")
            appendLine("with errors: $filesWithErrors")
            appendLine()
            appendLine("=== ROOT TRIGGERS — first error per file (the fix-by-class backlog) ===")
            rootByCategory.forEach { (signature, group) ->
                appendLine("[${group.size} files] $signature")
                group.take(3).forEach { example ->
                    appendLine("      e.g. ${example.file}:${example.line}  got: ${example.snippet}")
                }
            }
            appendLine()
            appendLine("=== all errors by category (cascade-inflated; for reference) ===")
            byCategory.forEach { (signature, group) -> appendLine("[${group.size}] $signature") }
        }
        File(dir.parentFile ?: dir, "differential-report.txt").writeText(report)
        println(report)
    }

    private fun corpusDir(): File? {
        val path = System.getenv("APPLESCRIPT_CORPUS_DIR") ?: System.getProperty("corpus.dir")
        return path?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isDirectory }
    }

    /** Collapse quoted literals so `got 'set'` and `got 'width'` cluster into one class. */
    private fun normalizeSignature(description: String?): String =
        (description ?: "<no description>").replace(Regex("'[^']*'"), "'…'")

    private data class Finding(
        val signature: String,
        val file: String,
        val line: Int,
        val snippet: String,
    )
}
