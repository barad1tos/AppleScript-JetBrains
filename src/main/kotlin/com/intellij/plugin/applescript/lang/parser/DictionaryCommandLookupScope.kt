package com.intellij.plugin.applescript.lang.parser

import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary

internal data class DictionaryCommandLookupScope(
    val toldApplicationName: String,
    val areThereUseStatements: Boolean,
    val applicationsToImport: Set<String>?,
) {
    val shouldCheckStandardLibrary: Boolean =
        !areThereUseStatements ||
            applicationsToImport == null ||
            applicationsToImport.contains(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
}
