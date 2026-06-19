package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_TYPE_S
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER

internal object AppleScriptParserPhrases {
    fun isKnownFallbackBareSelectorStart(builder: PsiBuilder): Boolean =
        when {
            builder.tokenText.equals("default", ignoreCase = true) ->
                builder.lookAhead(1) === VAR_IDENTIFIER || builder.lookAhead(1) === BUILT_IN_TYPE_S
            hasBareSelectorPhrase(builder, "starting", "at") -> true
            hasBareSelectorPhrase(builder, "sound", "name") -> true
            hasBareSelectorPhrase(builder, "giving", "up", "after") -> true
            hasBareSelectorPhrase(builder, "subtitle") -> true
            else -> false
        }

    fun consumeBareSelectorPhrase(
        builder: PsiBuilder,
        vararg words: String,
    ): Boolean = consumeBareSelectorPhrase(builder, words.asList())

    fun consumeBareSelectorPhrase(
        builder: PsiBuilder,
        words: List<String>,
    ): Boolean {
        if (!hasBareSelectorPhrase(builder, words)) return false
        repeat(words.size) {
            builder.advanceLexer()
        }
        return true
    }

    private fun hasBareSelectorPhrase(
        builder: PsiBuilder,
        vararg words: String,
    ): Boolean = hasBareSelectorPhrase(builder, words.asList())

    private fun hasBareSelectorPhrase(
        builder: PsiBuilder,
        words: List<String>,
    ): Boolean {
        val marker = builder.mark()
        var matches = true
        for (word in words) {
            if (!matchesWord(builder, word)) {
                matches = false
                break
            }
            builder.advanceLexer()
        }
        marker.rollbackTo()
        return matches
    }

    private fun matchesWord(
        builder: PsiBuilder,
        word: String,
    ): Boolean =
        builder.tokenText.equals(word, ignoreCase = true) ||
            builder.tokenType.toString().equals(word, ignoreCase = true)
}
