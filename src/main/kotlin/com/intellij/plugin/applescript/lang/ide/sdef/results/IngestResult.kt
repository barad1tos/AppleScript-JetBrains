package com.intellij.plugin.applescript.lang.ide.sdef.results

/**
 * Result of ingesting a dictionary file into the dictionary index.
 *
 * Exhaustive `when` usage is enforced at internal call sites by the sealed interface.
 */
sealed interface IngestResult {
    /** All requested suites were parsed and indexed successfully. */
    data class Success(
        val suitesIngested: Int,
        val commandsIndexed: Int,
    ) : IngestResult

    /** Some suites were ingested; others were skipped (file missing, parse error). */
    data class Partial(
        val suitesIngested: Int,
        val skipped: List<String>,
    ) : IngestResult

    /** Ingestion failed entirely (e.g. the source file did not parse). */
    data class Failed(
        val reason: String,
        val cause: Throwable? = null,
    ) : IngestResult
}
