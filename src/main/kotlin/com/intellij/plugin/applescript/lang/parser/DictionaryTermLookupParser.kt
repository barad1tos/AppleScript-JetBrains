package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder

internal data class DictionaryTermLookupScope(
    val toldApplicationName: String,
    val areThereUseStatements: Boolean,
    val applicationsToImportFrom: Set<String>?,
)

internal object DictionaryTermLookupParser {
    fun parsePropertyName(
        builder: PsiBuilder,
        level: Int,
        lookupScope: DictionaryTermLookupScope,
    ): Boolean = DictionaryPropertyLookupParser.parsePropertyName(builder, level, lookupScope)

    fun parseClassName(
        builder: PsiBuilder,
        level: Int,
        isPluralForm: Boolean,
        lookupScope: DictionaryTermLookupScope,
    ): Boolean = DictionaryClassLookupParser.parseClassName(builder, level, isPluralForm, lookupScope)

    fun parseConstant(
        builder: PsiBuilder,
        level: Int,
        insideExpression: Boolean,
        lookupScope: DictionaryTermLookupScope,
    ): Boolean = DictionaryConstantLookupParser.parseConstant(builder, level, insideExpression, lookupScope)
}
