package com.intellij.plugin.applescript.lang.dictionary.discovery

import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo

/**
 * Phase 4 SERVICE-09 (plan 04-04 / D-05): sealed return type for
 * [com.intellij.plugin.applescript.lang.dictionary.files.SdefFileProvider.fetch].
 *
 * Three variants give call sites compile-time exhaustive `when` and a typed channel for
 * each fetch outcome:
 *  - [Empty]   the application has no dictionary (legitimate state — e.g. non-scriptable
 *              app, or a name the discovery sweep could not resolve to a bundle file).
 *  - [Loaded]  dictionary file was generated / cached AND a [DictionaryInfo] was populated.
 *  - [Failed]  a recoverable error occurred during the load (sdef CLI missing, file
 *              permissions, malformed bundle, etc.). The `cause` carries the underlying
 *              exception when available.
 *
 * Service-INTERNAL scope only — this type does NOT appear on any `@JvmStatic` parser-util method
 * and is NOT exposed on the parser hot path. PSI-side sealing for resolver / completion shapes is
 * deferred until that surface is refactored.
 */
sealed interface DictionaryLoadResult {
    /** No dictionary available for the requested application. */
    object Empty : DictionaryLoadResult

    /** Dictionary successfully generated / cached and registered with persistence. */
    data class Loaded(
        val info: DictionaryInfo,
    ) : DictionaryLoadResult

    /**
     * Dictionary fetch failed for [applicationName] with the given [reason]. [cause] is
     * non-null when the failure originated from an exception (sdef CLI missing,
     * NotScriptableApplicationException, IO error); null when the failure is a value-level
     * predicate (e.g. unsupported file extension, off-EDT guard tripped).
     */
    data class Failed(
        val applicationName: String,
        val reason: String,
        val cause: Throwable? = null,
    ) : DictionaryLoadResult
}
