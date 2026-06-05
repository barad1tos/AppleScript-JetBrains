package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary

internal object DictionaryPropertyLookupParser {
    fun parsePropertyName(
        builder: PsiBuilder,
        level: Int,
        lookupScope: DictionaryTermLookupScope,
    ): Boolean =
        tryToParseApplicationProperty(builder, level + 1, lookupScope.toldApplicationName) ||
            parseImportedOrDefaultProperties(builder, level, lookupScope) ||
            FallbackDictionaryTermParser.parseKeywordAsProperty(builder, level + 1) ||
            FallbackDictionaryTermParser.parsePropertyName(builder, level + 1)

    private fun parseImportedOrDefaultProperties(
        builder: PsiBuilder,
        level: Int,
        lookupScope: DictionaryTermLookupScope,
    ): Boolean =
        if (lookupScope.areThereUseStatements) {
            parseImportedApplicationProperties(builder, level, lookupScope.applicationsToImportFrom.orEmpty())
        } else {
            parseDefaultProperties(builder, level)
        }

    private fun parseImportedApplicationProperties(
        builder: PsiBuilder,
        level: Int,
        applicationsToImport: Set<String>,
    ): Boolean {
        var result = false
        for (applicationName in applicationsToImport) {
            result = tryToParseApplicationProperty(builder, level + 1, applicationName)
            if (result) break
        }
        return result
    }

    private fun parseDefaultProperties(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        tryToParseStdProperty(builder, level + 1) ||
            tryToParseApplicationProperty(
                builder,
                level + 1,
                ApplicationDictionary.COCOA_STANDARD_LIBRARY,
            )

    private fun tryToParseStdProperty(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "tryToParseStdProperty")) return false
        val marker = enter_section_(builder)
        val currentTokenText = Ref<String>()
        currentTokenText.set(builder.tokenText ?: "")
        var propertyWithPrefixExists =
            DictionaryPropertyRegistry.isStdPropertyWithPrefixExist(
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && propertyWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            propertyWithPrefixExists = DictionaryPropertyRegistry.isStdPropertyWithPrefixExist(nextTokenText)
            if (propertyWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (DictionaryPropertyRegistry.isStdProperty(currentTokenText.get())) {
                result = true
                break
            }
        }
        exit_section_(builder, marker, null, result)
        return result
    }

    private fun tryToParseApplicationProperty(
        builder: PsiBuilder,
        level: Int,
        applicationName: String,
    ): Boolean {
        if (!recursion_guard_(builder, level, "tryToParseApplicationProperty")) return false
        val marker = enter_section_(builder)
        val currentTokenText = Ref<String>()
        currentTokenText.set(builder.tokenText ?: "")
        var propertyWithPrefixExists =
            DictionaryPropertyRegistry.isPropertyWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && propertyWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            propertyWithPrefixExists =
                DictionaryPropertyRegistry.isPropertyWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
            if (propertyWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (DictionaryPropertyRegistry.isApplicationProperty(
                    applicationName,
                    currentTokenText.get(),
                )
            ) {
                result = true
                break
            }
        }
        exit_section_(builder, marker, null, result)
        return result
    }
}
