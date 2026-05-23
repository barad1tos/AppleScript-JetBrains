package com.intellij.plugin.applescript.lang.sdef

/**
 * Returns true when [extension] (without the leading dot) matches one of the
 * supported AppleScript dictionary file extensions defined in
 * [ApplicationDictionary.SUPPORTED_DICTIONARY_EXTENSIONS]. The check is
 * case-insensitive (`SDEF` and `sdef` both resolve to true).
 *
 * Relocated from `ApplicationDictionaryImpl.Companion.extensionSupported` in
 * v1.1 (D-09) so UI actions no longer reach into a PSI impl just to ask a
 * string-membership question. No `@JvmStatic` needed — all three callers are
 * Kotlin (`LoadDictionaryAction`, two sites in
 * `AppleScriptSystemDictionaryRegistryService`).
 */
fun extensionSupported(extension: String?): Boolean =
    extension != null && ApplicationDictionary.SUPPORTED_DICTIONARY_EXTENSIONS.contains(extension.lowercase())
