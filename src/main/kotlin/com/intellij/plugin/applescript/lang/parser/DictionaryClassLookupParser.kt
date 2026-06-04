package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary

internal object DictionaryClassLookupParser {
    private enum class ClassLookupResult {
        Match,
        AmbiguousProperty,
        Missing,
    }

    fun parseClassName(
        builder: PsiBuilder,
        level: Int,
        isPluralForm: Boolean,
        lookupScope: DictionaryTermLookupScope,
    ): Boolean {
        val currentTokenText = Ref<String>()
        currentTokenText.set(builder.tokenText ?: "")
        val dictionaryResult = parseDictionaryClassName(builder, currentTokenText, isPluralForm, lookupScope)
        return dictionaryResult == ClassLookupResult.Match ||
            FallbackDictionaryTermParser.parseClassName(builder, level + 1)
    }

    private fun parseDictionaryClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        isPluralForm: Boolean,
        lookupScope: DictionaryTermLookupScope,
    ): ClassLookupResult {
        val applicationResult =
            parseApplicationClassNameChecked(
                builder,
                currentTokenText,
                isPluralForm,
                lookupScope.toldApplicationName,
                lookupScope.toldApplicationName,
            )
        val scopedResult =
            if (applicationResult == ClassLookupResult.Missing) {
                parseScopedClassName(builder, currentTokenText, isPluralForm, lookupScope)
            } else {
                applicationResult
            }
        return if (scopedResult == ClassLookupResult.Missing) {
            parseCocoaClassName(builder, currentTokenText, isPluralForm, lookupScope.toldApplicationName)
        } else {
            scopedResult
        }
    }

    private fun parseScopedClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        isPluralForm: Boolean,
        lookupScope: DictionaryTermLookupScope,
    ): ClassLookupResult =
        if (lookupScope.areThereUseStatements) {
            parseImportedClassName(
                builder,
                currentTokenText,
                isPluralForm,
                lookupScope.applicationsToImportFrom.orEmpty(),
            )
        } else if (parseStdClassNameChecked(builder, currentTokenText, isPluralForm)) {
            ClassLookupResult.Match
        } else {
            ClassLookupResult.Missing
        }

    private fun parseImportedClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        isPluralForm: Boolean,
        applicationsToImport: Set<String>,
    ): ClassLookupResult {
        var result = ClassLookupResult.Missing
        for (applicationName in applicationsToImport) {
            result =
                parseApplicationClassNameChecked(
                    builder,
                    currentTokenText,
                    isPluralForm,
                    applicationName,
                    applicationName,
                )
            if (result != ClassLookupResult.Missing) break
        }
        return result
    }

    private fun parseCocoaClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        isPluralForm: Boolean,
        toldApplicationName: String,
    ): ClassLookupResult =
        parseApplicationClassNameChecked(
            builder,
            currentTokenText,
            isPluralForm,
            ApplicationDictionary.COCOA_STANDARD_LIBRARY,
            toldApplicationName,
        )

    private fun parseApplicationClassNameChecked(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        isPluralForm: Boolean,
        classApplicationName: String,
        propertyApplicationName: String,
    ): ClassLookupResult {
        val marker = enter_section_(builder)
        val result =
            DictionaryClassNameScanner.parseApplicationClassName(
                builder,
                currentTokenText,
                isPluralForm,
                classApplicationName,
            )
        var propertyExists = false
        if (result) {
            currentTokenText.set(currentTokenText.get() + " " + builder.tokenText)
            propertyExists =
                ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
                    propertyApplicationName,
                    currentTokenText.get(),
                )
        }
        exit_section_(builder, marker, null, result && !propertyExists)
        return when {
            propertyExists -> ClassLookupResult.AmbiguousProperty
            result -> ClassLookupResult.Match
            else -> ClassLookupResult.Missing
        }
    }

    private fun parseStdClassNameChecked(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        isPluralForm: Boolean,
    ): Boolean {
        val marker = enter_section_(builder)
        val result = DictionaryClassNameScanner.parseStdClassName(builder, currentTokenText, isPluralForm)
        exit_section_(builder, marker, null, result)
        return result
    }
}
