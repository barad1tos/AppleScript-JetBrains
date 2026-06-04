package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder

internal object FallbackDictionaryTermActions {
    fun advanceTerm(builder: PsiBuilder): Boolean {
        builder.advanceLexer()
        return true
    }

    fun advanceTermPair(builder: PsiBuilder): Boolean {
        builder.advanceLexer()
        builder.advanceLexer()
        return true
    }
}
