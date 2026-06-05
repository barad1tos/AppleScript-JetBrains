package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL

internal object DictionaryDirectParameterParser {
    fun parseValue(
        builder: PsiBuilder,
        level: Int,
        parameter: CommandDirectParameter,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseCommandDirectParameterValue")) {
            val isTellCompound = isParsingTellCompoundStatement(builder)
            result =
                isParameterSelectorAhead(builder, parameter, isTellCompound) ||
                parseExpression(builder, level, parameter, isTellCompound)
        }
        return result
    }

    private fun isParsingTellCompoundStatement(builder: PsiBuilder): Boolean =
        builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_COMPOUND_STATEMENT) == true

    private fun isParameterSelectorAhead(
        builder: PsiBuilder,
        parameter: CommandDirectParameter,
        isTellCompound: Boolean,
    ): Boolean {
        var result = false
        if (isTellCompound || parameter.isOptional()) {
            val command = parameter.getCommand()
            for (parameterName in command.parameterNames) {
                result = nextTokenIs(builder, parameterName)
                if (result) break
            }
        }
        return result
    }

    private fun parseExpression(
        builder: PsiBuilder,
        level: Int,
        parameter: CommandDirectParameter,
        isTellCompound: Boolean,
    ): Boolean {
        val marker = enter_section_(builder, level, _NONE_, "<parse Command Direct Parameter Value >")
        var result = false
        if (parameter.typeSpecifier == "type") {
            result = TypeSpecifierParser.parseTypeSpecifier(builder, level + 1)
        }
        if (!result) {
            result = AppleScriptParser.expression(builder, level + 1)
        }
        exit_section_(builder, level, marker, DIRECT_PARAMETER_VAL, result, false, null)
        return result || parameter.isOptional() || isTellCompound
    }
}
