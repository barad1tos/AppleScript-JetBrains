package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser
import com.intellij.lang.parser.GeneratedParserUtilBase._AND_
import com.intellij.lang.parser.GeneratedParserUtilBase._COLLAPSE_
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIsFast
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.AppleScriptNames
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.psi.AppleScriptTypes.APPLICATION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.APP
import com.intellij.plugin.applescript.psi.AppleScriptTypes.APPLICATION_REFERENCE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_CONSTANT_LITERAL_EXPRESSION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CLASS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COLON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMA
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COUNT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DATE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_CLASS_NAME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_COMMAND_NAME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIGITS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DOES_NOT_CONTAIN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.END
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ENDS_WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EQ
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EVENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FILE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GIVEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HOURS_CONSTANT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ID
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_CONTAIN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_NOT_IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ITEM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LAND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MINUTES_CONSTANT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NAMED
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SCRIPT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SCRIPTING_ADDITIONS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SECONDS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STARTS_BEGINS_WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TAB
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TELL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THE_KW
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THRU
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THROUGH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHERE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHOSE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import java.util.Stack

class AppleScriptGeneratedParserUtil : GeneratedParserUtilBase() {

    companion object {
        private val PARSING_COMMAND_HANDLER_CALL_PARAMETERS: Key<Boolean> =
            Key.create("applescript.parsing.command.handler.parameters")
        private val PARSING_COMMAND_ASSIGNMENT_STATEMENT: Key<Boolean> =
            Key.create("applescript.parsing.assignment.statement")
        private val PARSING_COMMAND_HANDLER_BOOLEAN_PARAMETER: Key<Boolean> =
            Key.create("applescript.parsing.command.handler.boolean.parameter")

        @JvmField
        val TOLD_APPLICATION_NAME_STACK: Key<Stack<String>> =
            Key.create("applescript.parsing.current.dictionary.name.stack")

        @JvmField
        val TOLD_APPLICATION_ID_STACK: Key<Stack<String>> =
            Key.create("applescript.parsing.current.dictionary.id.stack")

        private val PARSING_TELL_SIMPLE_STATEMENT: Key<Boolean> =
            Key.create("applescript.parsing.tell.simple.statement")
        private val PARSING_TELL_SIMPLE_OBJECT_REF: Key<Boolean> =
            Key.create("applescript.parsing.tell.simple.object.ref")
        private val PARSING_TELL_COMPOUND_STATEMENT: Key<Boolean> =
            Key.create("applescript.parsing.tell.simple.statement")
        private val APPLICATION_NAME_PUSHED: Key<Boolean> =
            Key.create("applescript.parsing.tell.statement.application.name.pushed")
        private val USED_APPLICATION_NAMES: Key<MutableSet<String>> =
            Key.create("applescript.parsing.use.statement.application.name.set")
        private val WAS_USE_STATEMENT_USED: Key<Boolean> =
            Key.create("applescript.parsing.is.use.statement.used")
        private val IS_PARSING_USING_TERMS_FROM_STATEMENT: Key<Boolean> =
            Key.create("applescript.parsing.is.use.statement.used")
        private val PARSING_LITERAL_EXPRESSION: Key<Boolean> =
            Key.create("applescript.parsing.literal.expression")

        private enum class FallbackTermKind {
            Property,
            Class,
        }

        private enum class FallbackCommandParameterMode {
            OptionalDirectParameter,
            ParametersOnly,
        }

        private fun parseDictionaryCommandNameInner(
            builder: PsiBuilder,
            level: Int,
            parsedName: Ref<String>,
            toldApplicationName: String,
            areThereUseStatements: Boolean,
            applicationsToImportFrom: Set<String>?,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseDictionaryCommandNameInner")) return false
            parsedName.set("")

            val checkStandardLibrary = !areThereUseStatements ||
                applicationsToImportFrom == null ||
                applicationsToImportFrom.contains(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)

            ParsableScriptSuiteRegistryHelper.ensureKnownApplicationInitialized(toldApplicationName)
            var result = parseCommandNameForApplication(
                builder,
                level + 1,
                parsedName,
                toldApplicationName,
                checkStandardLibrary,
            )
            if (result) return true

            if (areThereUseStatements && !applicationsToImportFrom.isNullOrEmpty()) {
                for (applicationName in applicationsToImportFrom) {
                    ParsableScriptSuiteRegistryHelper.ensureKnownApplicationInitialized(applicationName)
                    result = parseCommandNameForApplication(builder, level + 1, parsedName, applicationName, false)
                    if (result) return true
                }
            }

            if (checkStandardLibrary) {
                result = parseStdLibCommandName(builder, level + 1, parsedName)
                if (result) return true
            }

            result = parseCommandNameForApplication(
                builder,
                level + 1,
                parsedName,
                ApplicationDictionary.COCOA_STANDARD_LIBRARY,
                checkStandardLibrary,
            )
            if (result) return true

            return parseWellKnownCommandFallback(builder, level + 1, parsedName)
        }

        private fun parseWellKnownCommandFallback(
            builder: PsiBuilder,
            level: Int,
            parsedName: Ref<String>,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseWellKnownCommandFallback")) return false
            if (builder.tokenType !== VAR_IDENTIFIER) return false

            if (parseFallbackCommandPhrase(builder, parsedName, "choose", "from", "list")) return true
            if (parseFallbackCommandPhrase(builder, parsedName, "display", "dialog")) return true
            if (parseFallbackCommandPhrase(builder, parsedName, "do", "shell", "script")) return true
            if (parseFallbackCommandPhrase(builder, parsedName, "run", "script")) return true

            if (builder.tokenText == "read" && builder.lookAhead(1) !== FILE) {
                return false
            }
            val currentText = builder.tokenText
            if (currentText != null && isWellKnownSingleWordCommand(currentText)) {
                parsedName.set(currentText)
                builder.advanceLexer()
                return true
            }
            return false
        }

        private fun parseFallbackCommandPhrase(
            builder: PsiBuilder,
            parsedName: Ref<String>,
            vararg words: String,
        ): Boolean {
            val marker = builder.mark()
            val commandName = mutableListOf<String>()
            for (word in words) {
                val tokenText = builder.tokenText
                if (tokenText == null || !tokenText.equals(word, ignoreCase = true)) {
                    marker.rollbackTo()
                    return false
                }
                commandName.add(tokenText)
                builder.advanceLexer()
            }
            marker.drop()
            parsedName.set(commandName.joinToString(" "))
            return true
        }

        @JvmStatic
        fun parseCommandHandlerCallExpression(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseCommandHandlerCallExpression")) return false
            if (!isCommandHandlerCallStart(builder)) return false

            val parsedCommandName = Ref<String>()
            val toldApplicationName = getTargetApplicationName(builder)
            val areThereUseStatements = builder.getUserData(WAS_USE_STATEMENT_USED) == true
            val applicationsToImport = if (areThereUseStatements) {
                builder.getUserData(USED_APPLICATION_NAMES)
            } else {
                null
            }

            val commandNameMarker = enter_section_(
                builder,
                level,
                _COLLAPSE_,
                "<parse Command Handler Call Expression>",
            )
            var result = parseDictionaryCommandNameInner(
                builder,
                level + 1,
                parsedCommandName,
                toldApplicationName,
                areThereUseStatements,
                applicationsToImport,
            )
            exit_section_(builder, level, commandNameMarker, DICTIONARY_COMMAND_NAME, result, false, null)

            if (!result) return false

            val allCommandsWithName = getAllCommandsWithName(
                builder,
                parsedCommandName.get(),
                toldApplicationName,
                areThereUseStatements,
                applicationsToImport,
            )

            if (allCommandsWithName.isEmpty() &&
                parseFallbackParametersForCommand(builder, level + 1, parsedCommandName.get())
            ) {
                return true
            }

            for (command in allCommandsWithName) {
                result = parseParametersForCommand(builder, level + 1, command)
                if (result) break
            }

            val incompleteHandlerCall = !result &&
                allCommandsWithName.isNotEmpty() &&
                (builder.tokenType === NLS || builder.eof())
            return result || incompleteHandlerCall
        }

        private fun isCommandHandlerCallStart(builder: PsiBuilder): Boolean {
            if (nextTokenIs(builder, NLS)) return false
            if (builder.tokenType === COUNT) return false
            val tokenText = builder.tokenText
            return !tokenText.isNullOrEmpty() && AppleScriptNames.isIdentifierStart(tokenText[0])
        }

        private fun isWellKnownSingleWordCommand(commandName: String): Boolean =
            commandName == "read" ||
                commandName == "move" ||
                commandName == "exists" ||
                commandName == "run" ||
                commandName == "delete" ||
                commandName == "make"

        private fun fallbackCommandParameterMode(commandName: String): FallbackCommandParameterMode? =
            when (commandName.lowercase()) {
                "make" -> FallbackCommandParameterMode.ParametersOnly
                "choose from list",
                "display dialog",
                "do shell script",
                "run script",
                "read",
                "move",
                "exists",
                "run",
                "delete",
                -> FallbackCommandParameterMode.OptionalDirectParameter
                else -> {
                    if (commandName.contains(" ")) FallbackCommandParameterMode.OptionalDirectParameter else null
                }
            }

        private fun parseFallbackParametersForCommand(
            builder: PsiBuilder,
            level: Int,
            commandName: String?,
        ): Boolean {
            val mode = commandName?.let(::fallbackCommandParameterMode) ?: return false
            if (mode == FallbackCommandParameterMode.OptionalDirectParameter) {
                parseOptionalFallbackDirectParameter(builder, level + 1)
            }
            parseFallbackCommandParameters(builder, level + 1)
            return true
        }

        private fun parseOptionalFallbackDirectParameter(builder: PsiBuilder, level: Int): Boolean {
            consumeToken(builder, OF)
            if (!isFallbackDirectParameterStart(builder.tokenType)) return false

            val parameterMarker = enter_section_(builder, level, _NONE_, "<fallback direct parameter>")
            val parameterResult = AppleScriptParser.expression(builder, level + 1)
            exit_section_(builder, level, parameterMarker, DIRECT_PARAMETER_VAL, parameterResult, false, null)
            return parameterResult
        }

        private fun parseFallbackCommandParameters(builder: PsiBuilder, level: Int) {
            while (isFallbackCommandParameterStart(builder.tokenType)) {
                val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter>")
                val result = parseFallbackCommandParameterSelector(builder, level + 1) &&
                    AppleScriptParser.expression(builder, level + 1)
                exit_section_(builder, level, marker, COMMAND_PARAMETER, result, false, null)
                if (!result) return
            }
        }

        private fun parseFallbackCommandParameterSelector(builder: PsiBuilder, level: Int): Boolean {
            val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter selector>")
            val result = when {
                isFallbackPrepositionParameterStart(builder.tokenType) -> {
                    val selectorStart = builder.tokenType
                    builder.advanceLexer()
                    if (selectorStart === WITH && builder.tokenType === VAR_IDENTIFIER) {
                        builder.advanceLexer()
                    }
                    true
                }
                builder.tokenType === VAR_IDENTIFIER -> parseFallbackBareParameterSelector(builder)
                else -> false
            }
            exit_section_(builder, level, marker, COMMAND_PARAMETER_SELECTOR, result, false, null)
            return result
        }

        private fun parseFallbackBareParameterSelector(builder: PsiBuilder): Boolean {
            val firstWord = builder.tokenText.orEmpty()
            builder.advanceLexer()
            if (firstWord.equals("default", ignoreCase = true) && builder.tokenType === VAR_IDENTIFIER) {
                builder.advanceLexer()
                return true
            }
            if (firstWord.equals("giving", ignoreCase = true) &&
                builder.tokenText.equals("up", ignoreCase = true) &&
                builder.lookAhead(1).hasText("after")
            ) {
                builder.advanceLexer()
                builder.advanceLexer()
            }
            return true
        }

        private fun isFallbackDirectParameterStart(tokenType: IElementType?): Boolean =
            tokenType != null &&
                tokenType !== NLS &&
                tokenType !== COMMENT &&
                !isFallbackPrepositionParameterStart(tokenType)

        private fun isFallbackCommandParameterStart(tokenType: IElementType?): Boolean =
            isFallbackPrepositionParameterStart(tokenType) || tokenType === VAR_IDENTIFIER

        private fun isFallbackPrepositionParameterStart(tokenType: IElementType?): Boolean =
            tokenType === TO ||
                tokenType === INTO ||
                tokenType === FROM ||
                tokenType === WITH ||
                tokenType === WITHOUT ||
                tokenType === GIVEN

        private fun IElementType?.hasText(text: String): Boolean =
            this != null && toString().equals(text, ignoreCase = true)

        @JvmStatic
        fun parseApplicationHandlerDefinitionSignature(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseApplicationHandlerDefinitionSignature")) return false
            if (builder.getUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT) != true ||
                builder.getUserData(PARSING_TELL_COMPOUND_STATEMENT) == true
            ) {
                return false
            }

            val parsedCommandName = Ref<String>()
            val toldApplicationName = getTargetApplicationName(builder)
            val commandNameMarker = enter_section_(builder, level, _COLLAPSE_, "<parse Application Handler Definition")
            var result = parseDictionaryCommandNameInner(
                builder,
                level + 1,
                parsedCommandName,
                toldApplicationName,
                true,
                null,
            )
            exit_section_(builder, level, commandNameMarker, DICTIONARY_COMMAND_NAME, result, false, null)

            if (!result) return false

            val allCommandsWithName = getAllCommandsWithName(
                builder,
                parsedCommandName.get(),
                toldApplicationName,
                false,
                null,
            )

            for (command in allCommandsWithName) {
                result = parseParametersForCommand(builder, level + 1, command)
                if (result) break
            }

            val incompleteHandlerCall = !result &&
                allCommandsWithName.isNotEmpty() &&
                (builder.tokenType === NLS || builder.eof())
            return result || incompleteHandlerCall
        }

