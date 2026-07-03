package com.intellij.plugin.applescript.lang.dictionary.project

import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.io.File

/**
 * Service-level outcome for materializing a project dictionary — from cached sources, from the
 * on-demand registry path, or from an explicitly loaded dictionary file.
 *
 * The public dictionary API remains nullable for existing callers; this type keeps project-cache,
 * registered-cache, generated-cache, on-demand, file-load, stale-fallback, malformed-cache,
 * materialization-failure, miss, and ignore-list states distinguishable inside the service and
 * regression tests.
 */
internal sealed interface DictionaryMaterializationResult {
    val dictionary: ApplicationDictionary?

    data class Created(
        override val dictionary: ApplicationDictionary,
        val source: Source,
    ) : DictionaryMaterializationResult

    data class Cached(
        override val dictionary: ApplicationDictionary,
    ) : DictionaryMaterializationResult

    data class StaleFallback(
        override val dictionary: ApplicationDictionary,
    ) : DictionaryMaterializationResult

    data class ParseFailed(
        val generatedDictionaryFile: File,
        val fallbackDictionary: ApplicationDictionary? = null,
    ) : DictionaryMaterializationResult {
        override val dictionary: ApplicationDictionary?
            get() = fallbackDictionary
    }

    data class MaterializationFailed(
        val generatedDictionaryFile: File,
        val fallbackDictionary: ApplicationDictionary? = null,
    ) : DictionaryMaterializationResult {
        override val dictionary: ApplicationDictionary?
            get() = fallbackDictionary
    }

    data object Ignored : DictionaryMaterializationResult {
        override val dictionary: ApplicationDictionary? = null
    }

    data object Missing : DictionaryMaterializationResult {
        override val dictionary: ApplicationDictionary? = null
    }

    enum class Source {
        RegisteredCache,
        GeneratedCache,
        RegistryInfo,
    }
}
