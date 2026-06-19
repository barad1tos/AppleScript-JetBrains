package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FILE

internal object KnownFallbackCommandNameParser {
    private val commandPhrases =
        listOf(
            listOf("choose", "from", "list"),
            listOf("display", "dialog"),
            listOf("display", "alert"),
            listOf("display", "notification"),
            listOf("do", "shell", "script"),
            listOf("run", "script"),
        )

    fun parse(
        builder: PsiBuilder,
        parsedName: Ref<String>,
    ): Boolean =
        parseKnownCommandPhrase(builder, parsedName) ||
            parseSingleWordCommand(builder, parsedName)

    private fun parseKnownCommandPhrase(
        builder: PsiBuilder,
        parsedName: Ref<String>,
    ): Boolean = commandPhrases.any { phrase -> parseCommandPhrase(builder, parsedName, phrase) }

    private fun parseCommandPhrase(
        builder: PsiBuilder,
        parsedName: Ref<String>,
        words: List<String>,
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
        builder.putUserData(
            AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE,
            FallbackCommandParameterMode.OptionalDirectParameter,
        )
        return true
    }

    private fun parseSingleWordCommand(
        builder: PsiBuilder,
        parsedName: Ref<String>,
    ): Boolean {
        val commandName = builder.tokenText
        val isReadWithoutFile = commandName == "read" && builder.lookAhead(1) !== FILE
        val result = commandName != null && !isReadWithoutFile && isWellKnownSingleWordCommand(commandName)
        if (result) {
            parsedName.set(commandName)
            builder.advanceLexer()
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE,
                fallbackParameterMode(commandName),
            )
        }
        return result
    }

    private fun fallbackParameterMode(commandName: String): FallbackCommandParameterMode =
        if (commandName.equals("make", ignoreCase = true)) {
            FallbackCommandParameterMode.ParametersOnly
        } else {
            FallbackCommandParameterMode.OptionalDirectParameter
        }

    private fun isWellKnownSingleWordCommand(commandName: String): Boolean =
        commandName == "read" ||
            commandName == "move" ||
            commandName == "exists" ||
            commandName == "run" ||
            commandName == "delete" ||
            commandName == "make" ||
            commandName == "execute"
}
