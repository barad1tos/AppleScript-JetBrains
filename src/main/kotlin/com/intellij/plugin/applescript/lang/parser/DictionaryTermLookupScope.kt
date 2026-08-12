package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder

internal data class DictionaryTermLookupScope(
    val toldApplicationName: String,
    val areThereUseStatements: Boolean,
    val applicationsToImportFrom: Set<String>?,
) {
    companion object {
        fun of(
            builder: PsiBuilder,
            areThereUseStatements: Boolean = ParserState.areThereUseStatements(builder),
            toldApplicationName: String = ParserApplicationNameStack.getTargetApplicationName(builder),
        ): DictionaryTermLookupScope =
            DictionaryTermLookupScope(
                toldApplicationName,
                areThereUseStatements,
                ParserState.usedApplicationNamesForLookup(builder, areThereUseStatements),
            )
    }
}
