package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ABOUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AGAINST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GIVEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OVER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.UNDER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.USING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

internal object DictionaryDirectParameterParser {
    private val commandParameterSelectorStarts =
        TokenSet.create(
            ABOUT,
            AGAINST,
            AS,
            BY,
            FOR,
            FROM,
            GIVEN,
            INTO,
            ON,
            OVER,
            TO,
            UNDER,
            USING,
            VAR_IDENTIFIER,
            WITH,
            WITHOUT,
        )

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
        var result = parseTypedValue(builder, level + 1, parameter.typeSpecifier)
        if (!result) {
            result = AppleScriptParser.expression(builder, level + 1)
        }
        if (!result) {
            result = parseBracketedDirectValueBeforeSelector(builder)
        }
        exit_section_(builder, level, marker, DIRECT_PARAMETER_VAL, result, false, null)
        return result || parameter.isOptional() || isTellCompound
    }

    private fun parseTypedValue(
        builder: PsiBuilder,
        level: Int,
        typeSpecifier: String,
    ): Boolean =
        when (typeSpecifier) {
            "type" -> TypeSpecifierParser.parseTypeSpecifier(builder, level + 1)
            else -> false
        }

    private fun parseBracketedDirectValueBeforeSelector(builder: PsiBuilder): Boolean {
        val shouldConsume = isBracketedDirectParameterBeforeSelector(builder)
        if (shouldConsume) {
            AppleScriptBracketedValueParser.consume(builder, allowNewlines = true)
        }
        return shouldConsume
    }

    private fun isBracketedDirectParameterBeforeSelector(builder: PsiBuilder): Boolean =
        AppleScriptBracketedValueParser
            .scan(builder, allowNewlines = true)
            ?.let { scan -> isCommandParameterSelectorStart(nextNonSpaceToken(builder, scan.endOffset)) } == true

    private fun isCommandParameterSelectorStart(tokenType: IElementType?): Boolean =
        tokenType != null && commandParameterSelectorStarts.contains(tokenType)

    private fun nextNonSpaceToken(
        builder: PsiBuilder,
        initialOffset: Int,
    ): IElementType? {
        var offset = initialOffset
        var tokenType = builder.lookAhead(offset)
        while (tokenType === TokenType.WHITE_SPACE) {
            offset += 1
            tokenType = builder.lookAhead(offset)
        }
        return tokenType
    }
}
