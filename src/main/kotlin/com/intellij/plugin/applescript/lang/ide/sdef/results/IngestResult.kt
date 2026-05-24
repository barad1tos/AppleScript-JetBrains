package com.intellij.plugin.applescript.lang.ide.sdef.results

/**
 * Phase 4 SERVICE-09 (D-05): Sealed return type for [com.intellij.plugin.applescript.lang.ide.sdef.SdefIndexService.ingest].
 *
 * Service-INTERNAL only — not exposed via [com.intellij.plugin.applescript.lang.parser.ParsableScriptHelper]
 * (the parser-util hot path does not see this type — it consumes primitive `Boolean` /
 * `Collection` per the FROZEN_CONTRACT preserved by [ParserUtilContractTest][com.intellij.plugin.applescript.test.parser.ParserUtilContractTest]).
 *
 * Exhaustive-`when` is enforced at every internal call site through the sealed-interface
 * compile-time check.
 */
sealed interface IngestResult {
    /** All requested suites were parsed and indexed successfully. */
    data class Success(val suitesIngested: Int, val commandsIndexed: Int) : IngestResult

    /** Some suites were ingested; others were skipped (file missing, parse error). */
    data class Partial(val suitesIngested: Int, val skipped: List<String>) : IngestResult

    /** Ingestion failed entirely (e.g. the source file did not parse). */
    data class Failed(val reason: String, val cause: Throwable? = null) : IngestResult
}
