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
            DictionaryClassRegistry.isStdClassPluralWithPrefixExist(
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && classWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            classWithPrefixExists = DictionaryClassRegistry.isStdClassPluralWithPrefixExist(nextTokenText)
            if (classWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (DictionaryClassRegistry.isStdLibClassPluralName(currentTokenText.get())) {
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
            DictionaryClassRegistry.isStdClassWithPrefixExist(
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && classWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            classWithPrefixExists = DictionaryClassRegistry.isStdClassWithPrefixExist(nextTokenText)
            if (classWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (DictionaryClassRegistry.isStdLibClass(currentTokenText.get())) {
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
            DictionaryClassRegistry.isClassPluralWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && classWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            classWithPrefixExists =
                DictionaryClassRegistry.isClassPluralWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
            if (classWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (DictionaryClassRegistry.isApplicationClassPluralName(
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
            DictionaryClassRegistry.isClassWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
        var nextTokenText = currentTokenText.get()
        var result = false
        while (builder.tokenText != null && classWithPrefixExists) {
            builder.advanceLexer()
            nextTokenText += " ${builder.tokenText}"
            classWithPrefixExists =
                DictionaryClassRegistry.isClassWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
            if (classWithPrefixExists) {
                currentTokenText.set(nextTokenText)
            } else if (DictionaryClassRegistry.isApplicationClass(
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
