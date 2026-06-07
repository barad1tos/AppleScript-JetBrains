package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._COLLAPSE_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.AppleScriptNames
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_PROPERTY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COUNT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_COMMAND_NAME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SET
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

internal object CommandHandlerCallParser {
    fun parseCallExpression(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseCommandHandlerCallExpression") &&
            isCommandHandlerCallStart(builder)
        ) {
            val parsedCommandName = Ref<String>()
            val lookupScope = commandLookupScope(builder)
            val commandNameResult = parseCommandNameSection(builder, level, parsedCommandName, lookupScope)

            if (commandNameResult) {
                val allCommandsWithName =
                    DictionaryCommandCollector.collectCommands(builder, parsedCommandName.get(), lookupScope)
                result =
                    parseFallbackOrDictionaryCommandParameters(
                        builder,
                        level,
                        parsedCommandName.get(),
                        allCommandsWithName,
                    )
            }
        }
        return result
    }

    fun parseApplicationHandlerDefinitionSignature(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseApplicationHandlerDefinitionSignature") &&
            builder.getUserData(AppleScriptGeneratedParserUtil.IS_PARSING_USING_TERMS_FROM_STATEMENT) == true &&
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_TELL_COMPOUND_STATEMENT) != true
        ) {
            val parsedCommandName = Ref<String>()
            val lookupScope =
                DictionaryCommandLookupScope(
                    ParserApplicationNameStack.getTargetApplicationName(builder),
                    true,
                    null,
                )
            val commandNameResult = parseApplicationHandlerCommandName(builder, level, parsedCommandName, lookupScope)

            if (commandNameResult) {
                val allCommandsWithName =
                    DictionaryCommandCollector.collectCommands(
                        builder,
                        parsedCommandName.get(),
                        DictionaryCommandLookupScope(lookupScope.toldApplicationName, false, null),
                    )
                result = parseDictionaryCommandParameters(builder, level, allCommandsWithName)
            }
        }
        return result
    }

    private fun isCommandHandlerCallStart(builder: PsiBuilder): Boolean {
        val tokenText = builder.tokenText
        return !nextTokenIs(builder, NLS) &&
            builder.tokenType !== COUNT &&
            !isAssignmentObjectOperandBeforeTerminator(builder) &&
            !isAssignmentTargetPhraseBeforeTerminator(builder) &&
            !tokenText.isNullOrEmpty() &&
            AppleScriptNames.isIdentifierStart(tokenText[0])
    }

    private fun isAssignmentObjectOperandBeforeTerminator(builder: PsiBuilder): Boolean =
        builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_ASSIGNMENT_STATEMENT) == true &&
            builder.lookAhead(1) === TO &&
            isObjectPointer(previousNonSpaceToken(builder))

    private fun isAssignmentTargetPhraseBeforeTerminator(builder: PsiBuilder): Boolean =
        builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_ASSIGNMENT_STATEMENT) == true &&
            isAssignmentTargetIntroducer(previousNonSpaceToken(builder)) &&
            hasAssignmentTerminatorAfterTargetPhrase(builder)

    private fun isAssignmentTargetIntroducer(tokenType: IElementType?): Boolean =
        tokenType === SET || isObjectPointer(tokenType)

    private fun hasAssignmentTerminatorAfterTargetPhrase(builder: PsiBuilder): Boolean {
        var offset = 0
        var tokenType = builder.tokenType
        var consumedTargetWord = false
        while (isAssignmentTargetWord(tokenType)) {
            consumedTargetWord = true
            offset += 1
            tokenType = builder.lookAhead(offset)
        }
        return consumedTargetWord && tokenType === TO
    }

    private fun isAssignmentTargetWord(tokenType: IElementType?): Boolean =
        tokenType === VAR_IDENTIFIER ||
            tokenType === BUILT_IN_PROPERTY ||
            tokenType === SET ||
            FallbackDictionaryTermPredicates.isContextualPropertyTerm(tokenType)

    private fun previousNonSpaceToken(builder: PsiBuilder): IElementType? {
        var index = -1
        var tokenType = builder.rawLookup(index)
        while (tokenType === TokenType.WHITE_SPACE) {
            tokenType = builder.rawLookup(--index)
        }
        return tokenType
    }

    private fun isObjectPointer(tokenType: IElementType?): Boolean = tokenType === OF || tokenType === IN

    private fun commandLookupScope(builder: PsiBuilder): DictionaryCommandLookupScope {
        val areThereUseStatements =
            builder.getUserData(AppleScriptGeneratedParserUtil.WAS_USE_STATEMENT_USED) == true
        val applicationsToImport =
            if (areThereUseStatements) {
                builder.getUserData(AppleScriptGeneratedParserUtil.USED_APPLICATION_NAMES)
            } else {
                null
            }
        return DictionaryCommandLookupScope(
            ParserApplicationNameStack.getTargetApplicationName(builder),
            areThereUseStatements,
            applicationsToImport,
        )
    }

    private fun parseCommandNameSection(
        builder: PsiBuilder,
        level: Int,
        parsedCommandName: Ref<String>,
        lookupScope: DictionaryCommandLookupScope,
    ): Boolean {
        val commandNameMarker =
            enter_section_(
                builder,
                level,
                _COLLAPSE_,
                "<parse Command Handler Call Expression>",
            )
        val commandNameResult =
            DictionaryCommandNameParser.parseName(
                builder,
                level + 1,
                parsedCommandName,
                lookupScope,
            )
        exit_section_(builder, level, commandNameMarker, DICTIONARY_COMMAND_NAME, commandNameResult, false, null)
        return commandNameResult
    }

    private fun parseApplicationHandlerCommandName(
        builder: PsiBuilder,
        level: Int,
        parsedCommandName: Ref<String>,
        lookupScope: DictionaryCommandLookupScope,
    ): Boolean {
        val commandNameMarker =
            enter_section_(builder, level, _COLLAPSE_, "<parse Application Handler Definition")
        val commandNameResult =
            DictionaryCommandNameParser.parseName(
                builder,
                level + 1,
                parsedCommandName,
                lookupScope,
            )
        exit_section_(builder, level, commandNameMarker, DICTIONARY_COMMAND_NAME, commandNameResult, false, null)
        return commandNameResult
    }

    private fun parseFallbackOrDictionaryCommandParameters(
        builder: PsiBuilder,
        level: Int,
        parsedCommandName: String,
        allCommandsWithName: List<AppleScriptCommand>,
    ): Boolean =
        if (allCommandsWithName.isEmpty() || isFallbackFirstCommand(parsedCommandName)) {
            FallbackCommandParameterParser.parseParameters(builder, level + 1, parsedCommandName)
        } else {
            parseDictionaryCommandParameters(builder, level, allCommandsWithName)
        }

    // `make` routes through the fallback parameter parser even when a loaded standard dictionary
    // defines it: the dictionary command consumes app-specific multi-word noun phrases (e.g.
    // `make new lock screen task`) too narrowly and drops the trailing nouns. Promote to a
    // Set<String> if a second command ever needs the same fallback-first treatment.
    private fun isFallbackFirstCommand(commandName: String): Boolean = commandName.equals("make", ignoreCase = true)

    private fun parseDictionaryCommandParameters(
        builder: PsiBuilder,
        level: Int,
        allCommandsWithName: List<AppleScriptCommand>,
    ): Boolean {
        val hasAcceptedCommandSection =
            allCommandsWithName.any { command ->
                DictionaryCommandParameterParser.parseParametersForCommand(builder, level + 1, command)
            }
        return hasAcceptedCommandSection ||
            isIncompleteHandlerCall(allCommandsWithName, builder)
    }

    private fun isIncompleteHandlerCall(
        allCommandsWithName: List<AppleScriptCommand>,
        builder: PsiBuilder,
    ): Boolean =
        allCommandsWithName.isNotEmpty() &&
            (builder.tokenType === NLS || builder.eof())
}