        private fun getAllCommandsWithName(
            builder: PsiBuilder,
            parsedCommandName: String,
            toldApplicationName: String,
            areThereUseStatements: Boolean,
            applicationsToImport: Set<String>?,
        ): MutableList<AppleScriptCommand> {
            val allCommandsWithName = mutableListOf<AppleScriptCommand>()
            allCommandsWithName.addAll(
                ParsableScriptSuiteRegistryHelper.findApplicationCommands(
                    builder.project,
                    toldApplicationName,
                    parsedCommandName,
                ),
            )
            if (ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY == toldApplicationName) {
                allCommandsWithName.addAll(
                    ParsableScriptSuiteRegistryHelper.findApplicationCommands(
                        builder.project,
                        ApplicationDictionary.COCOA_STANDARD_LIBRARY,
                        parsedCommandName,
                    ),
                )
            }
            if (areThereUseStatements) {
                if (applicationsToImport != null) {
                    for (applicationName in applicationsToImport) {
                        allCommandsWithName.addAll(
                            ParsableScriptSuiteRegistryHelper.findApplicationCommands(
                                builder.project,
                                applicationName,
                                parsedCommandName,
                            ),
                        )
                    }
                }
            } else {
                allCommandsWithName.addAll(
                    ParsableScriptSuiteRegistryHelper.findStdCommands(builder.project, parsedCommandName),
                )
            }
            if (allCommandsWithName.isEmpty()) {
                allCommandsWithName.addAll(
                    ParsableScriptSuiteRegistryHelper.findApplicationCommands(
                        builder.project,
                        ApplicationDictionary.COCOA_STANDARD_LIBRARY,
                        parsedCommandName,
                    ),
                )
            }
            return allCommandsWithName
        }

