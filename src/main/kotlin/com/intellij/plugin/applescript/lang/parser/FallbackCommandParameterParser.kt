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

internal enum class FallbackCommandParameterMode {
    OptionalDirectParameter,
    ParametersOnly,
}

private data class FallbackCommandParameterParseState(
    val mode: FallbackCommandParameterMode,
    val isPermissiveHead: Boolean,
)

internal object FallbackCommandParameterParser {
    fun parseParameters(
        builder: PsiBuilder,
        level: Int,
        commandName: String?,
    ): Boolean {
        var result = false
        if (commandName != null) {
            val isPermissiveCommand =
                builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED) == true
            val parserProvidedMode =
                builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE)
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED, null)
            builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_MODE, null)

            parameterParseState(commandName, isPermissiveCommand, parserProvidedMode)?.let { state ->
                result = parseParametersWithContext(builder, level, state)
            }
        }
        return result
    }

    private fun parameterParseState(
        commandName: String,
        isPermissiveCommand: Boolean,
        parserProvidedMode: FallbackCommandParameterMode?,
    ): FallbackCommandParameterParseState? =
        parameterMode(commandName, isPermissiveCommand, parserProvidedMode)?.let { mode ->
            // The generic-head coupling flag is single-use: capture then consume it here so it never leaks
            // into the direct-parameter expression parse or a sibling command on the same line.
            FallbackCommandParameterParseState(
                mode = mode,
                isPermissiveHead = isPermissiveCommand && parserProvidedMode == null,
            )
        }

    private fun parseParametersWithContext(
        builder: PsiBuilder,
        level: Int,
        state: FallbackCommandParameterParseState,
    ): Boolean {
        val previousFallbackContext =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS)
        // Mark the fallback context BEFORE the direct parameter so the generic permissive head cannot
        // re-engage on a parameter VALUE (`display dialog "Hello" default answer ...` - the nested
        // `default answer` must NOT be hijacked as a command head inside the direct-parameter expression).
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS, true)
        try {
            parseParameterBody(builder, level, state)
        } finally {
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETERS,
                previousFallbackContext,
            )
        }
        return true
    }

    private fun parseParameterBody(
        builder: PsiBuilder,
        level: Int,
        state: FallbackCommandParameterParseState,
    ) {
        if (state.isPermissiveHead) {
            // Unknown osax/application commands can carry heterogeneous tails that the strict
            // selector loop does not model: keyword-noun labels (`width for labels`,
            // `placeholder text`), the `return` constant, and bracketed values. Drain the
            // whole tail to the statement boundary so the unknown command does not leave a
            // dangling PsiErrorElement. Recognized fallback commands keep precise selector PSI.
            PermissiveCommandTailParser.drainTail(builder, level + 1)
        } else {
            if (state.mode == FallbackCommandParameterMode.OptionalDirectParameter) {
                FallbackDirectParameterParser.parseOptionalDirectParameter(builder, level + 1)
            }
            FallbackCommandSelectorParser.parseCommandParameters(builder, level + 1)
        }
    }

    private fun parameterMode(
        commandName: String,
        isPermissiveCommand: Boolean,
        parserProvidedMode: FallbackCommandParameterMode?,
    ): FallbackCommandParameterMode? =
        parserProvidedMode ?: when {
            // Multi-word fallback heads carry a direct object naturally (`display dialog "x"`).
            // A single-word unknown command gets one only when the generic command-name acceptor
            // has already proved a command-shaped tail, so plain variables remain untouched.
            commandName.contains(" ") || isPermissiveCommand ->
                FallbackCommandParameterMode.OptionalDirectParameter
            else -> null
        }

    // Literal/value starts accepted directly after an unknown command head, before any selector word.
    fun isValueLiteralStart(tokenType: IElementType?): Boolean =
        FallbackCommandParameterTokens.isValueLiteralStart(tokenType)

    fun isStructuredDirectParameterStart(tokenType: IElementType?): Boolean =
        FallbackCommandParameterTokens.isStructuredDirectParameterStart(tokenType)

    fun isIdentifierPhraseDirectParameterStart(builder: PsiBuilder): Boolean =
        FallbackCommandParameterValueBoundaries.hasIdentifierPhraseBeforePrepositionSelector(builder)

    fun parseSelectorTokens(builder: PsiBuilder): Boolean = FallbackCommandSelectorParser.parseSelectorTokens(builder)
}

private object FallbackDirectParameterParser {
    fun parseOptionalDirectParameter(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        consumeToken(builder, OF)
        if (!FallbackCommandParameterTokens.isDirectParameterStart(builder.tokenType)) return false

        val parameterMarker = enter_section_(builder, level, _NONE_, "<fallback direct parameter>")
        val parameterResult = parseValue(builder, level + 1)
        exit_section_(builder, level, parameterMarker, DIRECT_PARAMETER_VAL, parameterResult, false, null)
        return parameterResult
    }

