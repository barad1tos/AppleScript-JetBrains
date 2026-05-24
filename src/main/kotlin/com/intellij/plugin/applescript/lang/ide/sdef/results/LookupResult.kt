package com.intellij.plugin.applescript.lang.ide.sdef.results

/**
 * Phase 4 SERVICE-09 (D-05): Sealed return type for [com.intellij.plugin.applescript.lang.ide.sdef.SdefIndexService]
 * internal lookups.
 *
 * Service-INTERNAL — the public [com.intellij.plugin.applescript.lang.parser.ParsableScriptHelper]
 * methods on the facade return primitive `Boolean` / `Collection` per the frozen contract; this
 * sealed type is used in SdefIndexService internal callers and unit tests for richer outcome
 * semantics.
 */
sealed interface LookupResult {
    /** Name was found in the requested index. */
    object Hit : LookupResult

    /** Name was not present in the requested index. */
    object Miss : LookupResult

    /**
     * Returned when [com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService.isInitialized]
     * is false — caller should treat as [Miss] but with diagnostic context (init has not completed yet).
     */
    object Stale : LookupResult
}
