package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COLON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMA
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GIVEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LAND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT

internal object DictionaryCommandParameterParser {
    private data class ParameterParseState(
        val command: AppleScriptCommand,
        val parsedSelector: Ref<String>,
        val isGivenForm: Boolean,
        val isFirst: Boolean,
    )

    fun parseParametersForCommand(
        builder: PsiBuilder,
        level: Int,
        parsedCommandDefinition: AppleScriptCommand,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseParametersForCommand")) return false
        var result = true
        val marker = enter_section_(builder, level, _NONE_, "<parse command handler call parameters>")
        val previousCommandParameterContext =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS)
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS, true)
        try {
            parsedCommandDefinition.directParameter?.let { directParameter ->
                consumeToken(builder, OF)
                result = DictionaryDirectParameterParser.parseValue(builder, level + 1, directParameter)
            }
            if (parsedCommandDefinition.parameters.isNotEmpty()) {
                result = parseCommandParameters(builder, level + 1, parsedCommandDefinition)
            }
            exit_section_(builder, level, marker, null, result, false, null)
        } finally {
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS,
                previousCommandParameterContext,
            )
        }
        // The caller observes a balanced parser section; semantic overload completeness is represented in markers.
        return true
    }

    private fun parseCommandParameters(
        builder: PsiBuilder,
        level: Int,
        commandDefinition: AppleScriptCommand,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseCommandParameters")) return false
        val parsedParameterSelector = Ref<String>()
        parsedParameterSelector.set("")
        val marker = enter_section_(builder, level, _NONE_, "<parse Command Parameters>")
        var remainingMandatoryParameters = commandDefinition.mandatoryParameters.toSet()
        val isGivenForm = consumeToken(builder, GIVEN)
        var result = true
        var index = 0

        while (index < commandDefinition.parameters.size &&
            !nextTokenIs(builder, "", COMMENT, NLS) &&
            result
        ) {
            val state =
                ParameterParseState(
                    commandDefinition,
                    parsedParameterSelector,
                    isGivenForm,
                    index == 0,
                )
            result = parseParameterForCommand(builder, level + 1, state)
            commandDefinition
                .getParameterByName(parsedParameterSelector.get())
                ?.let { parameter -> remainingMandatoryParameters = remainingMandatoryParameters - parameter }
            parsedParameterSelector.set("")
            index++
        }

        result = remainingMandatoryParameters.isEmpty()
        exit_section_(builder, level, marker, null, result, false, null)
        return result
    }

    private fun parseParameterForCommand(
        builder: PsiBuilder,
        level: Int,
        state: ParameterParseState,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseParameterForCommand")) return false
        val marker = enter_section_(builder, level, _NONE_, "<parse Parameter For Command>")
        var result = parseGivenParameter(builder, level + 1, state)
        if (!result) result = parseBooleanParameter(builder, level + 1, state.command, state.parsedSelector)
        exit_section_(builder, level, marker, COMMAND_PARAMETER, result, false, null)
        return result
    }

    private fun parseBooleanParameter(
        builder: PsiBuilder,
        level: Int,
        command: AppleScriptCommand,
        parsedParameterSelector: Ref<String>,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseBooleanParameter")) return false
        val marker = enter_section_(builder, level, _NONE_, "<parse Boolean Parameter>")
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_BOOLEAN_PARAMETER, true)
        var result = consumeToken(builder, WITH)
        if (!result) result = consumeToken(builder, WITHOUT)
        if (!result) result = consumeToken(builder, LAND)
        result = result && parseCommandParameterSelector(builder, level + 1, command, parsedParameterSelector)
        exit_section_(builder, level, marker, null, result, false, null)
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_BOOLEAN_PARAMETER, false)
        return result
    }

    private fun parseGivenParameter(
        builder: PsiBuilder,
        level: Int,
        state: ParameterParseState,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseGivenParameter")) return false
        val marker = enter_section_(builder, level, _NONE_, "<parse Given Parameter>")
        var result = !state.isGivenForm || state.isFirst || consumeToken(builder, COMMA)
        result = result && parseCommandParameterSelector(builder, level + 1, state.command, state.parsedSelector)
        val parameterDefinition = state.command.getParameterByName(state.parsedSelector.get())
        if (state.isGivenForm) result = consumeToken(builder, COLON)
        result = result && parseCommandParameterValue(builder, level + 1, parameterDefinition, state.command)
        exit_section_(builder, level, marker, null, result, false, null)
        return result
    }

    private fun parseCommandParameterValue(
        builder: PsiBuilder,
        level: Int,
        parameterDefinition: CommandParameter?,
        commandDefinition: AppleScriptCommand,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseCommandParameterValue")) return false
        var result = false
        val marker = enter_section_(builder, level, _NONE_, "<parse Command Parameter Value>")

        val parameterTypeSpecifier = parameterDefinition?.typeSpecifier
        if (parameterTypeSpecifier == "type") {
            result = TypeSpecifierParser.parseTypeSpecifier(builder, level + 1)
        } else if (parameterTypeSpecifier == "text") {
            result = AppleScriptParser.stringLiteralExpression(builder, level + 1)
        }
        if (!result) result = parseSingleIdentifierValueBeforeNextSelector(builder, commandDefinition)
        if (!result) result = AppleScriptParser.expression(builder, level + 1)
        exit_section_(builder, level, marker, null, result, false, null)
        return result
    }

    private fun parseSingleIdentifierValueBeforeNextSelector(
        builder: PsiBuilder,
        commandDefinition: AppleScriptCommand,
    ): Boolean {
        if (builder.tokenType !== VAR_IDENTIFIER) return false

        val marker = builder.mark()
        builder.advanceLexer()
        val isBoundedValue = isCommandParameterSelectorStart(builder, commandDefinition)
        if (isBoundedValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isBoundedValue
    }

    private fun isCommandParameterSelectorStart(
        builder: PsiBuilder,
        commandDefinition: AppleScriptCommand,
    ): Boolean {
        val marker = builder.mark()
        var parsedParameterSelector = currentTokenWord(builder).orEmpty()
        var result = false

        while (!builder.eof() && builder.tokenType !== NLS && builder.tokenType !== COMMENT) {
            builder.advanceLexer()
            if (commandDefinition.getParameterByName(parsedParameterSelector) != null) {
                result = true
                break
            }
            parsedParameterSelector += " " + currentTokenWord(builder)
        }

        marker.rollbackTo()
        return result
    }

    private fun parseCommandParameterSelector(
        builder: PsiBuilder,
        level: Int,
        command: AppleScriptCommand,
        parsedParameterSelector: Ref<String>,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseCommandParameterSelector")) return false
        var result = false
        val marker = enter_section_(builder, level, _NONE_, "<parse Command Parameter Selector>")
        parsedParameterSelector.set(currentTokenWord(builder).orEmpty())
        while (!builder.eof() && builder.tokenType !== NLS && builder.tokenType !== COMMENT) {
            builder.advanceLexer()
            if (command.getParameterByName(parsedParameterSelector.get()) != null) {
                result = true
                break
            }
            parsedParameterSelector.set(parsedParameterSelector.get() + " " + currentTokenWord(builder))
        }
        exit_section_(builder, level, marker, COMMAND_PARAMETER_SELECTOR, result, false, null)
        return result
    }

    private fun currentTokenWord(builder: PsiBuilder): String? = builder.tokenText ?: builder.tokenType?.toString()
}
