package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ITEM

internal object TypeSpecifierParser {
    fun parseTypeSpecifier(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "typeSpecifier")) return false
        val marker = enter_section_(builder)
        var result = parseSingularClassName(builder, level + 1)
        if (!result) result = AppleScriptParser.builtInClassIdentifierPlural(builder, level + 1)
        if (!result) result = AppleScriptParser.dictionaryClassIdentifierPlural(builder, level + 1)
        exit_section_(builder, marker, null, result)
        return result
    }

    private fun parseSingularClassName(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "singularClassName")) return false
        val marker = enter_section_(builder)
        var result = AppleScriptParser.dictionaryClassName(builder, level + 1)
        if (!result) result = AppleScriptParser.builtInClassIdentifier(builder, level + 1)
        consumeToken(builder, ITEM)
        exit_section_(builder, marker, null, result)
        return result
    }
}