        private fun getTargetApplicationName(builder: PsiBuilder): String =
            peekTargetApplicationName(builder) ?: ApplicationDictionary.COCOA_STANDARD_LIBRARY

        private fun peekTargetApplicationName(builder: PsiBuilder): String? {
            val applicationNameStack = builder.getUserData(TOLD_APPLICATION_NAME_STACK)
            return if (!applicationNameStack.isNullOrEmpty()) applicationNameStack.peek() else null
        }

        private fun parseCommandNameForApplication(
            builder: PsiBuilder,
            level: Int,
            parsedName: Ref<String>,
            applicationName: String,
            checkStandardLibrary: Boolean,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseCommandNameForApplication")) return false
            var result = false
            parsedName.set("")
            val marker = enter_section_(builder)
            parsedName.set(builder.tokenText ?: "")

            var commandWithPrefixExists = ParsableScriptSuiteRegistryHelper.isCommandWithPrefixExist(
                applicationName,
                parsedName.get(),
            )
            var nextTokenText = parsedName.get()
            while (builder.tokenText != null && commandWithPrefixExists) {
                builder.advanceLexer()
                nextTokenText += " ${builder.tokenText}"
                commandWithPrefixExists = ParsableScriptSuiteRegistryHelper.isCommandWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
                if (commandWithPrefixExists) {
                    parsedName.set(nextTokenText)
                } else if (ParsableScriptSuiteRegistryHelper.isApplicationCommand(applicationName, parsedName.get())) {
                    result = !checkStandardLibrary ||
                        !ParsableScriptSuiteRegistryHelper.isStdCommandWithPrefixExist(nextTokenText)
                    result = result &&
                        !ParsableScriptSuiteRegistryHelper.isClassWithPrefixExist(applicationName, nextTokenText)
                    break
                }
            }
            exit_section_(builder, marker, null, result)
            return result
        }

