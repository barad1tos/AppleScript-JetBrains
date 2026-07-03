package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser
import com.intellij.lang.parser.GeneratedParserUtilBase._AND_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIsFast
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TELL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO

internal object TellStatementParser {
    fun parseSimpleStatement(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        recursion_guard_(builder, level, "tellSimpleStatement") &&
            nextTokenIs(builder, TELL) &&
            ParserState.withTellSimpleStatement(builder) {
                AppleScriptParser.tellSimpleStatement(builder, level + 1)
            }

    fun parseSimpleObjectReference(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseTellSimpleObjectReference")) return false
        var result = nextTokenIsFast(builder, LPAREN) && AppleScriptParser.parenthesizedExpression(builder, level + 1)
        if (!result) {
            result =
                ParserState.withTellSimpleObjectReference(builder) {
                    AppleScriptParser.expression(builder, level + 1)
                }
        }
        return result
    }

    fun parseExpression(
        builder: PsiBuilder,
        level: Int,
        dictionaryTermToken: String,
        expression: Parser,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseExpression") &&
            nextTokenIsFast(builder, dictionaryTermToken) &&
            !isApplicationDictionaryTerm(builder, level, dictionaryTermToken)
        ) {
            result = expression.parse(builder, level + 1)
        }
        return result
    }

    fun parseCompoundStatement(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        recursion_guard_(builder, level, "parseTellCompoundStatement") &&
            nextTokenIs(builder, TELL) &&
            ParserState.withTellCompoundStatement(builder) {
                AppleScriptParser.tellCompoundStatement(builder, level + 1)
            }

    fun parseUsingTermsFromStatement(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        recursion_guard_(builder, level, "parseUsingTermsFromStatement") &&
            ParserState.withUsingTermsFromStatement(builder) {
                AppleScriptParser.usingTermsFromStatement(builder, level + 1)
            }

    fun isTellStatementStart(builder: PsiBuilder): Boolean {
        var result = false
        if (isInTellStatement(builder)) {
            val previousElement = TellStatementTokenScanner.previousRelevantToken(builder)
            result =
                previousElement === TELL ||
                ParserState.isInsideUsingTermsFromStatement(builder) &&
                previousElement === FROM
        }
        return result
    }

    private fun isApplicationDictionaryTerm(
        builder: PsiBuilder,
        level: Int,
        dictionaryTermToken: String,
    ): Boolean {
        val toldApplicationName = ParserApplicationNameStack.peekTargetApplicationName(builder)
        val canCheckDictionary =
            ParserState.isInsideTellCompoundStatement(builder) &&
                !StringUtil.isEmpty(toldApplicationName)
        val isCommand =
            canCheckDictionary &&
                isApplicationCommand(builder, level, requireNotNull(toldApplicationName))
        val isProperty =
            canCheckDictionary &&
                DictionaryPropertyRegistry.isPropertyWithPrefixExist(
                    requireNotNull(toldApplicationName),
                    dictionaryTermToken,
                )
        return isCommand || isProperty
    }

    private fun isApplicationCommand(
        builder: PsiBuilder,
        level: Int,
        toldApplicationName: String,
    ): Boolean {
        val parsedName = Ref<String>()
        val commandNameMarker = enter_section_(builder, level, _AND_, "<parse Expression>")
        val result =
            DictionaryCommandNameParser.parseForApplication(
                builder,
                level + 1,
                parsedName,
                toldApplicationName,
                true,
            )
        exit_section_(builder, level, commandNameMarker, null, result, false, null)
        return result
    }

    private fun isInTellStatement(builder: PsiBuilder): Boolean =
        ParserState.isInsideTellSimpleStatement(builder) &&
            nextTokenIs(builder, TO) ||
            ParserState.isInsideTellCompoundStatement(builder) &&
            ParserState.hasExitedTellSimpleStatement(builder) ||
            ParserState.isInsideUsingTermsFromStatement(builder)
}
