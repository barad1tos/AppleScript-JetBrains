package com.intellij.plugin.applescript.lang.dictionary.discovery

import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo

/**
 * Typed result of one application-dictionary load attempt.
 *
 * Three variants give call sites an exhaustive returned outcome:
 *  - [Empty]   no application or supported dictionary input could be resolved.
 *  - [Loaded]  a dictionary was cached, registered, and initialized.
 *  - [Failed]  a load step failed or `fetch` converted an unexpected load error. The `cause`
 *              carries the underlying exception when available.
 */
sealed interface DictionaryLoadResult {
    /** No dictionary available for the requested application. */
    object Empty : DictionaryLoadResult

    /** Dictionary successfully cached, registered, and initialized. */
    data class Loaded(
        val info: DictionaryInfo,
    ) : DictionaryLoadResult

    /**
     * Dictionary fetch failed for [applicationName] with the given [reason]. [cause] is
     * non-null when the failure originated from an exception (sdef CLI missing,
     * NotScriptableApplicationException, IO error); null when the failure is a value-level
     * predicate (e.g. generation or initialization returned no usable output).
     */
    data class Failed(
        val applicationName: String,
        val reason: String,
        val cause: Throwable? = null,
    ) : DictionaryLoadResult
}
