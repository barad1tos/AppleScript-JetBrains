package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER

internal object FallbackDictionaryClassParser {
    fun parseClassName(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseFallbackBareIdentifier")) return false
        return FallbackDictionaryTermPredicates.isClassTermToken(builder.tokenType) &&
            parseAnchoredOrClassIdentifier(builder)
    }

    private fun parseAnchoredOrClassIdentifier(builder: PsiBuilder): Boolean =
        if (FallbackDictionaryTermPredicates.isFallbackAnchorForClass(builder.lookAhead(1))) {
            advanceTerm(builder)
        } else {
            parseClassIdentifier(builder)
        }

    private fun parseClassIdentifier(builder: PsiBuilder): Boolean =
        when {
            FallbackDictionaryTermPredicates.isClassDirectSelectorAnchor(builder.lookAhead(1)) -> advanceTerm(builder)
            builder.lookAhead(1) === VAR_IDENTIFIER &&
                FallbackDictionaryTermPredicates.isClassRangeAnchor(builder.lookAhead(2)) -> advanceTerm(builder)
            FallbackDictionaryTermPredicates.isProcessClassDirectReference(builder) -> advanceTerm(builder)
            else -> parseTwoWordIdentifier(builder)
        }

    private fun parseTwoWordIdentifier(builder: PsiBuilder): Boolean {
        val result =
            builder.lookAhead(1) === VAR_IDENTIFIER &&
                FallbackDictionaryTermPredicates.isFallbackAnchorForClass(builder.lookAhead(2))
        if (result) {
            advanceTermPair(builder)
        }
        return result
    }

    private fun advanceTerm(builder: PsiBuilder): Boolean {
        builder.advanceLexer()
        return true
    }

    private fun advanceTermPair(builder: PsiBuilder) {
        builder.advanceLexer()
        builder.advanceLexer()
    }
}
