package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.AppleScriptNames
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_CLASS_NAME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ITEM

internal object TypeSpecifierParser {
    fun parseTypeSpecifier(
        builder: PsiBuilder,
        level: Int,
        applicationName: String? = null,
    ): Boolean {
        if (!recursion_guard_(builder, level, "typeSpecifier")) return false
        val marker = enter_section_(builder)
        var result = parseSingularClassName(builder, level + 1, applicationName)
        if (!result) result = AppleScriptParser.builtInClassIdentifierPlural(builder, level + 1)
        if (!result) result = AppleScriptParser.dictionaryClassIdentifierPlural(builder, level + 1)
        exit_section_(builder, marker, null, result)
        return result
    }

    private fun parseSingularClassName(
        builder: PsiBuilder,
        level: Int,
        applicationName: String?,
    ): Boolean {
        if (!recursion_guard_(builder, level, "singularClassName")) return false
        val marker = enter_section_(builder)
        var result = parseDictionaryClassName(builder, level + 1, applicationName)
        if (!result) result = AppleScriptParser.builtInClassIdentifier(builder, level + 1)
        consumeToken(builder, ITEM)
        exit_section_(builder, marker, null, result)
        return result
    }

    private fun parseDictionaryClassName(
        builder: PsiBuilder,
        level: Int,
        applicationName: String?,
    ): Boolean =
        if (applicationName == null) {
            AppleScriptParser.dictionaryClassName(builder, level + 1)
        } else {
            parseDictionaryClassNameInScope(builder, level + 1, applicationName)
        }

    private fun parseDictionaryClassNameInScope(
        builder: PsiBuilder,
        level: Int,
        applicationName: String,
    ): Boolean {
        val tokenText = builder.tokenText
        if (tokenText.isNullOrEmpty() || !AppleScriptNames.isIdentifierStart(tokenText[0])) return false

        val marker = enter_section_(builder, level, _NONE_, DICTIONARY_CLASS_NAME, "<dictionary class name>")
        val result =
            DictionaryTermLookupParser.parseClassName(
                builder,
                level + 1,
                isPluralForm = false,
                lookupScope = DictionaryTermLookupScope(applicationName, areThereUseStatements = false, null),
            )
        exit_section_(builder, level, marker, result, false, null)
        return result
    }
}
