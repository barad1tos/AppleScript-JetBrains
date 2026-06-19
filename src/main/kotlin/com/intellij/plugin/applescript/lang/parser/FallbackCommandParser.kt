package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ABOUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AGAINST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FILE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GIVEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OVER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.UNDER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.USING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

internal object FallbackCommandParser {
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
                KnownFallbackCommandNameParser.parse(builder, parsedName) ||
                GenericFallbackCommandHeadParser.parse(builder, parsedName)
        }
        return result
    }
}

private object GenericFallbackCommandHeadParser {
    private val commandEntryPreviousTokens =
        TokenSet.create(
            NLS,
            THEN,
            TO,
        )

    private val commandSelectorStarts =
        TokenSet.create(
            WITH,
            WITHOUT,
            GIVEN,
            INTO,
            FROM,
            FOR,
            TO,
            ON,
            AS,
            USING,
            ABOUT,
            AGAINST,
            BY,
            OVER,
            UNDER,
        )

    // Permissive generic command head for unknown application/osax commands such as
    // `max width for labels ...`, `create rule ...`, `display enhanced alert ...`,
    // `quick search ...`, or `keystroke return ...`. This runs after the known-command
    // phrase parser, so built-in command phrases keep their precise parsing path. The fallback
    // accepts a leading identifier phrase only when a clear command tail follows; otherwise
    // it rolls back and leaves object-reference / plain-variable parsing unchanged.
    fun parse(
        builder: PsiBuilder,
        parsedName: Ref<String>,
    ): Boolean {
        var result = false
        if (isCommandLegalContext(builder)) {
            val marker = builder.mark()
            val words = parseHeadWords(builder)
            if (isAcceptedHead(words, builder)) {
                marker.drop()
                parsedName.set(words.joinToString(" "))
                configureParameterParsing(builder, words)
                result = true
            } else {
                marker.rollbackTo()
            }
        }
        return result
    }

    private fun parseHeadWords(builder: PsiBuilder): List<String> {
        val words = mutableListOf<String>()
        while (builder.tokenType === VAR_IDENTIFIER) {
            val shouldStartTail =
                isIdentifierBeforeSelectorStart(builder) ||
                    isObjectReferenceValueStart(builder)
            if (words.isNotEmpty() && shouldStartTail) {
                break
            }
            words += builder.tokenText.orEmpty()
            builder.advanceLexer()
        }
        return words
    }

    private fun configureParameterParsing(
        builder: PsiBuilder,
        words: List<String>,
    ) {
        if (words.size == 1 && FallbackCommandParameterParser.isStructuredDirectParameterStart(builder.tokenType)) {
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE,
                FallbackCommandParameterMode.OptionalDirectParameter,
            )
        } else {
            // Couple parameterMode to the acceptor: a single-word generic head needs permissive
            // tail parsing only when the tail is a label-run rather than a structured direct value.
            // FallbackCommandParameterParser.parseParameters reads and then restores it, so it
            // never leaks into sibling parses (Pitfall 1/2 stay gated).
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED, true)
        }
    }

    // Required-tail-shape gate (Pitfall 1): after the head run a CLEAR command tail must follow —
    // a preposition / FOR keyword, or a value start (literal / list / paren / `return` constant).
    // A head with no tail (NLS, operator, EOF) is NOT a command: roll back so the head falls
    // through to the object-reference / reference path exactly as before, and a genuine error
    // still surfaces.
    private fun isAcceptedHead(
        words: List<String>,
        builder: PsiBuilder,
    ): Boolean =
        words.isNotEmpty() &&
            !isSingleWordCoercionCandidate(words, builder) &&
            isCommandTailStart(builder)

    // Engage only at a command-entry position. Statement starts and tell-to bodies have no preceding
    // operand at this parser level, while mid-expression variables do. Also stay inactive while
    // another command's parameters are being parsed, which prevents fallback recursion from
    // reclassifying parameter values as nested commands.
    private fun isCommandLegalContext(builder: PsiBuilder): Boolean {
        val isInsideFallbackParameters =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS) == true
        val isInsideDictionaryParameters =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS) == true
        return !isInsideFallbackParameters &&
            !isInsideDictionaryParameters &&
            isCommandEntryBoundary(builder)
    }

    private fun isCommandEntryBoundary(builder: PsiBuilder): Boolean {
        val previousToken = AppleScriptParserTrivia.previousNonSpaceToken(builder)
        return previousToken == null || commandEntryPreviousTokens.contains(previousToken)
    }

    // Require a clear command tail after the candidate head: a parameter preposition,
    // `for`, or a value start. A bare identifier before NLS, EOF, or an operator is not
    // treated as a command head, so genuine syntax errors and plain variables still surface.
    private fun isCommandTailStart(builder: PsiBuilder): Boolean {
        val tokenType = builder.tokenType
        return tokenType != null &&
            (
                isPrepositionOrForStart(tokenType) ||
                    FallbackCommandParameterParser.isValueLiteralStart(tokenType) ||
                    isIdentifierBeforeSelectorStart(builder) ||
                    isObjectReferenceValueStart(builder)
            )
    }

    private fun isIdentifierBeforeSelectorStart(builder: PsiBuilder): Boolean =
        builder.tokenType === VAR_IDENTIFIER && isPrepositionOrForStart(builder.lookAhead(1))

    private fun isObjectReferenceValueStart(builder: PsiBuilder): Boolean =
        builder.tokenType === VAR_IDENTIFIER && builder.lookAhead(1) === OF

    private fun isSingleWordCoercionCandidate(
        words: List<String>,
        builder: PsiBuilder,
    ): Boolean = words.size == 1 && builder.tokenType === AS

    private fun isPrepositionOrForStart(tokenType: IElementType?): Boolean =
        tokenType != null && commandSelectorStarts.contains(tokenType)
}

private object KnownFallbackCommandNameParser {
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
