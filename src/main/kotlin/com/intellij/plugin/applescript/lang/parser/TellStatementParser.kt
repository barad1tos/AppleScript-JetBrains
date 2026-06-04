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
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "tellSimpleStatement") && nextTokenIs(builder, TELL)) {
            val pushStateBefore = builder.getUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED) == true
            builder.putUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED, false)
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_SIMPLE_STATEMENT, true)
            result = AppleScriptParser.tellSimpleStatement(builder, level + 1)
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_SIMPLE_STATEMENT, false)
            ParserApplicationNameStack.popApplicationNameIfWasPushed(builder)
            builder.putUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED, pushStateBefore)
        }
        return result
    }

    fun parseSimpleObjectReference(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseTellSimpleObjectReference")) return false
        var result = nextTokenIsFast(builder, LPAREN) && AppleScriptParser.parenthesizedExpression(builder, level + 1)
        if (!result) {
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_SIMPLE_OBJECT_REF, true)
            result = AppleScriptParser.expression(builder, level + 1)
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_SIMPLE_OBJECT_REF, false)
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
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseTellCompoundStatement") && nextTokenIs(builder, TELL)) {
            val pushStateBefore = builder.getUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED) == true
            val compoundStateBefore =
                builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_COMPOUND_STATEMENT) == true
            builder.putUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED, false)
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_COMPOUND_STATEMENT, true)
            result = AppleScriptParser.tellCompoundStatement(builder, level + 1)
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_COMPOUND_STATEMENT, compoundStateBefore)
            ParserApplicationNameStack.popApplicationNameIfWasPushed(builder)
            builder.putUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED, pushStateBefore)
        }
        return result
    }

    fun parseUsingTermsFromStatement(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseUsingTermsFromStatement")) return false
        val oldParseUsingTermsState =
            builder.getUserData(AppleScriptGeneratedParserUtil.IS_PARSING_USING_TERMS_FROM_STATEMENT) == true
        val oldPushedState = builder.getUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED) == true
        builder.putUserData(AppleScriptGeneratedParserUtil.IS_PARSING_USING_TERMS_FROM_STATEMENT, true)
        builder.putUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED, false)
        val result = AppleScriptParser.usingTermsFromStatement(builder, level + 1)
        ParserApplicationNameStack.popApplicationNameIfWasPushed(builder)
        builder.putUserData(
            AppleScriptGeneratedParserUtil.IS_PARSING_USING_TERMS_FROM_STATEMENT,
            oldParseUsingTermsState,
        )
        builder.putUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED, oldPushedState)
        return result
    }

    fun isTellStatementStart(builder: PsiBuilder): Boolean {
        var result = false
        if (isInTellStatement(builder)) {
            val previousElement = TellStatementTokenScanner.previousRelevantToken(builder)
            result =
                previousElement === TELL ||
                builder.getUserData(AppleScriptGeneratedParserUtil.IS_PARSING_USING_TERMS_FROM_STATEMENT) == true &&
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
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_COMPOUND_STATEMENT) == true &&
                !StringUtil.isEmpty(toldApplicationName)
        val isCommand =
            canCheckDictionary &&
                isApplicationCommand(builder, level, requireNotNull(toldApplicationName))
        val isProperty =
            canCheckDictionary &&
                ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
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
        builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_SIMPLE_STATEMENT) == true &&
            nextTokenIs(builder, TO) ||
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_COMPOUND_STATEMENT) == true &&
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_SIMPLE_STATEMENT) == false ||
            builder.getUserData(AppleScriptGeneratedParserUtil.IS_PARSING_USING_TERMS_FROM_STATEMENT) == true
}
