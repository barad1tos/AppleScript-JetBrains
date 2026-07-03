package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
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

    companion object {
        // Delegates to the term-scope factory so lookup-scope snapshot semantics have a single
        // source; the two scope types stay separate because their consumers name imports
        // differently (applicationsToImport vs applicationsToImportFrom).
        fun of(builder: PsiBuilder): DictionaryCommandLookupScope {
            val termScope = DictionaryTermLookupScope.of(builder)
            return DictionaryCommandLookupScope(
                termScope.toldApplicationName,
                termScope.areThereUseStatements,
                termScope.applicationsToImportFrom,
            )
        }
    }
}
