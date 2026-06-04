package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder

internal object FallbackDictionaryTermParser {
    fun parseKeywordAsProperty(
        builder: PsiBuilder,
        level: Int,
    ): Boolean = FallbackDictionaryPropertyParser.parseKeywordAsProperty(builder, level)

    fun parsePropertyName(
        builder: PsiBuilder,
        level: Int,
    ): Boolean = FallbackDictionaryPropertyParser.parsePropertyName(builder, level)

    fun parseClassName(
        builder: PsiBuilder,
        level: Int,
    ): Boolean = FallbackDictionaryClassParser.parseClassName(builder, level)
}
