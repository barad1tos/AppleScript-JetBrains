package com.intellij.plugin.applescript.lang.dictionary.index

/**
 * Phase 4 SERVICE-09 (D-05): Sealed return type for [SdefIndexService]
 * internal lookups.
 *
 * Service-INTERNAL — parser lookup facades return primitive `Boolean` / `Collection` for the
 * parser hot path; this sealed type is used in SdefIndexService internal callers and unit tests
 * for richer outcome semantics.
 */
sealed interface LookupResult {
    /** Name was found in the requested index. */
    object Hit : LookupResult

    /** Name was not present in the requested index. */
    object Miss : LookupResult

    /**
     * Returned when the relevant dictionary readiness gate is cold. Standard dictionary lookups use
     * `isInitialized`; application dictionary lookups use `areAppDictionariesIndexed`.
     *
     * Callers should treat this as [Miss] with diagnostic context.
     */
    object Stale : LookupResult
}
