package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS

internal object CommandHandlerParameterParser {
    private val FALLBACK_FIRST_COMMAND_NAMES: Set<String> =
        setOf(
            "choose from list",
            "make",
            "write",
        )

    fun parseFallbackOrDictionaryCommandParameters(
        builder: PsiBuilder,
        level: Int,
        parsedCommandName: String,
        allCommandsWithName: List<AppleScriptCommand>,
    ): Boolean =
        if (allCommandsWithName.isEmpty()) {
            parseUnknownCommandParameters(builder, level, parsedCommandName)
        } else if (isFallbackFirstCommand(parsedCommandName)) {
            parseFallbackFirstDictionaryCommandParameters(
                builder,
                level,
                parsedCommandName,
                allCommandsWithName,
            )
        } else {
            parseDictionaryCommandParameters(builder, level, allCommandsWithName)
        }

    fun parseDictionaryCommandParameters(
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

    private fun parseUnknownCommandParameters(
        builder: PsiBuilder,
        level: Int,
        parsedCommandName: String,
    ): Boolean {
        val previousMode =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE)
        val parserProvidedMode =
            when {
                FallbackCommandParameterParser.isStructuredDirectParameterStart(builder.tokenType) ->
                    FallbackCommandParameterMode.OptionalDirectParameter
                FallbackCommandParameterParser.isIdentifierPhraseDirectParameterStart(builder) ->
                    FallbackCommandParameterMode.OptionalDirectParameter
                else -> previousMode
            }
        builder.putUserData(
            AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE,
            parserProvidedMode,
        )
        return try {
            FallbackCommandParameterParser.parseParameters(builder, level + 1, parsedCommandName)
        } finally {
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE,
                previousMode,
            )
        }
    }

    private fun isFallbackFirstCommand(commandName: String): Boolean =
        commandName.lowercase() in FALLBACK_FIRST_COMMAND_NAMES

    private fun parseFallbackFirstDictionaryCommandParameters(
        builder: PsiBuilder,
        level: Int,
        parsedCommandName: String,
        allCommandsWithName: List<AppleScriptCommand>,
    ): Boolean {
        val previousMode =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE)
        val previousNames =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_NAMES)
        builder.putUserData(
            AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE,
            fallbackParameterMode(builder, allCommandsWithName),
        )
        builder.putUserData(
            AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_NAMES,
            fallbackParameterNames(allCommandsWithName),
        )
        return try {
            FallbackCommandParameterParser.parseParameters(builder, level + 1, parsedCommandName)
        } finally {
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE,
                previousMode,
            )
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_NAMES,
                previousNames,
            )
        }
    }

    private fun fallbackParameterMode(
        builder: PsiBuilder,
        allCommandsWithName: List<AppleScriptCommand>,
    ): FallbackCommandParameterMode =
        if (isDictionaryParameterSelectorStart(builder, allCommandsWithName)) {
            FallbackCommandParameterMode.ParametersOnly
        } else {
            FallbackCommandParameterMode.OptionalDirectParameter
        }

    private fun isDictionaryParameterSelectorStart(
        builder: PsiBuilder,
        allCommandsWithName: List<AppleScriptCommand>,
    ): Boolean {
        val tokenText = builder.tokenText ?: return false
        return allCommandsWithName.any { command ->
            command.parameters.any { parameter ->
                parameter
                    .getName()
                    .substringBefore(' ')
                    .equals(tokenText, ignoreCase = true)
            }
        }
    }

    private fun fallbackParameterNames(allCommandsWithName: List<AppleScriptCommand>): Set<String> =
        allCommandsWithName
            .flatMap { command -> command.parameters.map { parameter -> parameter.getName() } }
            .toSet()

    private fun isIncompleteHandlerCall(
        allCommandsWithName: List<AppleScriptCommand>,
        builder: PsiBuilder,
    ): Boolean =
        allCommandsWithName.isNotEmpty() &&
            (builder.tokenType === NLS || builder.eof())
}
