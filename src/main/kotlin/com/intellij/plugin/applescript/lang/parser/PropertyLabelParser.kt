package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COLON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

internal object PropertyLabelParser {
    fun parse(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parsePropertyLabelIdentifier")) return false

        val marker = builder.mark()
        val firstWordType = builder.tokenType
        val labelWordCount = parseLabelWords(builder)
        // Claim only labels the generated `identifier` rule (`identifier ::= var_identifier`) cannot
        // handle: multi-word labels (`showing output:`, `column count:`) and single keyword-noun
        // labels (`script:`, `count:`). A plain single `name:` is left to `identifier` so its PSI
        // shape is preserved.
        val claimsLabel = labelWordCount > 1 || (labelWordCount == 1 && firstWordType !== VAR_IDENTIFIER)
        val result = claimsLabel && builder.tokenType === COLON
        if (result) marker.drop() else marker.rollbackTo()
        return result
    }

    private fun parseLabelWords(builder: PsiBuilder): Int {
        var labelWordCount = 0
        while (isLabelWord(builder.tokenType)) {
            builder.advanceLexer()
            labelWordCount += 1
        }
        return labelWordCount
    }

    // A record/property label can be a dictionary-style noun and also a keyword noun that is not a
    // plain identifier (`count`, `class`, `id`, ...). The label position before `:` is unambiguous,
    // so the broader contextual-property-term set is safe to accept here.
    private fun isLabelWord(tokenType: IElementType?): Boolean =
        FallbackDictionaryAnchorPredicates.isMultiWordNounWord(tokenType) ||
            FallbackDictionaryTermPredicates.isContextualPropertyTerm(tokenType)
}
