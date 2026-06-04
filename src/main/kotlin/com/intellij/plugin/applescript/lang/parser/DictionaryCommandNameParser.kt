package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary

internal object DictionaryCommandNameParser {
    fun parseName(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
        lookupScope: DictionaryCommandLookupScope,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseDictionaryCommandNameInner")) {
            parsedName.set("")
            ParsableScriptSuiteRegistryHelper.ensureKnownApplicationInitialized(lookupScope.toldApplicationName)
            result = parseScopedName(builder, level, parsedName, lookupScope)
        }
        return result
    }

    fun parseForApplication(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
        applicationName: String,
        shouldCheckStandardLibrary: Boolean,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseCommandNameForApplication")) return false
        var result = false
        parsedName.set("")
        val marker = enter_section_(builder)
        parsedName.set(builder.tokenText ?: "")

        var commandWithPrefixExists =
            ParsableScriptSuiteRegistryHelper.isCommandWithPrefixExist(
                applicationName,
                parsedName.get(),
            )
        var nextTokenText = parsedName.get()
        while (builder.tokenText != null && commandWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            commandWithPrefixExists =
                ParsableScriptSuiteRegistryHelper.isCommandWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
            if (commandWithPrefixExists) {
                parsedName.set(nextTokenText)
            } else if (ParsableScriptSuiteRegistryHelper.isApplicationCommand(applicationName, parsedName.get())) {
                result =
                    !shouldCheckStandardLibrary ||
                    !ParsableScriptSuiteRegistryHelper.isStdCommandWithPrefixExist(nextTokenText)
                result = result &&
                    !ParsableScriptSuiteRegistryHelper.isClassWithPrefixExist(applicationName, nextTokenText)
                break
            }
        }
        exit_section_(builder, marker, null, result)
        return result
    }

    private fun parseScopedName(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
        lookupScope: DictionaryCommandLookupScope,
    ): Boolean =
        parseForApplication(
            builder,
            level + 1,
            parsedName,
            lookupScope.toldApplicationName,
            lookupScope.shouldCheckStandardLibrary,
        ) ||
            parseImportedNames(builder, level, parsedName, lookupScope) ||
            parseDefaultNames(builder, level, parsedName, lookupScope)

    private fun parseImportedNames(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
        lookupScope: DictionaryCommandLookupScope,
    ): Boolean {
        var result = false
        if (lookupScope.areThereUseStatements) {
            for (applicationName in lookupScope.applicationsToImport.orEmpty()) {
                ParsableScriptSuiteRegistryHelper.ensureKnownApplicationInitialized(applicationName)
                result = parseForApplication(builder, level + 1, parsedName, applicationName, false)
                if (result) break
            }
        }
        return result
    }

    private fun parseDefaultNames(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
        lookupScope: DictionaryCommandLookupScope,
    ): Boolean =
        parseStandardLibraryName(builder, level, parsedName, lookupScope.shouldCheckStandardLibrary) ||
            parseForApplication(
                builder,
                level + 1,
                parsedName,
                ApplicationDictionary.COCOA_STANDARD_LIBRARY,
                lookupScope.shouldCheckStandardLibrary,
            ) ||
            FallbackCommandParser.parseCommandName(builder, level + 1, parsedName)

    private fun parseStandardLibraryName(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
        shouldCheckStandardLibrary: Boolean,
    ): Boolean = shouldCheckStandardLibrary && parseStdLibName(builder, level + 1, parsedName)

    private fun parseStdLibName(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseStdLibCommandName")) return false
        var result = false
        parsedName.set("")
        parsedName.set(builder.tokenText ?: "")
        val marker = enter_section_(builder)
        var commandWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdCommandWithPrefixExist(parsedName.get())
        var nextTokenText = parsedName.get()
        while (builder.tokenText != null && commandWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            commandWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdCommandWithPrefixExist(nextTokenText)
            if (commandWithPrefixExists) {
                parsedName.set(nextTokenText)
            } else if (ParsableScriptSuiteRegistryHelper.isStdCommand(parsedName.get())) {
                result = true
                break
            }
        }
        exit_section_(builder, marker, null, result)
        return result
    }
}
