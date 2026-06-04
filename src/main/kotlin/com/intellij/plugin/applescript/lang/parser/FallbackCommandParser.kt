package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FILE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER

internal object FallbackCommandParser {
    private val commandPhrases =
        listOf(
            listOf("choose", "from", "list"),
            listOf("display", "dialog"),
            listOf("do", "shell", "script"),
            listOf("run", "script"),
        )

    fun parseCommandName(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseWellKnownCommandFallback") &&
            builder.tokenType === VAR_IDENTIFIER
        ) {
            result =
                parseKnownCommandPhrase(builder, parsedName) ||
                parseSingleWordCommand(builder, parsedName)
        }
        return result
    }

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
        }
        return result
    }

    private fun isWellKnownSingleWordCommand(commandName: String): Boolean =
        commandName == "read" ||
            commandName == "move" ||
            commandName == "exists" ||
            commandName == "run" ||
            commandName == "delete" ||
            commandName == "make"
}
