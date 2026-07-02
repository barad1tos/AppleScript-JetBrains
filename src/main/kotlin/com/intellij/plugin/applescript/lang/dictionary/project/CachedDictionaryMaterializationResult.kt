package com.intellij.plugin.applescript.lang.dictionary.project

import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.io.File

/**
 * Service-level outcome for materializing a project dictionary from already cached sources.
 *
 * The public dictionary API remains nullable for existing callers; this type keeps project-cache,
 * registered-cache, generated-cache, stale-fallback, malformed-cache, materialization-failure,
 * miss, and ignore-list states distinguishable inside the service and regression tests.
 */
internal sealed interface CachedDictionaryMaterializationResult {
    val dictionary: ApplicationDictionary?

    data class Created(
        override val dictionary: ApplicationDictionary,
        val source: Source,
    ) : CachedDictionaryMaterializationResult

    data class Cached(
        override val dictionary: ApplicationDictionary,
    ) : CachedDictionaryMaterializationResult

    data class StaleFallback(
        override val dictionary: ApplicationDictionary,
    ) : CachedDictionaryMaterializationResult

    data class ParseFailed(
        val generatedDictionaryFile: File,
        val fallbackDictionary: ApplicationDictionary? = null,
    ) : CachedDictionaryMaterializationResult {
        override val dictionary: ApplicationDictionary?
            get() = fallbackDictionary
    }

    data class MaterializationFailed(
        val generatedDictionaryFile: File,
        val fallbackDictionary: ApplicationDictionary? = null,
    ) : CachedDictionaryMaterializationResult {
        override val dictionary: ApplicationDictionary?
            get() = fallbackDictionary
    }

    data object Ignored : CachedDictionaryMaterializationResult {
        override val dictionary: ApplicationDictionary? = null
    }

    data object Missing : CachedDictionaryMaterializationResult {
        override val dictionary: ApplicationDictionary? = null
    }

    enum class Source {
        RegisteredCache,
        GeneratedCache,
    }
}
