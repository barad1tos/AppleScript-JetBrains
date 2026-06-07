package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CURRENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIGITS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ID
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHERE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHOSE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH

internal object ApplicationObjectReferenceParser {
    fun parse(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseApplicationObjectReference")) return false

        val marker = builder.mark()
        val result =
            if (builder.tokenType === CURRENT) {
                parseCurrentReference(builder)
            } else {
                parseReferenceWithSelector(builder, level + 1)
            }
        if (result) marker.drop() else marker.rollbackTo()
        return result
    }

    private fun parseCurrentReference(builder: PsiBuilder): Boolean {
        builder.advanceLexer()
        return parseTerm(builder) > 0
    }

    private fun parseReferenceWithSelector(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        val termWordCount = parseTerm(builder)
        return termWordCount > 0 &&
            (parseSelector(builder, level + 1) || isBareMultiWordReferenceBoundary(builder, termWordCount))
    }

    private fun parseSelector(
        builder: PsiBuilder,
        level: Int,
    ): Boolean =
        when (builder.tokenType) {
            DIGITS -> AppleScriptParser.integerLiteralExpression(builder, level + 1)
            STRING_LITERAL -> AppleScriptParser.stringLiteralExpression(builder, level + 1)
            ID -> parseIdSelector(builder, level + 1)
            else -> false
        }

    private fun parseIdSelector(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        builder.advanceLexer()
        return AppleScriptParser.expression(builder, level + 1)
    }

    private fun parseTerm(builder: PsiBuilder): Int {
        var termWordCount = 0
        while (FallbackDictionaryAnchorPredicates.isMultiWordNounWord(builder.tokenType)) {
            builder.advanceLexer()
            termWordCount += 1
        }
        return termWordCount
    }

    private fun isBareMultiWordReferenceBoundary(
        builder: PsiBuilder,
        termWordCount: Int,
    ): Boolean =
        termWordCount > 1 &&
            (
                builder.eof() ||
                    builder.tokenType === WITH ||
                    builder.tokenType === OF ||
                    builder.tokenType === IN ||
                    builder.tokenType === ON ||
                    builder.tokenType === WHOSE ||
                    builder.tokenType === WHERE ||
                    builder.tokenType === RPAREN ||
                    builder.tokenType === NLS
            )
}
