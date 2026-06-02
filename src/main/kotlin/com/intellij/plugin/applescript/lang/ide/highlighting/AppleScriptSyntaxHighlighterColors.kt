package com.intellij.plugin.applescript.lang.ide.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object AppleScriptSyntaxHighlighterColors {

    const val APPLE_SCRIPT_KEYWORD: String = "APPLE_SCRIPT_KEYWORD"
    const val APPLE_SCRIPT_LOGICAL_OPERATOR: String = "APPLE_SCRIPT_LOGICAL_OPERATOR"
    const val APPLE_SCRIPT_COMPARISON_OPERATOR: String = "APPLE_SCRIPT_COMPARISON_OPERATOR"
    const val APPLE_SCRIPT_LANGUAGE_LITERAL: String = "APPLE_SCRIPT_LANGUAGE_LITERAL"
    const val APPLE_SCRIPT_BUILT_IN_TYPE: String = "APPLE_SCRIPT_BUILT_IN_TYPE"
    private const val APPLE_SCRIPT_NUMBER = "APPLE_SCRIPT_NUMBER"
    private const val APPLE_SCRIPT_STRING = "APPLE_SCRIPT_STRING"
    private const val APPLE_SCRIPT_OPERATION_SIGN = "APPLE_SCRIPT_OPERATION_SIGN"
    private const val APPLE_SCRIPT_COMMENT = "APPLE_SCRIPT_COMMENT"
    private const val DICTIONARY_COMMAND = "DICTIONARY_COMMAND"
    private const val DICTIONARY_COMMAND_SELECTOR = "DICTIONARY_COMMAND_SELECTOR"
    private const val DICTIONARY_CLASS = "DICTIONARY_CLASS"
    private const val DICTIONARY_PROPERTY = "DICTIONARY_PROPERTY"
    private const val DICTIONARY_CONSTANT = "DICTIONARY_CONSTANT"
    private const val UNRESOLVED_ACCESS_ID = "UNRESOLVED_REFERENCE"

    @JvmField
    val KEYWORD: TextAttributesKey =
        createTextAttributesKey(APPLE_SCRIPT_KEYWORD, DefaultLanguageHighlighterColors.KEYWORD)

    @JvmField
    val LOGICAL_OPERATOR: TextAttributesKey =
        createTextAttributesKey(
            APPLE_SCRIPT_LOGICAL_OPERATOR,
            DefaultLanguageHighlighterColors.OPERATION_SIGN,
        )

    @JvmField
    val COMPARISON_OPERATOR: TextAttributesKey =
        createTextAttributesKey(
            APPLE_SCRIPT_COMPARISON_OPERATOR,
            DefaultLanguageHighlighterColors.OPERATION_SIGN,
        )

    @JvmField
    val LANGUAGE_LITERAL: TextAttributesKey =
        createTextAttributesKey(
            APPLE_SCRIPT_LANGUAGE_LITERAL,
            DefaultLanguageHighlighterColors.CONSTANT,
        )

    @JvmField
    val BUILT_IN_TYPE: TextAttributesKey =
        createTextAttributesKey(
            APPLE_SCRIPT_BUILT_IN_TYPE,
            DefaultLanguageHighlighterColors.CLASS_NAME,
        )

    @JvmField
    val STRING: TextAttributesKey =
        createTextAttributesKey(APPLE_SCRIPT_STRING, DefaultLanguageHighlighterColors.STRING)

    @JvmField
    val NUMBER: TextAttributesKey =
        createTextAttributesKey(APPLE_SCRIPT_NUMBER, DefaultLanguageHighlighterColors.NUMBER)

    @JvmField
    val OPERATION_SIGN: TextAttributesKey =
        createTextAttributesKey(
            APPLE_SCRIPT_OPERATION_SIGN,
            DefaultLanguageHighlighterColors.OPERATION_SIGN,
        )

    @JvmField
    val COMMENT: TextAttributesKey =
        createTextAttributesKey(APPLE_SCRIPT_COMMENT, DefaultLanguageHighlighterColors.LINE_COMMENT)

    @JvmField
    val UNRESOLVED_REFERENCE: TextAttributesKey =
        createTextAttributesKey(UNRESOLVED_ACCESS_ID, DefaultLanguageHighlighterColors.CLASS_REFERENCE)

    @JvmField
    val DICTIONARY_COMMAND_ATTR: TextAttributesKey =
        createTextAttributesKey(DICTIONARY_COMMAND, DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE)

    @JvmField
    val DICTIONARY_COMMAND_SELECTOR_ATTR: TextAttributesKey =
        createTextAttributesKey(DICTIONARY_COMMAND_SELECTOR, DefaultLanguageHighlighterColors.PARAMETER)

    @JvmField
    val DICTIONARY_CLASS_ATTR: TextAttributesKey =
        createTextAttributesKey(DICTIONARY_CLASS, DefaultLanguageHighlighterColors.NUMBER)

    @JvmField
    val DICTIONARY_PROPERTY_ATTR: TextAttributesKey =
        createTextAttributesKey(DICTIONARY_PROPERTY, DefaultLanguageHighlighterColors.INSTANCE_FIELD)

    @JvmField
    val DICTIONARY_CONSTANT_ATTR: TextAttributesKey =
        createTextAttributesKey(DICTIONARY_CONSTANT, DefaultLanguageHighlighterColors.CONSTANT)
}
