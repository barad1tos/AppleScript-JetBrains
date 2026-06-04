package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.AppleScriptNames
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_COMMAND_NAME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ITEM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.REFERENCE_EXPRESSION
import java.util.Stack

class AppleScriptGeneratedParserUtil : GeneratedParserUtilBase() {
    @Suppress("TooManyFunctions")
    companion object {
        internal val PARSING_COMMAND_HANDLER_CALL_PARAMETERS: Key<Boolean> =
            Key.create("applescript.parsing.command.handler.parameters")
        private val PARSING_COMMAND_ASSIGNMENT_STATEMENT: Key<Boolean> =
            Key.create("applescript.parsing.assignment.statement")
        internal val PARSING_COMMAND_HANDLER_BOOLEAN_PARAMETER: Key<Boolean> =
            Key.create("applescript.parsing.command.handler.boolean.parameter")

        @JvmField
        val TOLD_APPLICATION_NAME_STACK: Key<Stack<String>> =
            Key.create("applescript.parsing.current.dictionary.name.stack")

        // Follow-up: wire application-id dictionary lookup after the lint cleanup pass.
        @Suppress("unused")
        @JvmField
        val TOLD_APPLICATION_ID_STACK: Key<Stack<String>> =
            Key.create("applescript.parsing.current.dictionary.id.stack")

        internal val PARSING_TELL_SIMPLE_STATEMENT: Key<Boolean> =
            Key.create("applescript.parsing.tell.simple.statement")
        internal val PARSING_TELL_SIMPLE_OBJECT_REF: Key<Boolean> =
            Key.create("applescript.parsing.tell.simple.object.ref")
        internal val PARSING_TELL_COMPOUND_STATEMENT: Key<Boolean> =
            Key.create("applescript.parsing.tell.simple.statement")
        internal val APPLICATION_NAME_PUSHED: Key<Boolean> =
            Key.create("applescript.parsing.tell.statement.application.name.pushed")
        internal val USED_APPLICATION_NAMES: Key<Set<String>> =
            Key.create("applescript.parsing.use.statement.application.name.set")
        internal val WAS_USE_STATEMENT_USED: Key<Boolean> =
            Key.create("applescript.parsing.is.use.statement.used")
        internal val IS_PARSING_USING_TERMS_FROM_STATEMENT: Key<Boolean> =
            Key.create("applescript.parsing.is.use.statement.used")
        private val PARSING_LITERAL_EXPRESSION: Key<Boolean> =
            Key.create("applescript.parsing.literal.expression")

        @JvmStatic
        fun parseCommandHandlerCallExpression(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = CommandHandlerCallParser.parseCallExpression(builder, level)

        @JvmStatic
        fun parseApplicationHandlerDefinitionSignature(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = CommandHandlerCallParser.parseApplicationHandlerDefinitionSignature(builder, level)

        @JvmStatic
        fun isHandlerLabeledParametersCallAllowed(
            builder: PsiBuilder,
            level: Int,
        ): Boolean =
            recursion_guard_(builder, level, "isHandlerLabeledParametersCallAllowed") &&
                builder.getUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT) != true &&
                builder.getUserData(PARSING_TELL_SIMPLE_OBJECT_REF) != true &&
                builder.getUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS) != true

        @JvmStatic
        fun isTreePrevSimpleReference(
            builder: PsiBuilder,
            level: Int,
        ): Boolean {
            if (!recursion_guard_(builder, level, "isTreePrevSimpleReference")) return false
            val latestDoneMarker = builder.latestDoneMarker
            return latestDoneMarker != null &&
                latestDoneMarker.tokenType === REFERENCE_EXPRESSION
        }

        @JvmStatic
        fun parseAssignmentStatementInner(
            builder: PsiBuilder,
            level: Int,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseAssignmentStatementInner")) return false
            builder.putUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT, true)
            val result = AppleScriptParser.assignmentStatement(builder, level + 1)
            builder.putUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT, false)
            return result
        }

        @JvmStatic
        fun parseLiteralExpression(
            builder: PsiBuilder,
            level: Int,
            literalExpression: Parser,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseLiteralExpression")) return false
            builder.putUserData(PARSING_LITERAL_EXPRESSION, true)
            val result = literalExpression.parse(builder, level + 1)
            builder.putUserData(PARSING_LITERAL_EXPRESSION, false)
            return result
        }

        @JvmStatic
        fun parseTellSimpleStatementInner(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = TellStatementParser.parseSimpleStatement(builder, level)

        @JvmStatic
        fun parseTellSimpleObjectReference(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = TellStatementParser.parseSimpleObjectReference(builder, level)

        @JvmStatic
        fun parseUsedApplicationNameExternal(
            builder: PsiBuilder,
            level: Int,
            isImporting: Parser,
        ): Boolean = UseStatementParser.parseUsedApplicationNameExternal(builder, level, isImporting)

        @JvmStatic
        fun parseUseStatement(
            builder: PsiBuilder,
            level: Int,
            useStatement: Parser,
        ): Boolean = UseStatementParser.parseUseStatement(builder, level, useStatement)

        @JvmStatic
        fun parseExpression(
            builder: PsiBuilder,
            level: Int,
            dictionaryTermToken: String,
            expression: Parser,
        ): Boolean = TellStatementParser.parseExpression(builder, level, dictionaryTermToken, expression)

        @JvmStatic
        fun parseTellCompoundStatement(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = TellStatementParser.parseCompoundStatement(builder, level)

        @JvmStatic
        fun parseUsingTermsFromStatement(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = TellStatementParser.parseUsingTermsFromStatement(builder, level)

        @JvmStatic
        fun pushStdLibrary(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = UseStatementParser.pushStdLibrary(builder, level)

        @JvmStatic
        fun parseApplicationName(
            builder: PsiBuilder,
            level: Int,
            tellStatementStartCondition: Parser,
        ): Boolean = UseStatementParser.parseApplicationName(builder, level, tellStatementStartCondition)

        @JvmStatic
        fun isTellStatementStart(
            builder: PsiBuilder,
            level: Int,
        ): Boolean =
            recursion_guard_(builder, level, "isTellStatementStart") &&
                TellStatementParser.isTellStatementStart(builder)

        @JvmStatic
        fun typeSpecifier(
            builder: PsiBuilder,
            level: Int,
        ): Boolean {
            if (!recursion_guard_(builder, level, "typeSpecifier")) return false
            val marker = enter_section_(builder)
            var result = singularClassName(builder, level + 1)
            if (!result) result = AppleScriptParser.builtInClassIdentifierPlural(builder, level + 1)
            if (!result) result = AppleScriptParser.dictionaryClassIdentifierPlural(builder, level + 1)
            exit_section_(builder, marker, null, result)
            return result
        }

        @JvmStatic
        fun singularClassName(
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

        @JvmStatic
        fun parseCommandParameterSelector(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = FallbackCommandParameterParser.parseParameterSelector(builder, level)

        @JvmStatic
        fun isPossessivePronoun(
            builder: PsiBuilder,
            level: Int,
        ): Boolean =
            recursion_guard_(builder, level, "isPossessivePronoun") &&
                PossessivePronounParser.isPossessivePronoun(builder)

        @JvmStatic
        fun parseDictionaryCommandName(
            builder: PsiBuilder,
            level: Int,
        ): Boolean {
            if (nextTokenIs(builder, NLS)) return false
            val parsedCommandName = Ref<String>()
            val toldApplicationName = ParserApplicationNameStack.getTargetApplicationName(builder)
            val areThereUseStatements = builder.getUserData(WAS_USE_STATEMENT_USED) == true
            val applicationsToImport =
                if (areThereUseStatements) {
                    builder.getUserData(USED_APPLICATION_NAMES)
                } else {
                    null
                }
            val marker = enter_section_(builder, level, _COLLAPSE_, "<parse ApplicationDictionary Command Name>")
            val result =
                DictionaryCommandNameParser.parseName(
                    builder,
                    level + 1,
                    parsedCommandName,
                    DictionaryCommandLookupScope(toldApplicationName, areThereUseStatements, applicationsToImport),
                )
            exit_section_(builder, level, marker, DICTIONARY_COMMAND_NAME, result, false, null)
            return result
        }

        @JvmStatic
        fun parseIncompleteCommandCall(
            builder: PsiBuilder,
            level: Int,
        ): Boolean = parseDictionaryCommandName(builder, level + 1)

        @JvmStatic
        fun parseDictionaryPropertyName(
            builder: PsiBuilder,
            level: Int,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseDictionaryPropertyInner")) return false
            val toldApplicationName = ParserApplicationNameStack.getTargetApplicationName(builder)
            val areThereUseStatements = builder.getUserData(WAS_USE_STATEMENT_USED) == true
            val applicationsToImportFrom =
                if (areThereUseStatements) {
                    builder.getUserData(USED_APPLICATION_NAMES)
                } else {
                    null
                }

            return DictionaryTermLookupParser.parsePropertyName(
                builder,
                level + 1,
                DictionaryTermLookupScope(
                    toldApplicationName,
                    areThereUseStatements,
                    applicationsToImportFrom,
                ),
            )
        }

        @JvmStatic
        fun parseDictionaryClassName(
            builder: PsiBuilder,
            level: Int,
            isPluralForm: Boolean,
            checkForUseStatements: Parser,
        ): Boolean {
            var result = false
            val tokenText = builder.tokenText
            if (recursion_guard_(builder, level, "parseDictionaryClassName") &&
                !tokenText.isNullOrEmpty() &&
                AppleScriptNames.isIdentifierStart(tokenText[0])
            ) {
                val toldApplicationName = ParserApplicationNameStack.getTargetApplicationName(builder)
                val areThereUseStatements = checkForUseStatements.parse(builder, level + 1)
                val applicationsToImportFrom =
                    if (areThereUseStatements) {
                        builder.getUserData(USED_APPLICATION_NAMES)
                    } else {
                        null
                    }
                result =
                    DictionaryTermLookupParser.parseClassName(
                        builder,
                        level + 1,
                        isPluralForm,
                        DictionaryTermLookupScope(
                            toldApplicationName,
                            areThereUseStatements,
                            applicationsToImportFrom,
                        ),
                    )
            }
            return result
        }

        @JvmStatic
        fun parseCheckForUseStatements(
            builder: PsiBuilder,
            level: Int,
        ): Boolean =
            recursion_guard_(builder, level, "parseCheckForUseStatements") &&
                builder.getUserData(WAS_USE_STATEMENT_USED) == true

        @JvmStatic
        fun parseCommandParametersExpression(
            builder: PsiBuilder,
            level: Int,
        ): Boolean {
            if (!recursion_guard_(builder, level, "parseCommandParametersExpression")) return false
            builder.putUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS, true)
            val result = AppleScriptParser.expression(builder, level + 1)
            builder.putUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS, false)
            return result
        }

        @JvmStatic
        fun parseDictionaryConstant(
            builder: PsiBuilder,
            level: Int,
        ): Boolean {
            var result = false
            if (recursion_guard_(builder, level, "parseDictionaryConstant")) {
                val toldApplicationName = ParserApplicationNameStack.getTargetApplicationName(builder)
                val insideExpression =
                    builder.getUserData(PARSING_COMMAND_HANDLER_CALL_PARAMETERS) == true ||
                        builder.getUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT) == true ||
                        builder.getUserData(PARSING_LITERAL_EXPRESSION) == true
                if (ApplicationDictionary.COCOA_STANDARD_LIBRARY == toldApplicationName || insideExpression) {
                    val areThereUseStatements = builder.getUserData(WAS_USE_STATEMENT_USED) == true
                    val applicationsToImportFrom =
                        if (areThereUseStatements) {
                            builder.getUserData(USED_APPLICATION_NAMES)
                        } else {
                            null
                        }

                    result =
                        DictionaryTermLookupParser.parseConstant(
                            builder,
                            level + 1,
                            insideExpression,
                            DictionaryTermLookupScope(
                                toldApplicationName,
                                areThereUseStatements,
                                applicationsToImportFrom,
                            ),
                        )
                }
            }
            return result
        }
    }
}
