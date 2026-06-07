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
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OVER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.UNDER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.USING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

internal object FallbackCommandParser {
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

    private val commandPhrases =
        listOf(
            listOf("choose", "from", "list"),
            listOf("display", "dialog"),
            listOf("display", "alert"),
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
                parseSingleWordCommand(builder, parsedName) ||
                parseGenericPermissiveHead(builder, level, parsedName)
        }
        return result
    }

    // Permissive generic command head for unknown application/osax commands such as
    // `max width for labels ...`, `create rule ...`, `display enhanced alert ...`,
    // `quick search ...`, or `keystroke return ...`. This runs after the hardcoded
    // allowlist, so known command phrases keep their precise parsing path. The fallback
    // accepts a leading identifier phrase only when a clear command tail follows; otherwise
    // it rolls back and leaves object-reference / plain-variable parsing unchanged.
    private fun parseGenericPermissiveHead(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
    ): Boolean {
        if (!isCommandLegalContext(builder, level)) return false

        val marker = builder.mark()
        val words = mutableListOf<String>()
        // Head = the leading VAR_IDENTIFIER words (`display enhanced alert`,
        // `quick search`, `display notification`, `create rule`, `max width`, `keystroke`). The
        // trailing value/label words that are ALSO VAR_IDENTIFIER (e.g. `theStrings`) are
        // indistinguishable without a loaded dictionary, so they are drained generously by the
        // permissive tail consumer rather than split out here.
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
        // Required-tail-shape gate (Pitfall 1): after the head run a CLEAR command tail must follow —
        // a preposition / FOR keyword, or a value start (literal / list / paren / `return` constant).
        // A head with no tail (NLS, operator, EOF) is NOT a command: roll back so the head falls
        // through to the object-reference / reference path exactly as before, and a genuine error
        // still surfaces.
        if (words.isEmpty() || isSingleWordCoercionCandidate(words, builder) || !isCommandTailStart(builder)) {
            marker.rollbackTo()
            return false
        }
        marker.drop()
        parsedName.set(words.joinToString(" "))
        // Couple parameterMode to the acceptor: a single-word generic head needs OptionalDirectParameter,
        // which parameterMode grants only while this flag is set. FallbackCommandParameterParser.parseParameters
        // reads and then restores it, so it never leaks into sibling parses (Pitfall 1/2 stay gated).
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED, true)
        return true
    }

    // Engage only at a command-entry position. Statement starts and tell-to bodies have no preceding
    // operand at this parser level, while mid-expression variables do. Also stay inactive while
    // another command's parameters are being parsed, which prevents fallback recursion from
    // reclassifying parameter values as nested commands.
    private fun isCommandLegalContext(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS) == true) {
            return false
        }
        if (builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS) == true) {
            return false
        }
        return !AppleScriptGeneratedParserCommandHooks.isTreePrevSimpleReference(builder, level)
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
            commandName == "make" ||
            commandName == "execute"
}
