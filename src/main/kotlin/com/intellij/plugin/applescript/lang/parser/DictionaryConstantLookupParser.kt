package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary

internal object DictionaryConstantLookupParser {
    fun parseConstant(
        builder: PsiBuilder,
        level: Int,
        insideExpression: Boolean,
        lookupScope: DictionaryTermLookupScope,
    ): Boolean =
        tryToParseApplicationConstant(builder, level + 1, lookupScope.toldApplicationName) ||
            parseImportedOrDefaultConstants(builder, level, insideExpression, lookupScope)

    private fun parseImportedOrDefaultConstants(
        builder: PsiBuilder,
        level: Int,
        insideExpression: Boolean,
        lookupScope: DictionaryTermLookupScope,
    ): Boolean =
        if (lookupScope.areThereUseStatements && insideExpression) {
            parseImportedApplicationConstants(builder, level, lookupScope.applicationsToImportFrom.orEmpty())
        } else {
            parseDefaultConstants(builder, level)
        }

    private fun parseImportedApplicationConstants(
        builder: PsiBuilder,
        level: Int,
        applicationsToImport: Set<String>,
    ): Boolean {
        var result = false
        for (applicationName in applicationsToImport) {
            result = tryToParseApplicationConstant(builder, level + 1, applicationName)
            if (result) break
        }
        return result
    }

    private fun parseDefaultConstants(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        tryToParseStdConstant(builder, level + 1) ||
            tryToParseApplicationConstant(
                builder,
                level + 1,
                ApplicationDictionary.COCOA_STANDARD_LIBRARY,
            )

    private fun tryToParseStdConstant(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "tryToParseStdConstant")) return false
        val currentTokenText = Ref<String>()
        currentTokenText.set(builder.tokenText ?: "")
        var result = false
        var propertyOrClassExists = false
        var constantWithPrefixExists =
            DictionaryConstantRegistry.isStdConstantWithPrefixExist(
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        val marker = enter_section_(builder)
        while (builder.tokenText != null && constantWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            constantWithPrefixExists = DictionaryConstantRegistry.isStdConstantWithPrefixExist(nextTokenText)
            if (constantWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (DictionaryConstantRegistry.isStdConstant(currentTokenText.get())) {
                result = true
                break
            }
        }
        if (result) {
            currentTokenText.set(currentTokenText.get() + " " + builder.tokenText)
            propertyOrClassExists = DictionaryPropertyRegistry.isStdPropertyWithPrefixExist(
                currentTokenText.get(),
            ) ||
                DictionaryClassRegistry.isStdClassWithPrefixExist(currentTokenText.get())
        }
        result = result && !propertyOrClassExists
        exit_section_(builder, marker, null, result)
        return result
    }

    private fun tryToParseApplicationConstant(
        builder: PsiBuilder,
        level: Int,
        applicationName: String,
    ): Boolean {
        if (!recursion_guard_(builder, level, "tryToParseApplicationConstant")) return false
        val currentTokenText = Ref<String>()
        currentTokenText.set(builder.tokenText ?: "")
        var result = false
        var propertyOrClassExists = false
        var constantWithPrefixExists =
            DictionaryConstantRegistry.isConstantWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        val marker = enter_section_(builder)
        while (builder.tokenText != null && constantWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            constantWithPrefixExists =
                DictionaryConstantRegistry.isConstantWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
            if (constantWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (DictionaryConstantRegistry.isApplicationConstant(
                    applicationName,
                    currentTokenText.get(),
                )
            ) {
                result = true
                break
            }
        }
        if (result) {
            propertyOrClassExists = DictionaryPropertyRegistry.isPropertyWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            ) ||
                DictionaryClassRegistry.isClassWithPrefixExist(applicationName, currentTokenText.get())
            if (propertyOrClassExists) {
                currentTokenText.set(currentTokenText.get() + " " + builder.tokenText)
                propertyOrClassExists = DictionaryPropertyRegistry.isPropertyWithPrefixExist(
                    applicationName,
                    currentTokenText.get(),
                ) ||
                    DictionaryClassRegistry.isClassWithPrefixExist(applicationName, currentTokenText.get())
            }
        }
        result = result && !propertyOrClassExists
        exit_section_(builder, marker, null, result)
        return result
    }
}
