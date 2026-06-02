package com.intellij.plugin.applescript.lang.ide.highlighting

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.plugin.applescript.lang.lexer._AppleScriptLexer
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.BUILT_IN_TYPES
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.COMPARISON_OPERATORS
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.KEYWORDS
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.LANGUAGE_LITERALS
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.LOGICAL_OPERATORS
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.NUMBERS
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.OPERATORS
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.STRINGS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_CLASS_NAME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_COMMAND_NAME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_PROPERTY_NAME
import com.intellij.psi.tree.IElementType

class AppleScriptSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = FlexAdapter(_AppleScriptLexer(null))

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = when {
        LOGICAL_OPERATORS.contains(tokenType) -> pack(AppleScriptSyntaxHighlighterColors.LOGICAL_OPERATOR)
        COMPARISON_OPERATORS.contains(tokenType) -> pack(AppleScriptSyntaxHighlighterColors.COMPARISON_OPERATOR)
        LANGUAGE_LITERALS.contains(tokenType) -> pack(AppleScriptSyntaxHighlighterColors.LANGUAGE_LITERAL)
        BUILT_IN_TYPES.contains(tokenType) -> pack(AppleScriptSyntaxHighlighterColors.BUILT_IN_TYPE)
        else -> pack(ATTRIBUTES[tokenType])
    }

    companion object {
        private val ATTRIBUTES: MutableMap<IElementType, TextAttributesKey> = HashMap()

        init {
            fillMap(ATTRIBUTES, OPERATORS, AppleScriptSyntaxHighlighterColors.OPERATION_SIGN)
            fillMap(ATTRIBUTES, KEYWORDS, AppleScriptSyntaxHighlighterColors.KEYWORD)
            fillMap(
                ATTRIBUTES,
                AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
                DICTIONARY_COMMAND_NAME,
            )
            fillMap(
                ATTRIBUTES,
                AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
                COMMAND_PARAMETER_SELECTOR,
            )
            fillMap(ATTRIBUTES, AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR, DICTIONARY_CLASS_NAME)
            fillMap(ATTRIBUTES, AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR, DICTIONARY_PROPERTY_NAME)
            fillMap(ATTRIBUTES, STRINGS, AppleScriptSyntaxHighlighterColors.STRING)
            fillMap(ATTRIBUTES, NUMBERS, AppleScriptSyntaxHighlighterColors.NUMBER)
            ATTRIBUTES[COMMENT] = AppleScriptSyntaxHighlighterColors.COMMENT
        }
    }
}