        private fun parseParametersForCommand(
            builder: PsiBuilder,
            level: Int,
            parsedCommandDefinition: AppleScriptCommand,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseParametersForCommand")) return false
            var result = true
            val marker = enter_section_(builder, level, _NONE_, "<parse command handler call parameters>")
            builder.putUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS, true)
            parsedCommandDefinition.directParameter?.let {
                consumeToken(builder, OF)
                result = parseCommandDirectParameterValue(builder, level + 1, it)
            }
            if (parsedCommandDefinition.parameters.isNotEmpty()) {
                result = parseCommandParameters(builder, level + 1, parsedCommandDefinition)
            }
            exit_section_(builder, level, marker, null, result, false, null)
            builder.putUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS, false)
            return true
        }

        @JvmStatic
        fun isHandlerLabeledParametersCallAllowed(builder: PsiBuilder, level: Int): Boolean =
            builder.getUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT) != true &&
                builder.getUserData(PARSING_TELL_SIMPLE_OBJECT_REF) != true &&
                builder.getUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS) != true

        @JvmStatic
        fun isTreePrevSimpleReference(builder: PsiBuilder, level: Int): Boolean {
            val latestDoneMarker = builder.latestDoneMarker
            return latestDoneMarker != null &&
                latestDoneMarker.tokenType === com.intellij.plugin.applescript.psi.AppleScriptTypes.REFERENCE_EXPRESSION
        }

        @JvmStatic
        fun parseAssignmentStatementInner(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseAssignmentStatementInner")) return false
            builder.putUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT, true)
            val result = AppleScriptParser.assignmentStatement(builder, level + 1)
            builder.putUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT, false)
            return result
        }

        @JvmStatic
        fun parseLiteralExpression(builder: PsiBuilder, level: Int, literalExpression: Parser): Boolean {
            if (!recursion_guard_(builder, level, "parseLiteralExpression")) return false
            builder.putUserData(PARSING_LITERAL_EXPRESSION, true)
            val result = literalExpression.parse(builder, level + 1)
            builder.putUserData(PARSING_LITERAL_EXPRESSION, false)
            return result
        }

        @JvmStatic
        fun parseTellSimpleStatementInner(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "tellSimpleStatement")) return false
            if (!nextTokenIs(builder, TELL)) return false
            val pushStateBefore = builder.getUserData(APPLICATION_NAME_PUSHED) == true
            builder.putUserData(APPLICATION_NAME_PUSHED, false)
            builder.putUserData(PARSING_TELL_SIMPLE_STATEMENT, true)
            val result = AppleScriptParser.tellSimpleStatement(builder, level + 1)
            builder.putUserData(PARSING_TELL_SIMPLE_STATEMENT, false)
            popApplicationNameIfWasPushed(builder)
            builder.putUserData(APPLICATION_NAME_PUSHED, pushStateBefore)
            return result
        }

        @JvmStatic
        fun parseTellSimpleObjectReference(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseTellSimpleObjectReference")) return false
            var result = nextTokenIsFast(builder, LPAREN) && AppleScriptParser.parenthesizedExpression(builder, level + 1)
            if (!result) {
                builder.putUserData(PARSING_TELL_SIMPLE_OBJECT_REF, true)
                result = AppleScriptParser.expression(builder, level + 1)
                builder.putUserData(PARSING_TELL_SIMPLE_OBJECT_REF, false)
            }
            return result
        }

        private fun popApplicationNameIfWasPushed(builder: PsiBuilder) {
            if (builder.getUserData(APPLICATION_NAME_PUSHED) == true) {
                val dictionaryNameStack = builder.getUserData(TOLD_APPLICATION_NAME_STACK)
                if (!dictionaryNameStack.isNullOrEmpty()) {
                    dictionaryNameStack.pop()
                }
            }
        }

        @JvmStatic
        fun parseUsedApplicationNameExternal(builder: PsiBuilder, level: Int, isImporting: Parser): Boolean {
            if (!recursion_guard_(builder, level, "parseUsedApplicationNameExternal")) return false
            if (!nextTokenIs(builder, "parseUsedApplicationNameExternal", APPLICATION, APP, SCRIPTING_ADDITIONS)) {
                return false
            }

            var applicationName: String? = null
            var result = consumeToken(builder, SCRIPTING_ADDITIONS)
            if (result) {
                applicationName = ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
            } else {
                val applicationReferenceMarker = enter_section_(builder, level, _NONE_, "<application reference>")
                val classMarker = enter_section_(builder, level, _NONE_, "<dictionary class name>")
                result = consumeToken(builder, APPLICATION)
                if (!result) result = consumeToken(builder, APP)
                exit_section_(builder, level, classMarker, DICTIONARY_CLASS_NAME, result, false, null)
                if (result) {
                    val applicationNameString = builder.tokenText
                    result = consumeToken(builder, STRING_LITERAL)
                    if (result && !StringUtil.isEmpty(applicationNameString)) {
                        applicationName = applicationNameString?.replace("\"", "")
                    }
                }
                exit_section_(builder, level, applicationReferenceMarker, APPLICATION_REFERENCE, result, false, null)
            }

            val doTermsImport = isImporting.parse(builder, level + 1)
            val nonEmptyApplicationName = applicationName
            if (doTermsImport && !StringUtil.isEmpty(nonEmptyApplicationName)) {
                val usedApplicationNames = builder.getUserData(USED_APPLICATION_NAMES) ?: HashSet<String>().also {
                    builder.putUserData(USED_APPLICATION_NAMES, it)
                }
                usedApplicationNames.add(
                    requireNotNull(nonEmptyApplicationName) {
                        "applicationName non-null: guarded by !StringUtil.isEmpty above"
                    },
                )
            }
            return result
        }

        @JvmStatic
        fun parseUseStatement(builder: PsiBuilder, level: Int, useStatement: Parser): Boolean {
            if (!recursion_guard_(builder, level, "parseUseStatement")) return false
            val result = useStatement.parse(builder, level + 1)
            val previousPass = builder.getUserData(WAS_USE_STATEMENT_USED) == true
            builder.putUserData(WAS_USE_STATEMENT_USED, result || previousPass)
            return result
        }

        @JvmStatic
        fun parseExpression(
            builder: PsiBuilder,
            level: Int,
            dictionaryTermToken: String,
            expression: Parser,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseExpression")) return false
            if (!nextTokenIsFast(builder, dictionaryTermToken)) return false

            if (builder.getUserData(PARSING_TELL_COMPOUND_STATEMENT) == true) {
                val toldApplicationName = peekTargetApplicationName(builder)
                if (!StringUtil.isEmpty(toldApplicationName)) {
                    val parsedName = Ref<String>()
                    val commandNameMarker = enter_section_(builder, level, _AND_, "<parse Expression>")
                    val result = parseCommandNameForApplication(
                        builder,
                        level + 1,
                        parsedName,
                        toldApplicationName ?: "",
                        true,
                    )
                    exit_section_(builder, level, commandNameMarker, null, result, false, null)
                    if (result) return false
                    if (ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(toldApplicationName ?: "", dictionaryTermToken)) {
                        return false
                    }
                }
            }
            return expression.parse(builder, level + 1)
        }

        @JvmStatic
        fun parseTellCompoundStatement(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseTellCompoundStatement")) return false
            if (!nextTokenIs(builder, TELL)) return false

            val pushStateBefore = builder.getUserData(APPLICATION_NAME_PUSHED) == true
            val compoundStateBefore = builder.getUserData(PARSING_TELL_COMPOUND_STATEMENT) == true
            builder.putUserData(APPLICATION_NAME_PUSHED, false)
            builder.putUserData(PARSING_TELL_COMPOUND_STATEMENT, true)

            val result = AppleScriptParser.tellCompoundStatement(builder, level + 1)

            builder.putUserData(PARSING_TELL_COMPOUND_STATEMENT, compoundStateBefore)
            popApplicationNameIfWasPushed(builder)
            builder.putUserData(APPLICATION_NAME_PUSHED, pushStateBefore)
            return result
        }

        @JvmStatic
        fun parseUsingTermsFromStatement(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseUsingTermsFromStatement")) return false

            val oldParseUsingTermsState = builder.getUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT) == true
            val oldPushedState = builder.getUserData(APPLICATION_NAME_PUSHED) == true
            builder.putUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT, true)
            builder.putUserData(APPLICATION_NAME_PUSHED, false)

            val result = AppleScriptParser.usingTermsFromStatement(builder, level + 1)

            popApplicationNameIfWasPushed(builder)
            builder.putUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT, oldParseUsingTermsState)
            builder.putUserData(APPLICATION_NAME_PUSHED, oldPushedState)

            return result
        }

        @JvmStatic
        fun pushStdLibrary(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "pushStdLibrary")) return false
            val result = consumeToken(builder, SCRIPTING_ADDITIONS)
            if (result) {
                pushTargetApplicationName(builder, ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
            }
            return result
        }

        @JvmStatic
        fun parseApplicationName(
            builder: PsiBuilder,
            level: Int,
            tellStatementStartCondition: Parser,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseApplicationName")) return false
            consumeToken(builder, THE_KW)

            val classMarker = enter_section_(builder, level, _NONE_, "<parse application name>")
            var hasApplicationToken = consumeToken(builder, APPLICATION)
            if (!hasApplicationToken) hasApplicationToken = consumeToken(builder, APP)
            exit_section_(builder, level, classMarker, DICTIONARY_CLASS_NAME, hasApplicationToken, false, null)

            if (!nextTokenIs(builder, "", STRING_LITERAL, ID)) return false
            var applicationNameString = builder.tokenText
            var result = consumeToken(builder, STRING_LITERAL)
            if (!result) result = consumeToken(builder, ID)

            if (result && applicationNameString != null) {
                applicationNameString = applicationNameString.replace("\"", "")
                if (!StringUtil.isEmptyOrSpaces(applicationNameString) &&
                    tellStatementStartCondition.parse(builder, level + 1)
                ) {
                    pushTargetApplicationName(builder, applicationNameString)
                }
            }
            return result
        }

        @JvmStatic
        fun isTellStatementStart(builder: PsiBuilder, level: Int): Boolean {
            if (!isInTellStatement(builder, level + 1)) return false
            var index = -1
            var previousElement = builder.rawLookup(index)
            while (previousElement === TokenType.WHITE_SPACE ||
                previousElement === MY ||
                previousElement === APPLICATION ||
                previousElement === STRING_LITERAL ||
                previousElement == null ||
                previousElement === ID
            ) {
                previousElement = builder.rawLookup(--index)
            }
            return previousElement === TELL ||
                builder.getUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT) == true && previousElement === FROM
        }

        private fun isInTellStatement(builder: PsiBuilder, level: Int): Boolean =
            builder.getUserData(PARSING_TELL_SIMPLE_STATEMENT) == true && nextTokenIs(builder, TO) ||
                builder.getUserData(PARSING_TELL_COMPOUND_STATEMENT) == true &&
                builder.getUserData(PARSING_TELL_SIMPLE_STATEMENT) == false ||
                builder.getUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT) == true

        private fun pushTargetApplicationName(builder: PsiBuilder, applicationNameString: String): Stack<String> {
            val dictionaryNameStack = builder.getUserData(TOLD_APPLICATION_NAME_STACK) ?: Stack<String>().also {
                builder.putUserData(TOLD_APPLICATION_NAME_STACK, it)
            }
            dictionaryNameStack.push(applicationNameString)
            builder.putUserData(APPLICATION_NAME_PUSHED, true)
            return dictionaryNameStack
        }

        private fun parseCommandParameters(
            builder: PsiBuilder,
            level: Int,
            commandDefinition: AppleScriptCommand,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseCommandParameters")) return false
            var result = true
            val parsedParameterSelector = Ref<String>()
            parsedParameterSelector.set("")
            val marker = enter_section_(builder, level, _NONE_, "<parse Command Parameters>")

            val mandatoryParameters = commandDefinition.mandatoryParameters.toMutableList()
            if (mandatoryParameters.isNotEmpty()) {
                val givenForm = consumeToken(builder, GIVEN)
                var index = 0
                while (index < commandDefinition.parameters.size &&
                    !nextTokenIs(builder, "", COMMENT, NLS) &&
                    result
                ) {
                    result = parseParameterForCommand(
                        builder,
                        level + 1,
                        commandDefinition,
                        parsedParameterSelector,
                        givenForm,
                        index == 0,
                    )
                    commandDefinition.getParameterByName(parsedParameterSelector.get())?.let(mandatoryParameters::remove)
                    parsedParameterSelector.set("")
                    index++
                }
            } else {
                val givenForm = consumeToken(builder, GIVEN)
                var index = 0
                while (index < commandDefinition.parameters.size &&
                    !nextTokenIs(builder, "", COMMENT, NLS) &&
                    result
                ) {
                    result = parseParameterForCommand(
                        builder,
                        level + 1,
                        commandDefinition,
                        parsedParameterSelector,
                        givenForm,
                        index == 0,
                    )
                    parsedParameterSelector.set("")
                    index++
                }
            }
            result = mandatoryParameters.isEmpty()
            result = true
            exit_section_(builder, level, marker, null, result, false, null)

            return result
        }

        private fun parseParameterForCommand(
            builder: PsiBuilder,
            level: Int,
            command: AppleScriptCommand,
            parsedParameterSelector: Ref<String>,
            givenForm: Boolean,
            first: Boolean,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseParameterForCommand")) return false
            val marker = enter_section_(builder, level, _NONE_, "<parse Parameter For Command>")
            var result = parseGivenParameter(builder, level + 1, command, parsedParameterSelector, givenForm, first)
            if (!result) result = parseBooleanParameter(builder, level + 1, command, parsedParameterSelector)
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
            builder.putUserData(PARSING_COMMAND_HANDLER_BOOLEAN_PARAMETER, true)
            var result = consumeToken(builder, WITH)
            if (!result) result = consumeToken(builder, WITHOUT)
            if (!result) result = consumeToken(builder, LAND)
            result = result && parseCommandParameterSelector(builder, level + 1, command, parsedParameterSelector)
            exit_section_(builder, level, marker, null, result, false, null)
            builder.putUserData(PARSING_COMMAND_HANDLER_BOOLEAN_PARAMETER, false)
            return result
        }

        private fun parseGivenParameter(
            builder: PsiBuilder,
            level: Int,
            command: AppleScriptCommand,
            parsedParameterSelector: Ref<String>,
            givenForm: Boolean,
            first: Boolean,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseGivenParameter")) return false
            val marker = enter_section_(builder, level, _NONE_, "<parse Given Parameter>")
            var result = !givenForm || first || consumeToken(builder, COMMA)
            result = result && parseCommandParameterSelector(builder, level + 1, command, parsedParameterSelector)
            val parameterDefinition = command.getParameterByName(parsedParameterSelector.get())
            if (givenForm) result = consumeToken(builder, COLON)
            result = result && parseCommandParameterValue(builder, level + 1, parameterDefinition)
            exit_section_(builder, level, marker, null, result, false, null)
            return result
        }

        private fun parseCommandParameterValue(
            builder: PsiBuilder,
            level: Int,
            parameterDefinition: CommandParameter?,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseCommandParameterValue")) return false
            var result = false
            val marker = enter_section_(builder, level, _NONE_, "<parse Command Parameter Value>")

            val parameterTypeSpecifier = parameterDefinition?.typeSpecifier
            if (parameterTypeSpecifier == "type") {
                result = typeSpecifier(builder, level + 1)
            } else if (parameterTypeSpecifier == "text") {
                result = AppleScriptParser.stringLiteralExpression(builder, level + 1)
            }
            if (!result) result = AppleScriptParser.expression(builder, level + 1)
            exit_section_(builder, level, marker, null, result, false, null)
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
            parsedParameterSelector.set(builder.tokenText ?: "")
            while (!builder.eof() && builder.tokenType !== NLS && builder.tokenType !== COMMENT) {
                builder.advanceLexer()
                if (command.getParameterByName(parsedParameterSelector.get()) != null) {
                    result = true
                    break
                }
                parsedParameterSelector.set(parsedParameterSelector.get() + " " + builder.tokenText)
            }
            exit_section_(builder, level, marker, COMMAND_PARAMETER_SELECTOR, result, false, null)
            return result
        }

        private fun parseStdLibCommandName(
            builder: PsiBuilder,
            level: Int,
            parsedName: Ref<String>,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseStdLibCommandName")) return false
            var result = false
            parsedName.set("")
            parsedName.set(builder.tokenText ?: "")
            val marker = enter_section_(builder)
            var commandWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdCommandWithPrefixExist(parsedName.get())
            var nextTokenText = parsedName.get()
            while (builder.tokenText != null && commandWithPrefixExists) {
                builder.advanceLexer()
                nextTokenText += " ${builder.tokenText}"
                commandWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdCommandWithPrefixExist(nextTokenText)
                if (commandWithPrefixExists) {
                    parsedName.set(nextTokenText)
                } else if (ParsableScriptSuiteRegistryHelper.isStdCommand(parsedName.get())) {
                    result = true
                    break
                }
            }
            exit_section_(builder, marker, null, result)
            return result
        }

        private fun parseCommandDirectParameterValue(
            builder: PsiBuilder,
            level: Int,
            parameter: CommandDirectParameter,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseCommandDirectParameterValue")) return false
            var result = false
            val isTellCompound = builder.getUserData(PARSING_TELL_COMPOUND_STATEMENT) == true
            if (isTellCompound || parameter.isOptional()) {
                val command = parameter.getCommand()
                for (parameterName in command.parameterNames) {
                    if (nextTokenIs(builder, parameterName)) return true
                }
            }
            val marker = enter_section_(builder, level, _NONE_, "<parse Command Direct Parameter Value >")
            val parameterTypeSpecifier = parameter.typeSpecifier
            if (parameterTypeSpecifier == "type") {
                result = typeSpecifier(builder, level + 1)
            }
            if (!result) {
                result = AppleScriptParser.expression(builder, level + 1)
            }
            exit_section_(builder, level, marker, DIRECT_PARAMETER_VAL, result, false, null)
            return result || parameter.isOptional() || isTellCompound
        }

        @JvmStatic
        fun typeSpecifier(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "typeSpecifier")) return false
            val marker = enter_section_(builder)
            var result = singularClassName(builder, level + 1)
            if (!result) result = AppleScriptParser.builtInClassIdentifierPlural(builder, level + 1)
            if (!result) result = AppleScriptParser.dictionaryClassIdentifierPlural(builder, level + 1)
            exit_section_(builder, marker, null, result)
            return result
        }

        @JvmStatic
        fun singularClassName(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "singularClassName")) return false
            val marker = enter_section_(builder)
            var result = AppleScriptParser.dictionaryClassName(builder, level + 1)
            if (!result) result = AppleScriptParser.builtInClassIdentifier(builder, level + 1)
            consumeToken(builder, ITEM)
            exit_section_(builder, marker, null, result)
            return result
        }

        @JvmStatic
        fun parseCommandParameterSelector(builder: PsiBuilder, level: Int): Boolean = false

        @JvmStatic
        fun isPossessivePpronoun(builder: PsiBuilder, level: Int): Boolean {
            val previousNode: LighterASTNode? = builder.latestDoneMarker
            return previousNode != null &&
                previousNode.tokenType === BUILT_IN_CONSTANT_LITERAL_EXPRESSION &&
                previousNode.toString().equals("its", ignoreCase = true)
        }

        @JvmStatic
        fun parseDictionaryCommandName(builder: PsiBuilder, level: Int): Boolean {
            if (nextTokenIs(builder, NLS)) return false
            val parsedCommandName = Ref<String>()
            val toldApplicationName = getTargetApplicationName(builder)
            val areThereUseStatements = builder.getUserData(WAS_USE_STATEMENT_USED) == true
            val applicationsToImport = if (areThereUseStatements) {
                builder.getUserData(USED_APPLICATION_NAMES)
            } else {
                null
            }
            val marker = enter_section_(builder, level, _COLLAPSE_, "<parse ApplicationDictionary Command Name>")
            val result = parseDictionaryCommandNameInner(
                builder,
                level + 1,
                parsedCommandName,
                toldApplicationName,
                areThereUseStatements,
                applicationsToImport,
            )
            exit_section_(builder, level, marker, DICTIONARY_COMMAND_NAME, result, false, null)
            return result
        }

        @JvmStatic
        fun parseIncompleteCommandCall(builder: PsiBuilder, level: Int): Boolean =
            parseDictionaryCommandName(builder, level + 1)

        @JvmStatic
        fun parseDictionaryPropertyName(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseDictionaryPropertyInner")) return false
            val toldApplicationName = getTargetApplicationName(builder)
            val areThereUseStatements = builder.getUserData(WAS_USE_STATEMENT_USED) == true
            val applicationsToImportFrom = if (areThereUseStatements) {
                builder.getUserData(USED_APPLICATION_NAMES)
            } else {
                null
            }

            var result = tryToParseApplicationProperty(builder, level + 1, toldApplicationName)
            if (result) return true

            if (areThereUseStatements) {
                if (!applicationsToImportFrom.isNullOrEmpty()) {
                    for (applicationName in applicationsToImportFrom) {
                        result = tryToParseApplicationProperty(builder, level + 1, applicationName)
                        if (result) return true
                    }
                }
            } else {
                result = tryToParseStdProperty(builder, level + 1)
                if (result) return true
                result = tryToParseApplicationProperty(
                    builder,
                    level + 1,
                    ApplicationDictionary.COCOA_STANDARD_LIBRARY,
                )
                if (result) return true
            }

            if (parseKeywordAsPropertyFallback(builder, level + 1)) return true
            return parseFallbackBareIdentifier(builder, level + 1, FallbackTermKind.Property)
        }

        private fun parseKeywordAsPropertyFallback(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseKeywordAsPropertyFallback")) return false
            if (isContextualPropertyTerm(builder.tokenType) &&
                isFallbackAnchor(builder.lookAhead(1), FallbackTermKind.Property)
            ) {
                builder.advanceLexer()
                return true
            }
            if (builder.tokenType === VAR_IDENTIFIER &&
                isContextualPropertyTerm(builder.lookAhead(1)) &&
                isFallbackAnchor(builder.lookAhead(2), FallbackTermKind.Property)
            ) {
                builder.advanceLexer()
                builder.advanceLexer()
                return true
            }
            if (isContextualPropertyTerm(builder.tokenType) &&
                builder.lookAhead(1) === VAR_IDENTIFIER &&
                isFallbackAnchor(builder.lookAhead(2), FallbackTermKind.Property)
            ) {
                builder.advanceLexer()
                builder.advanceLexer()
                return true
            }
            return false
        }

        @JvmStatic
        fun parseDictionaryClassName(
            builder: PsiBuilder,
            level: Int,
            isPluralForm: Boolean,
            checkForUseStatements: Parser,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseDictionaryClassName")) return false
            val tokenText = builder.tokenText
            if (tokenText.isNullOrEmpty() || !AppleScriptNames.isIdentifierStart(tokenText[0])) return false
            val toldApplicationName = getTargetApplicationName(builder)
            val areThereUseStatements = checkForUseStatements.parse(builder, level + 1)
            val applicationsToImportFrom = if (areThereUseStatements) {
                builder.getUserData(USED_APPLICATION_NAMES)
            } else {
                null
            }
            val currentTokenText = Ref<String>()
            currentTokenText.set(tokenText)
            val result = parseDictionaryClassName(
                builder,
                level + 1,
                currentTokenText,
                isPluralForm,
                toldApplicationName,
                areThereUseStatements,
                applicationsToImportFrom,
            )
            if (result) return true
            return parseFallbackBareIdentifier(builder, level + 1, FallbackTermKind.Class)
        }

        @JvmStatic
        fun parseCheckForUseStatements(builder: PsiBuilder, level: Int): Boolean =
            recursion_guard_(builder, level, "parseCheckForUseStatements") &&
                builder.getUserData(WAS_USE_STATEMENT_USED) == true

        private fun parseFallbackBareIdentifier(
            builder: PsiBuilder,
            level: Int,
            termKind: FallbackTermKind,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseFallbackBareIdentifier")) return false
            if (!isFallbackTermToken(builder.tokenType, termKind)) return false
            if (isFallbackAnchor(builder.lookAhead(1), termKind)) {
                return advanceFallbackTerm(builder)
            }
            return when (termKind) {
                FallbackTermKind.Class -> parseFallbackClassIdentifier(builder)
                FallbackTermKind.Property -> parseFallbackPropertyIdentifier(builder)
            }
        }

        private fun parseFallbackClassIdentifier(builder: PsiBuilder): Boolean {
            if (isClassDirectSelectorAnchor(builder.lookAhead(1))) {
                return advanceFallbackTerm(builder)
            }
            if (builder.lookAhead(1) === VAR_IDENTIFIER && isClassRangeAnchor(builder.lookAhead(2))) {
                return advanceFallbackTerm(builder)
            }
            if (isProcessClassDirectReference(builder)) {
                return advanceFallbackTerm(builder)
            }
            return parseFallbackTwoWordIdentifier(builder, FallbackTermKind.Class)
        }

        private fun parseFallbackPropertyIdentifier(builder: PsiBuilder): Boolean {
            if (
                builder.tokenType === VAR_IDENTIFIER &&
                isContextualPropertyTerm(builder.lookAhead(1)) &&
                isFallbackAnchor(builder.lookAhead(2), FallbackTermKind.Property)
            ) {
                return advanceFallbackTermPair(builder)
            }
            if (
                isContextualPropertyTerm(builder.lookAhead(1)) &&
                isPropertyTerminatorAnchor(builder.lookAhead(2))
            ) {
                return advanceFallbackTermPair(builder)
            }
            if (builder.lookAhead(1) === VAR_IDENTIFIER && isPropertyTerminatorAnchor(builder.lookAhead(2))) {
                return advanceFallbackTermPair(builder)
            }
            return parseFallbackTwoWordIdentifier(builder, FallbackTermKind.Property)
        }

        private fun parseFallbackTwoWordIdentifier(builder: PsiBuilder, termKind: FallbackTermKind): Boolean {
            if (builder.lookAhead(1) !== VAR_IDENTIFIER) return false
            if (!isFallbackAnchor(builder.lookAhead(2), termKind)) return false
            return advanceFallbackTermPair(builder)
        }

        private fun advanceFallbackTerm(builder: PsiBuilder): Boolean {
            builder.advanceLexer()
            return true
        }

        private fun advanceFallbackTermPair(builder: PsiBuilder): Boolean {
            builder.advanceLexer()
            builder.advanceLexer()
            return true
        }

        private fun isFallbackAnchor(tokenType: IElementType?, termKind: FallbackTermKind): Boolean {
            if (tokenType === OF || tokenType === IN) return true
            if (termKind == FallbackTermKind.Property) {
                return isPropertyComparisonAnchor(tokenType)
            }
            if (termKind == FallbackTermKind.Class) {
                return tokenType === DIGITS || tokenType === WHOSE || tokenType === WHERE
            }
            return false
        }

        private fun isFallbackTermToken(tokenType: IElementType?, termKind: FallbackTermKind): Boolean =
            tokenType === VAR_IDENTIFIER || termKind == FallbackTermKind.Class && isContextualClassTerm(tokenType)

        private fun isClassDirectSelectorAnchor(tokenType: IElementType?): Boolean =
            tokenType === STRING_LITERAL || tokenType === NAMED

        private fun isProcessClassDirectReference(builder: PsiBuilder): Boolean =
            builder.tokenText.equals("process", ignoreCase = true) &&
                builder.lookAhead(1) === VAR_IDENTIFIER &&
                isClassDirectReferenceAnchor(builder.lookAhead(2))

        private fun isClassDirectReferenceAnchor(tokenType: IElementType?): Boolean =
            tokenType === AS ||
                tokenType === OF ||
                tokenType === IN ||
                tokenType === RPAREN ||
                tokenType === NLS ||
                tokenType === COMMENT

        private fun isPropertyComparisonAnchor(tokenType: IElementType?): Boolean =
            tokenType === EQ ||
                tokenType === NE ||
                tokenType === LT ||
                tokenType === GT ||
                tokenType === LE ||
                tokenType === GE ||
                tokenType === STARTS_BEGINS_WITH ||
                tokenType === ENDS_WITH ||
                tokenType === DOES_NOT_CONTAIN ||
                tokenType === IS_IN ||
                tokenType === IS_NOT_IN ||
                tokenType === IS_CONTAIN

        private fun isPropertyTerminatorAnchor(tokenType: IElementType?): Boolean =
            tokenType === NLS || tokenType === RPAREN

        private fun isClassRangeAnchor(tokenType: IElementType?): Boolean =
            tokenType === THRU || tokenType === THROUGH || tokenType === FROM || tokenType === TO

        private fun isContextualPropertyTerm(tokenType: IElementType?): Boolean =
            tokenType === CLASS ||
                tokenType === COUNT ||
                tokenType === HOURS_CONSTANT ||
                tokenType === ID ||
                tokenType === MINUTES_CONSTANT ||
                tokenType === SECONDS ||
                tokenType === END ||
                isContextualSpecifierTermToken(tokenType)

        private fun isContextualClassTerm(tokenType: IElementType?): Boolean =
            isContextualSpecifierTermToken(tokenType)

        private fun isContextualSpecifierTermToken(tokenType: IElementType?): Boolean =
            tokenType === EVENT || tokenType === TAB || tokenType === FILE || tokenType === DATE

        private fun tryToParseStdProperty(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "tryToParseStdProperty")) return false
            var result = false
            val marker = enter_section_(builder)
            val currentTokenText = Ref<String>()
            currentTokenText.set(builder.tokenText ?: "")
            var propertyWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdPropertyWithPrefixExist(
                currentTokenText.get(),
            )
            var nextTokenText = currentTokenText.get()
            while (builder.tokenText != null && propertyWithPrefixExists) {
                builder.advanceLexer()
                nextTokenText += " ${builder.tokenText}"
                propertyWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdPropertyWithPrefixExist(nextTokenText)
                if (propertyWithPrefixExists) {
                    currentTokenText.set(nextTokenText)
                } else if (ParsableScriptSuiteRegistryHelper.isStdProperty(currentTokenText.get())) {
                    result = true
                    break
                }
            }
            exit_section_(builder, marker, null, result)
            return result
        }

        private fun tryToParseApplicationProperty(
            builder: PsiBuilder,
            level: Int,
            applicationName: String,
        ): Boolean {
            if (!recursion_guard_(builder, level, "tryToParseApplicationProperty")) return false
            var result = false
            val marker = enter_section_(builder)
            val currentTokenText = Ref<String>()
            currentTokenText.set(builder.tokenText ?: "")
            var propertyWithPrefixExists = ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
            var nextTokenText = currentTokenText.get()
            while (builder.tokenText != null && propertyWithPrefixExists) {
                builder.advanceLexer()
                nextTokenText += " ${builder.tokenText}"
                propertyWithPrefixExists = ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
                if (propertyWithPrefixExists) {
                    currentTokenText.set(nextTokenText)
                } else if (ParsableScriptSuiteRegistryHelper.isApplicationProperty(applicationName, currentTokenText.get())) {
                    result = true
                    break
                }
            }
            exit_section_(builder, marker, null, result)
            return result
        }

        private fun parseDictionaryClassName(
            builder: PsiBuilder,
            level: Int,
            currentTokenText: Ref<String>,
            isPluralForm: Boolean,
            toldApplicationName: String,
            areThereUseStatements: Boolean,
            applicationsToImportFrom: Set<String>?,
        ): Boolean {
            var propertyExists = false
            var marker = enter_section_(builder)
            var result = parseApplicationClassName(builder, level + 1, currentTokenText, isPluralForm, toldApplicationName)
            if (result) {
                currentTokenText.set(currentTokenText.get() + " " + builder.tokenText)
                propertyExists = ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
                    toldApplicationName,
                    currentTokenText.get(),
                )
            }
            exit_section_(builder, marker, null, result && !propertyExists)
            if (propertyExists) return false
            if (result) return true

            if (areThereUseStatements) {
                if (!applicationsToImportFrom.isNullOrEmpty()) {
                    for (applicationName in applicationsToImportFrom) {
                        marker = enter_section_(builder)
                        result = parseApplicationClassName(builder, level + 1, currentTokenText, isPluralForm, applicationName)
                        if (result) {
                            currentTokenText.set(currentTokenText.get() + " " + builder.tokenText)
                            propertyExists = ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
                                applicationName,
                                currentTokenText.get(),
                            )
                        }
                        exit_section_(builder, marker, null, result && !propertyExists)
                        if (propertyExists) return false
                        if (result) return true
                    }
                }
            } else {
                marker = enter_section_(builder)
                result = parseStdClassName(builder, level + 1, currentTokenText, isPluralForm)
                exit_section_(builder, marker, null, result)
                if (result) return true
            }

            marker = enter_section_(builder)
            result = parseApplicationClassName(
                builder,
                level + 1,
                currentTokenText,
                isPluralForm,
                ApplicationDictionary.COCOA_STANDARD_LIBRARY,
            )
            if (result) {
                currentTokenText.set(currentTokenText.get() + " " + builder.tokenText)
                propertyExists = ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
                    toldApplicationName,
                    currentTokenText.get(),
                )
            }
            exit_section_(builder, marker, null, result && !propertyExists)
            return !propertyExists && result
        }

        private fun parseStdClassName(
            builder: PsiBuilder,
            level: Int,
            currentTokenText: Ref<String>,
            isPluralForm: Boolean,
        ): Boolean {
            currentTokenText.set(builder.tokenText ?: "")
            if (isPluralForm) {
                var classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdClassPluralWithPrefixExist(
                    currentTokenText.get(),
                )
                var nextTokenText = currentTokenText.get()
                while (builder.tokenText != null && classWithPrefixExists) {
                    builder.advanceLexer()
                    nextTokenText += " ${builder.tokenText}"
                    classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdClassPluralWithPrefixExist(nextTokenText)
                    if (classWithPrefixExists) {
                        currentTokenText.set(nextTokenText)
                    } else if (ParsableScriptSuiteRegistryHelper.isStdLibClassPluralName(currentTokenText.get())) {
                        return true
                    }
                }
                return false
            }

            var classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdClassWithPrefixExist(
                currentTokenText.get(),
            )
            var nextTokenText = currentTokenText.get()
            while (builder.tokenText != null && classWithPrefixExists) {
                builder.advanceLexer()
                nextTokenText += " ${builder.tokenText}"
                classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdClassWithPrefixExist(nextTokenText)
                if (classWithPrefixExists) {
                    currentTokenText.set(nextTokenText)
                } else if (ParsableScriptSuiteRegistryHelper.isStdLibClass(currentTokenText.get())) {
                    return true
                }
            }
            return false
        }

        private fun parseApplicationClassName(
            builder: PsiBuilder,
            level: Int,
            currentTokenText: Ref<String>,
            isPluralForm: Boolean,
            applicationName: String,
        ): Boolean {
            currentTokenText.set(builder.tokenText ?: "")
            if (isPluralForm) {
                var classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isClassPluralWithPrefixExist(
                    applicationName,
                    currentTokenText.get(),
                )
                var nextTokenText = currentTokenText.get()
                while (builder.tokenText != null && classWithPrefixExists) {
                    builder.advanceLexer()
                    nextTokenText += " ${builder.tokenText}"
                    classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isClassPluralWithPrefixExist(
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
                        return true
                    }
                }
                return false
            }

            var classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isClassWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
            var nextTokenText = currentTokenText.get()
            while (builder.tokenText != null && classWithPrefixExists) {
                builder.advanceLexer()
                nextTokenText += " ${builder.tokenText}"
                classWithPrefixExists = ParsableScriptSuiteRegistryHelper.isClassWithPrefixExist(
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
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun parseCommandParametersExpression(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseCommandParametersExpression")) return false
            builder.putUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS, true)
            val result = AppleScriptParser.expression(builder, level + 1)
            builder.putUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS, false)
            return result
        }

        @JvmStatic
        fun parseDictionaryConstant(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "parseDictionaryConstant")) return false
            val toldApplicationName = getTargetApplicationName(builder)
            val insideExpression = builder.getUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS) == true ||
                builder.getUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT) == true ||
                builder.getUserData(PARSING_LITERAL_EXPRESSION) == true
            if (ApplicationDictionary.COCOA_STANDARD_LIBRARY != toldApplicationName && !insideExpression) {
                return false
            }

            val areThereUseStatements = builder.getUserData(WAS_USE_STATEMENT_USED) == true
            val applicationsToImportFrom = if (areThereUseStatements) {
                builder.getUserData(USED_APPLICATION_NAMES)
            } else {
                null
            }

            var result = tryToParseApplicationConstant(builder, level + 1, toldApplicationName)
            if (result) return true
            if (areThereUseStatements && insideExpression) {
                if (!applicationsToImportFrom.isNullOrEmpty()) {
                    for (applicationName in applicationsToImportFrom) {
                        result = tryToParseApplicationConstant(builder, level + 1, applicationName)
                        if (result) return true
                    }
                }
            } else {
                result = tryToParseStdConstant(builder, level + 1)
                if (result) return true
                result = tryToParseApplicationConstant(
                    builder,
                    level + 1,
                    ApplicationDictionary.COCOA_STANDARD_LIBRARY,
                )
                if (result) return true
            }
            return false
        }

        private fun tryToParseStdConstant(builder: PsiBuilder, level: Int): Boolean {
            if (!recursion_guard_(builder, level, "tryToParseStdConstant")) return false
            val currentTokenText = Ref<String>()
            currentTokenText.set(builder.tokenText ?: "")
            var result = false
            var propertyOrClassExists = false
            var constantWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdConstantWithPrefixExist(
                currentTokenText.get(),
            )
            var nextTokenText = currentTokenText.get()
            val marker = enter_section_(builder)
            while (builder.tokenText != null && constantWithPrefixExists) {
                builder.advanceLexer()
                nextTokenText += " ${builder.tokenText}"
                constantWithPrefixExists = ParsableScriptSuiteRegistryHelper.isStdConstantWithPrefixExist(nextTokenText)
                if (constantWithPrefixExists) {
                    currentTokenText.set(nextTokenText)
                } else if (ParsableScriptSuiteRegistryHelper.isStdConstant(currentTokenText.get())) {
                    result = true
                    break
                }
            }
            if (result) {
                currentTokenText.set(currentTokenText.get() + " " + builder.tokenText)
                propertyOrClassExists = ParsableScriptSuiteRegistryHelper.isStdPropertyWithPrefixExist(
                    currentTokenText.get(),
                ) || ParsableScriptSuiteRegistryHelper.isStdClassWithPrefixExist(currentTokenText.get())
            }
            result = result && !propertyOrClassExists
            exit_section_(builder, marker, null, result)
            return result
        }

        private fun tryToParseApplicationConstant(
            builder: PsiBuilder,
            level: Int,
            applicationName: String,
        ): Boolean {
            if (!recursion_guard_(builder, level, "tryToParseApplicationConstant")) return false
            val currentTokenText = Ref<String>()
            currentTokenText.set(builder.tokenText ?: "")
            var result = false
            var propertyOrClassExists = false
            var constantWithPrefixExists = ParsableScriptSuiteRegistryHelper.isConstantWithPrefixExist(
                applicationName,
                currentTokenText.get(),
            )
            var nextTokenText = currentTokenText.get()
            val marker = enter_section_(builder)
            while (builder.tokenText != null && constantWithPrefixExists) {
                builder.advanceLexer()
                nextTokenText += " ${builder.tokenText}"
                constantWithPrefixExists = ParsableScriptSuiteRegistryHelper.isConstantWithPrefixExist(
                    applicationName,
                    nextTokenText,
                )
                if (constantWithPrefixExists) {
                    currentTokenText.set(nextTokenText)
                } else if (ParsableScriptSuiteRegistryHelper.isApplicationConstant(
                        applicationName,
                        currentTokenText.get(),
                    )
                ) {
                    result = true
                    break
                }
            }
            if (result) {
                propertyOrClassExists = ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
                    applicationName,
                    currentTokenText.get(),
                ) || ParsableScriptSuiteRegistryHelper.isClassWithPrefixExist(applicationName, currentTokenText.get())
                if (propertyOrClassExists) {
                    currentTokenText.set(currentTokenText.get() + " " + builder.tokenText)
                    propertyOrClassExists = ParsableScriptSuiteRegistryHelper.isPropertyWithPrefixExist(
                        applicationName,
                        currentTokenText.get(),
                    ) || ParsableScriptSuiteRegistryHelper.isClassWithPrefixExist(applicationName, currentTokenText.get())
                }
            }
            result = result && !propertyOrClassExists
            exit_section_(builder, marker, null, result)
            return result
        }
    }
}
