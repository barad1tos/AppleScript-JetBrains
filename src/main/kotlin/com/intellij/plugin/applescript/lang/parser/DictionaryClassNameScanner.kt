package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.openapi.util.Ref

internal object DictionaryClassNameScanner {
    fun parseStdClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        isPluralForm: Boolean,
    ): Boolean {
        currentTokenText.set(builder.tokenText ?: "")
        return if (isPluralForm) {
            parseStdPluralClassName(builder, currentTokenText)
        } else {
            parseStdSingularClassName(builder, currentTokenText)
        }
    }

    fun parseApplicationClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        isPluralForm: Boolean,
        applicationName: String,
    ): Boolean {
        currentTokenText.set(builder.tokenText ?: "")
        return if (isPluralForm) {
            parseApplicationPluralClassName(builder, currentTokenText, applicationName)
        } else {
            parseApplicationSingularClassName(builder, currentTokenText, applicationName)
        }
    }

    private fun parseStdPluralClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
    ): Boolean {
        var classWithPrefixExists =
            ParsableScriptSuiteRegistryHelper.isStdClassPluralWithPrefixExist(
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && classWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdClassPluralWithPrefixExist(nextTokenText)
            if (classWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (ParsableScriptSuiteRegistryHelper.isStdLibClassPluralName(currentTokenText.get())) {
                result = true
                break
            }
        }
        return result
    }

    private fun parseStdSingularClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
    ): Boolean {
        var classWithPrefixExists =
            ParsableScriptSuiteRegistryHelper.isStdClassWithPrefixExist(
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && classWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdClassWithPrefixExist(nextTokenText)
            if (classWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (ParsableScriptSuiteRegistryHelper.isStdLibClass(currentTokenText.get())) {
                result = true
                break
            }
        }
        return result
    }

    private fun parseApplicationPluralClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        applicationName: String,
    ): Boolean {
        var classWithPrefixExists =
            ParsableScriptSuiteRegistryHelper.isClassPluralWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && classWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            classWithPrefixExists =
                ParsableScriptSuiteRegistryHelper.isClassPluralWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
            if (classWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (ParsableScriptSuiteRegistryHelper.isApplicationClassPluralName(
                    applicationName,
                    currentTokenText.get(),
                )
            ) {
                result = true
                break
            }
        }
        return result
    }

    private fun parseApplicationSingularClassName(
        builder: PsiBuilder,
        currentTokenText: Ref<String>,
        applicationName: String,
    ): Boolean {
        var classWithPrefixExists =
            ParsableScriptSuiteRegistryHelper.isClassWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && classWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            classWithPrefixExists =
                ParsableScriptSuiteRegistryHelper.isClassWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
            if (classWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (ParsableScriptSuiteRegistryHelper.isApplicationClass(
                    applicationName,
                    currentTokenText.get(),
                )
            ) {
                result = true
                break
            }
        }
        return result
    }
}
