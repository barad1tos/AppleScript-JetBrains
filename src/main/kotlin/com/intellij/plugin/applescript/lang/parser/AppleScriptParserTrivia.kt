package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

internal object AppleScriptParserTrivia {
    fun previousNonSpaceToken(builder: PsiBuilder): IElementType? {
        var index = -1
        var tokenType = builder.rawLookup(index)
        while (tokenType === TokenType.WHITE_SPACE) {
            tokenType = builder.rawLookup(--index)
        }
        return tokenType
    }
}
