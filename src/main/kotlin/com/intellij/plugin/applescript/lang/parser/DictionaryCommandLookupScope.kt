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
        fun of(builder: PsiBuilder): DictionaryCommandLookupScope {
            val areThereUseStatements = ParserState.areThereUseStatements(builder)
            return DictionaryCommandLookupScope(
                ParserApplicationNameStack.getTargetApplicationName(builder),
                areThereUseStatements,
                ParserState.usedApplicationNamesForLookup(builder, areThereUseStatements),
            )
        }
    }
}
