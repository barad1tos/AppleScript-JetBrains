package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
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
        // Mark the fallback context before the direct parameter so the generic permissive head cannot
        // re-engage on a command parameter value.
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
            commandName.contains(" ") || isPermissiveCommand ->
                FallbackCommandParameterMode.OptionalDirectParameter
            else -> null
        }

    fun isValueLiteralStart(tokenType: IElementType?): Boolean =
        FallbackCommandParameterTokens.isValueLiteralStart(tokenType)

    fun isStructuredDirectParameterStart(tokenType: IElementType?): Boolean =
        FallbackCommandParameterTokens.isStructuredDirectParameterStart(tokenType)

    fun isBuiltInClassDirectParameterStart(builder: PsiBuilder): Boolean =
        FallbackCommandParameterValueBoundaries.hasBuiltInClassValueBeforeBoundary(builder)

    fun isPropertyReferenceDirectParameterStart(builder: PsiBuilder): Boolean =
        FallbackCommandParameterValueBoundaries.hasPropertyReferenceValueBeforeBoundary(builder)

    fun isGrammarValueDirectParameterStart(builder: PsiBuilder): Boolean =
        builder.tokenType !== VAR_IDENTIFIER &&
            FallbackCommandParameterValueBoundaries.hasGrammarValueBeforeBoundary(builder)

    fun isIdentifierPhraseDirectParameterStart(builder: PsiBuilder): Boolean =
        FallbackCommandParameterValueBoundaries.hasIdentifierPhraseBeforePrepositionSelector(builder)

    fun parseSelectorTokens(builder: PsiBuilder): Boolean = FallbackCommandSelectorParser.parseSelectorTokens(builder)
}
