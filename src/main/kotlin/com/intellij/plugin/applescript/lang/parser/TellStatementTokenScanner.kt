package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.APPLICATION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ID
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.psi.TokenType

internal object TellStatementTokenScanner {
    fun previousRelevantToken(builder: PsiBuilder): Any? {
        var index = -1
        var previousElement = builder.rawLookup(index)
        while (isIgnorableTellStartToken(previousElement)) {
            previousElement = builder.rawLookup(--index)
        }
        return previousElement
    }

    private fun isIgnorableTellStartToken(token: Any?): Boolean =
        token === TokenType.WHITE_SPACE ||
            token === MY ||
            token === APPLICATION ||
            token === STRING_LITERAL ||
            token == null ||
            token === ID
}
