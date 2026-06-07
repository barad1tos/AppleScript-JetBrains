package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ABOUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AFTER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AGAINST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_TYPE_S
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CURRENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIGITS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DOT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FALSE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GIVEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MISSING_VALUE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OVER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RETURN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TEXT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TRUE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.UNDER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.USING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT
import com.intellij.psi.tree.IElementType

internal object FallbackCommandParameterParser {
    private val KNOWN_FALLBACK_COMMAND_NAMES: Set<String> =
        setOf(
            "choose from list",
            "delete",
            "display dialog",
            "do shell script",
            "execute",
            "exists",
            "make",
            "move",
            "read",
            "run",
            "run script",
            "write",
        )

    private enum class ParameterMode {
        OptionalDirectParameter,
        ParametersOnly,
    }

    fun parseParameters(
        builder: PsiBuilder,
        level: Int,
        commandName: String?,
    ): Boolean {
        val mode = commandName?.let { parameterMode(it, builder) } ?: return false
        // The generic-head coupling flag is single-use: capture then consume it here so it never leaks
        // into the direct-parameter expression parse or a sibling command on the same line.
        val isPermissiveHead =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED) == true &&
                commandName.lowercase() !in KNOWN_FALLBACK_COMMAND_NAMES
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED, null)
        val previousFallbackContext =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS)
        // Mark the fallback context BEFORE the direct parameter so the generic permissive head cannot
        // re-engage on a parameter VALUE (`display dialog "Hello" default answer …` — the nested
        // `default answer` must NOT be hijacked as a command head inside the direct-parameter expression).
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS, true)
        try {
            if (isPermissiveHead) {
                // Unknown osax/application commands can carry heterogeneous tails that the strict
                // selector loop does not model: keyword-noun labels (`width for labels`,
                // `placeholder text`), the `return` constant, and bracketed values. Drain the
                // whole tail to the statement boundary so the unknown command does not leave a
                // dangling PsiErrorElement. Recognized fallback commands keep precise selector PSI.
                drainPermissiveCommandTail(builder, level + 1)
            } else {
                if (mode == ParameterMode.OptionalDirectParameter) {
                    parseOptionalDirectParameter(builder, level + 1)
                }
                parseCommandParameters(builder, level + 1)
            }
        } finally {
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS,
                previousFallbackContext,
            )
        }
        return true
    }

    // Consume residual unknown-command tail tokens until the statement boundary, wrapping each
    // chunk in COMMAND_PARAMETER. Every iteration advances at least one token, so it terminates.
    // Object-reference continuations (`of`, `whose`, `thru`) are not command-tail starts and remain
    // available to the surrounding object-reference grammar.
    private fun drainPermissiveCommandTail(
        builder: PsiBuilder,
        level: Int,
    ) {
        while (!builder.eof() && isPermissiveTailStart(builder.tokenType)) {
            val marker = enter_section_(builder, level, _NONE_, "<permissive command parameter>")
            val consumed = consumePermissiveParameterChunk(builder, level + 1)
            exit_section_(builder, level, marker, COMMAND_PARAMETER, consumed, false, null)
            if (!consumed) break
        }
    }

    private fun consumePermissiveParameterChunk(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        var advanced = false
        // Selector words: a preposition, the FOR/AS/USING keyword glue, or a bare VAR_IDENTIFIER /
        // keyword-noun label run (`rule width`, `width for labels`, `placeholder text`, `sound name`).
        // Stop before a bracket so a `{…}` / `(…)` value is consumed as the chunk's value, not a label.
        while (isPermissiveSelectorWord(builder.tokenType) && !isObjectReferenceValueStart(builder)) {
            builder.advanceLexer()
            advanced = true
        }
        // Value: a bracketed list / parenthesized value (depth-matched so it always balances), or a
        // literal / reference expression. The bracket matcher avoids relying on the full expression
        // parser for `{command down}`-style modifier lists, which it does not consume cleanly here.
        when {
            builder.tokenType === LCURLY || builder.tokenType === LPAREN -> {
                if (!consumeBracketedValue(builder)) return false
                advanced = true
            }
            isValueLiteralStart(builder.tokenType) || builder.tokenType === VAR_IDENTIFIER -> {
                val before = builder.currentOffset
                val parsed = AppleScriptParser.expression(builder, level + 1)
                if (!parsed || builder.currentOffset == before) {
                    builder.advanceLexer()
                }
                advanced = true
            }
            !advanced && isPermissiveTailStart(builder.tokenType) -> {
                builder.advanceLexer()
                advanced = true
            }
        }
        return advanced
    }

    // Depth-matched consumption of a `{...}` list or `(...)` group value. Returning false on an
    // unterminated bracket keeps the parser diagnostic visible and prevents the fallback from
    // swallowing following statements as command-tail text.
    private fun consumeBracketedValue(builder: PsiBuilder): Boolean {
        val expectedClosers = mutableListOf<IElementType>()
        while (!builder.eof()) {
            val token = builder.tokenType
            when (token) {
                LCURLY -> expectedClosers += RCURLY
                LPAREN -> expectedClosers += RPAREN
                RCURLY, RPAREN -> {
                    if (expectedClosers.isEmpty()) return false
                    val expectedCloser = expectedClosers.removeAt(expectedClosers.lastIndex)
                    if (expectedCloser !== token) return false
                }
                NLS -> if (expectedClosers.isNotEmpty()) return false
            }
            builder.advanceLexer()
            if (expectedClosers.isEmpty()) return true
        }
        return false
    }

    private fun isPermissiveSelectorWord(tokenType: IElementType?): Boolean =
        isPrepositionParameterStart(tokenType) ||
            tokenType === FOR ||
            tokenType === AS ||
            tokenType === USING ||
            isPermissiveKeywordSelectorWord(tokenType) ||
            tokenType === VAR_IDENTIFIER

    private fun isPermissiveKeywordSelectorWord(tokenType: IElementType?): Boolean =
        // Some selector words lex as AppleScript keywords rather than identifiers, but are safe here
        // because this predicate is used only after an unknown command head has already been accepted.
        tokenType === AFTER || tokenType === TEXT

    private fun isObjectReferenceValueStart(builder: PsiBuilder): Boolean =
        builder.tokenType === VAR_IDENTIFIER && builder.lookAhead(1) === OF

    // Tokens that can continue an unknown-command tail. Statement terminators and object-reference
    // continuations stay excluded so the drain stops at the correct boundary.
    private fun isPermissiveTailStart(tokenType: IElementType?): Boolean =
        tokenType != null &&
            tokenType !== NLS &&
            tokenType !== COMMENT &&
            tokenType !== OF &&
            (
                isPermissiveSelectorWord(tokenType) ||
                    isValueLiteralStart(tokenType)
            )

    private fun parameterMode(
        commandName: String,
        builder: PsiBuilder,
    ): ParameterMode? =
        when (commandName.lowercase()) {
            "make" -> ParameterMode.ParametersOnly
            "choose from list",
            "display dialog",
            "do shell script",
            "run script",
            "read",
            "move",
            "exists",
            "run",
            "delete",
            "execute",
            "write",
            -> ParameterMode.OptionalDirectParameter
            else -> {
                // Multi-word names always take an optional direct parameter. A single-word unknown
                // command head gets one only when the permissive command-name fallback accepted it;
                // otherwise the fallback declines and leaves the normal parse path untouched.
                when {
                    commandName.contains(" ") -> ParameterMode.OptionalDirectParameter
                    builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED) == true ->
                        ParameterMode.OptionalDirectParameter
                    else -> null
                }
            }
        }

    // Literal/value starts accepted directly after an unknown command head, before any selector word.
    fun isValueLiteralStart(tokenType: IElementType?): Boolean =
        tokenType === STRING_LITERAL ||
            tokenType === DIGITS ||
            tokenType === DOT ||
            tokenType === LCURLY ||
            tokenType === LPAREN ||
            tokenType === RETURN ||
            tokenType === TRUE ||
            tokenType === FALSE ||
            tokenType === MISSING_VALUE ||
            tokenType === CURRENT

    private fun parseOptionalDirectParameter(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        consumeToken(builder, OF)
        if (!isDirectParameterStart(builder.tokenType)) return false

        val parameterMarker = enter_section_(builder, level, _NONE_, "<fallback direct parameter>")
        val parameterResult = AppleScriptParser.expression(builder, level + 1)
        exit_section_(builder, level, parameterMarker, DIRECT_PARAMETER_VAL, parameterResult, false, null)
        return parameterResult
    }

    private fun parseCommandParameters(
        builder: PsiBuilder,
        level: Int,
    ) {
        while (isPrepositionParameterStart(builder.tokenType) || builder.tokenType === VAR_IDENTIFIER) {
            val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter>")
            val result =
                parseParameterSelector(builder, level + 1) &&
                    (
                        parseSingleIdentifierValueBeforeBareSelector(builder) ||
                            AppleScriptParser.expression(builder, level + 1)
                    )
            exit_section_(builder, level, marker, COMMAND_PARAMETER, result, false, null)
            if (!result) return
        }
    }

    private fun parseSingleIdentifierValueBeforeBareSelector(builder: PsiBuilder): Boolean {
        if (builder.tokenType !== VAR_IDENTIFIER) return false

        val marker = builder.mark()
        builder.advanceLexer()
        val isBoundedValue =
            builder.tokenType === VAR_IDENTIFIER &&
                AppleScriptParserPhrases.isKnownFallbackBareSelectorStart(builder)
        if (isBoundedValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isBoundedValue
    }

    fun parseParameterSelector(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseParameterSelector")) return false
        val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter selector>")
        val result = parseSelectorTokens(builder)
        exit_section_(builder, level, marker, COMMAND_PARAMETER_SELECTOR, result, false, null)
        return result
    }

    fun parseSelectorTokens(builder: PsiBuilder): Boolean =
        when {
            isPrepositionParameterStart(builder.tokenType) -> {
                val selectorStart = builder.tokenType
                builder.advanceLexer()
                if (selectorStart === WITH && builder.tokenType === VAR_IDENTIFIER) {
                    builder.advanceLexer()
                }
                true
            }
            builder.tokenType === VAR_IDENTIFIER -> {
                parseBareParameterSelector(builder)
                true
            }
            else -> false
        }

    private fun parseBareParameterSelector(builder: PsiBuilder) {
        if (builder.tokenText.equals("default", ignoreCase = true) &&
            (builder.lookAhead(1) === VAR_IDENTIFIER || builder.lookAhead(1) === BUILT_IN_TYPE_S)
        ) {
            builder.advanceLexer()
            builder.advanceLexer()
            return
        }
        if (AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "starting", "at")) return
        if (AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "sound", "name")) return
        if (AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "giving", "up", "after")) return

        builder.advanceLexer()
    }

    private fun isDirectParameterStart(tokenType: IElementType?): Boolean =
        tokenType != null &&
            tokenType !== NLS &&
            tokenType !== COMMENT &&
            !isPrepositionParameterStart(tokenType)

    private fun isPrepositionParameterStart(tokenType: IElementType?): Boolean =
        tokenType === TO ||
            tokenType === INTO ||
            tokenType === FROM ||
            tokenType === ON ||
            tokenType === WITH ||
            tokenType === WITHOUT ||
            tokenType === GIVEN ||
            tokenType === AS ||
            tokenType === USING ||
            tokenType === ABOUT ||
            tokenType === AGAINST ||
            tokenType === BY ||
            tokenType === OVER ||
            tokenType === UNDER
}