    private fun parseValue(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        parseCompleteNumberLiteral(builder, level + 1) ||
            FallbackCommandParameterValueBoundaries.consumeIdentifierPhraseBeforePrepositionSelector(builder) ||
            FallbackCommandParameterValueBoundaries.parseExpressionAtValueBoundary(builder, level + 1) ||
            parseStructuredBracketFallback(builder)

    private fun parseStructuredBracketFallback(builder: PsiBuilder): Boolean =
        FallbackCommandParameterTokens.isStructuredBracketStart(builder.tokenType) &&
            PermissiveCommandTailParser.consumeBracketedValue(builder)

    private fun parseCompleteNumberLiteral(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        val marker = builder.mark()
        val parsed = AppleScriptParser.numberLiteralExpression(builder, level + 1)
        val isCompleteValue = parsed && isDirectParameterBoundary(builder.tokenType)
        if (isCompleteValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isCompleteValue
    }

    private fun isDirectParameterBoundary(tokenType: IElementType?): Boolean =
        tokenType == null ||
            tokenType === NLS ||
            tokenType === COMMENT ||
            FallbackCommandParameterTokens.isParameterSelectorStart(tokenType)
}

private object PermissiveCommandTailParser {
    // Consume residual unknown-command tail tokens until the statement boundary, wrapping each
    // chunk in COMMAND_PARAMETER. Every iteration advances at least one token, so it terminates.
    // Object-reference continuations (`of`, `whose`, `thru`) are not command-tail starts and remain
    // available to the surrounding object-reference grammar.
    fun drainTail(
        builder: PsiBuilder,
        level: Int,
    ) {
        while (!builder.eof() && isPermissiveTailStart(builder.tokenType)) {
            val marker = enter_section_(builder, level, _NONE_, "<permissive command parameter>")
            val consumed = consumeParameterChunk(builder, level + 1)
            exit_section_(builder, level, marker, COMMAND_PARAMETER, consumed, false, null)
            if (!consumed) break
        }
    }

    private fun consumeParameterChunk(
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
                advanced = consumeBracketedValue(builder)
            }
            FallbackCommandParameterTokens.isValueLiteralStart(builder.tokenType) ||
                builder.tokenType === VAR_IDENTIFIER -> {
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
    fun consumeBracketedValue(builder: PsiBuilder): Boolean {
        val expectedClosers = mutableListOf<IElementType>()
        var consumedBalancedValue = false
        var shouldContinue = true
        while (!builder.eof() && shouldContinue) {
            val isValidToken =
                when (val token = builder.tokenType) {
                    LCURLY -> {
                        expectedClosers += RCURLY
                        true
                    }
                    LPAREN -> {
                        expectedClosers += RPAREN
                        true
                    }
                    RCURLY, RPAREN -> {
                        expectedClosers.isNotEmpty() &&
                            expectedClosers.removeAt(expectedClosers.lastIndex) === token
                    }
                    NLS -> expectedClosers.isEmpty()
                    else -> true
                }
            if (isValidToken) {
                builder.advanceLexer()
                consumedBalancedValue = expectedClosers.isEmpty()
            }
            shouldContinue = isValidToken && !consumedBalancedValue
        }
        return consumedBalancedValue
    }

    private fun isPermissiveSelectorWord(tokenType: IElementType?): Boolean =
        FallbackCommandParameterTokens.isPrepositionParameterStart(tokenType) ||
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
                    FallbackCommandParameterTokens.isValueLiteralStart(tokenType)
            )
}

private object FallbackCommandParameterValueBoundaries {
    fun parseExpressionAtValueBoundary(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        val marker = builder.mark()
        val parsed = AppleScriptParser.expression(builder, level + 1)
        val isCompleteValue = parsed && isValueBoundary(builder.tokenType)
        if (isCompleteValue) {
            marker.drop()
        } else {
            marker.rollbackTo()
        }
        return isCompleteValue
    }

    fun hasIdentifierPhraseBeforePrepositionSelector(builder: PsiBuilder): Boolean =
        identifierRunLength(builder).let { identifiers ->
            identifiers > 0 &&
                FallbackCommandParameterTokens.isPrepositionParameterStart(builder.lookAhead(identifiers))
        }

    fun consumeIdentifierPhraseBeforePrepositionSelector(builder: PsiBuilder): Boolean {
        val identifierCount = identifierRunLength(builder)
        val shouldConsume =
            identifierCount > 0 &&
                FallbackCommandParameterTokens.isPrepositionParameterStart(builder.lookAhead(identifierCount))
        if (shouldConsume) {
            repeat(identifierCount) {
                builder.advanceLexer()
            }
        }
        return shouldConsume
    }

    fun consumeIdentifierPhraseBeforeStructuredBareSelector(builder: PsiBuilder): Boolean {
        val identifierCount = identifierRunLength(builder)
        val shouldConsume =
            identifierCount >= 2 &&
                FallbackCommandParameterTokens.isStructuredBracketStart(builder.lookAhead(identifierCount))
        if (shouldConsume) {
            repeat(identifierCount - 1) {
                builder.advanceLexer()
            }
        }
        return shouldConsume
    }

    private fun identifierRunLength(builder: PsiBuilder): Int {
        var offset = 0
        while (builder.lookAhead(offset) === VAR_IDENTIFIER) {
            offset += 1
        }
        return offset
    }

    private fun isValueBoundary(tokenType: IElementType?): Boolean =
        tokenType == null ||
            tokenType === NLS ||
            tokenType === COMMENT ||
            FallbackCommandParameterTokens.isParameterSelectorStart(tokenType)
}

private object FallbackCommandSelectorParser {
    fun parseCommandParameters(
        builder: PsiBuilder,
        level: Int,
    ) {
        var shouldContinue = true
        while (shouldContinue &&
            (
                FallbackCommandParameterTokens.isPrepositionParameterStart(builder.tokenType) ||
                    builder.tokenType === VAR_IDENTIFIER
            )
        ) {
            val marker = enter_section_(builder, level, _NONE_, "<fallback command parameter>")
            val result =
                parseParameterSelector(builder, level + 1) &&
                    parseParameterValue(builder, level + 1)
            exit_section_(builder, level, marker, COMMAND_PARAMETER, result, false, null)
            shouldContinue = result
        }
    }

    private fun parseParameterValue(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        parseSimpleLiteralValueBeforeSelector(builder) ||
            FallbackCommandParameterValueBoundaries.consumeIdentifierPhraseBeforeStructuredBareSelector(builder) ||
            parseSingleIdentifierValueBeforeBareSelector(builder) ||
            FallbackCommandParameterValueBoundaries.parseExpressionAtValueBoundary(builder, level + 1) ||
            parseStructuredBracketFallback(builder)

    private fun parseStructuredBracketFallback(builder: PsiBuilder): Boolean =
        FallbackCommandParameterTokens.isStructuredBracketStart(builder.tokenType) &&
            PermissiveCommandTailParser.consumeBracketedValue(builder)

    private fun parseSimpleLiteralValueBeforeSelector(builder: PsiBuilder): Boolean {
        val isBoundedLiteral =
            builder.tokenType === STRING_LITERAL &&
                FallbackCommandParameterTokens.isParameterSelectorStart(builder.lookAhead(1))
        if (isBoundedLiteral) {
            builder.advanceLexer()
        }
        return isBoundedLiteral
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
            parseDictionarySelectorTokens(builder) -> true
            FallbackCommandParameterTokens.isPrepositionParameterStart(builder.tokenType) -> {
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

    private fun parseDictionarySelectorTokens(builder: PsiBuilder): Boolean {
        val parameterNames =
            builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_FALLBACK_COMMAND_PARAMETER_NAMES).orEmpty()
        return parameterNames
            .asSequence()
            .map { parameterName -> parameterName.split(' ') }
            .sortedByDescending { words -> words.size }
            .any { words ->
                AppleScriptParserPhrases.consumeBareSelectorPhrase(
                    builder,
                    words,
                )
            }
    }

    private fun parseBareParameterSelector(builder: PsiBuilder) {
        when {
            builder.tokenText.equals("default", ignoreCase = true) &&
                (builder.lookAhead(1) === VAR_IDENTIFIER || builder.lookAhead(1) === BUILT_IN_TYPE_S) -> {
                builder.advanceLexer()
                builder.advanceLexer()
            }
            AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "starting", "at") -> Unit
            AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "sound", "name") -> Unit
            AppleScriptParserPhrases.consumeBareSelectorPhrase(builder, "giving", "up", "after") -> Unit
            else -> builder.advanceLexer()
        }
    }
}

private object FallbackCommandParameterTokens {
    fun isValueLiteralStart(tokenType: IElementType?): Boolean =
        isStructuredDirectParameterStart(tokenType) ||
            tokenType === RETURN ||
            tokenType === TRUE ||
            tokenType === FALSE ||
            tokenType === MISSING_VALUE ||
            tokenType === CURRENT

    fun isStructuredDirectParameterStart(tokenType: IElementType?): Boolean =
        tokenType === STRING_LITERAL ||
            tokenType === DIGITS ||
            tokenType === DOT ||
            isStructuredBracketStart(tokenType)

    fun isStructuredBracketStart(tokenType: IElementType?): Boolean =
        tokenType === LCURLY ||
            tokenType === LPAREN

    fun isDirectParameterStart(tokenType: IElementType?): Boolean =
        tokenType != null &&
            tokenType !== NLS &&
            tokenType !== COMMENT &&
            !isPrepositionParameterStart(tokenType)

    fun isPrepositionParameterStart(tokenType: IElementType?): Boolean =
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

    fun isParameterSelectorStart(tokenType: IElementType?): Boolean =
        isPrepositionParameterStart(tokenType) || tokenType === VAR_IDENTIFIER
}
