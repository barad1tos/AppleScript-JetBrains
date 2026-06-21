package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AFTER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TEXT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

internal object PermissiveCommandTailParser {
    // Consume residual unknown-command tail tokens until the statement boundary, wrapping each
    // chunk in COMMAND_PARAMETER. Object-reference continuations stay available to object refs.
    fun drainTail(
        builder: PsiBuilder,
        level: Int,
    ) {
        while (!builder.eof() && isPermissiveTailStart(builder)) {
            val marker = enter_section_(builder, level, _NONE_, "<permissive command parameter>")
            val consumed = consumeParameterChunk(builder, level + 1)
            exit_section_(builder, level, marker, COMMAND_PARAMETER, consumed, false, null)
            if (!consumed) break
        }
    }

    fun consumeBracketedValue(builder: PsiBuilder): Boolean =
        AppleScriptBracketedValueParser.consume(builder, allowNewlines = false)

    private fun consumeParameterChunk(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!isObjectReferenceValueStart(builder) &&
            FallbackCommandSelectorParser.parseParameterContent(builder, level + 1)
        ) {
            return true
        }

        var advanced = false
        while (isPermissiveSelectorWord(builder.tokenType) && !isObjectReferenceValueStart(builder)) {
            builder.advanceLexer()
            advanced = true
        }

        when {
            AppleScriptBracketedValueParser.isBracketStart(builder.tokenType) -> {
                advanced = consumeBracketedValue(builder)
            }
            FallbackCommandParameterTokens.isValueLiteralStart(builder.tokenType) ||
                FallbackCommandParameterParser.isGrammarValueDirectParameterStart(builder) ||
                builder.tokenType === VAR_IDENTIFIER -> {
                val before = builder.currentOffset
                val parsed = AppleScriptParser.expression(builder, level + 1)
                if (!parsed || builder.currentOffset == before) {
                    builder.advanceLexer()
                }
                advanced = true
            }
            !advanced && isPermissiveTailStart(builder) -> {
                builder.advanceLexer()
                advanced = true
            }
        }
        return advanced
    }

    private fun isPermissiveSelectorWord(tokenType: IElementType?): Boolean =
        FallbackCommandParameterTokens.isCommandSelectorStart(tokenType) ||
            isPermissiveKeywordSelectorWord(tokenType) ||
            tokenType === VAR_IDENTIFIER

    private fun isPermissiveKeywordSelectorWord(tokenType: IElementType?): Boolean =
        tokenType === AFTER || tokenType === TEXT

    private fun isObjectReferenceValueStart(builder: PsiBuilder): Boolean =
        builder.tokenType === VAR_IDENTIFIER && builder.lookAhead(1) === OF

    private fun isPermissiveTailStart(builder: PsiBuilder): Boolean {
        val tokenType = builder.tokenType
        return tokenType != null &&
            tokenType !== NLS &&
            tokenType !== COMMENT &&
            tokenType !== OF &&
            (
                isPermissiveSelectorWord(tokenType) ||
                    FallbackCommandParameterTokens.isValueLiteralStart(tokenType) ||
                    FallbackCommandParameterParser.isGrammarValueDirectParameterStart(builder)
            )
    }
}
