package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_CONSTANT_LITERAL_EXPRESSION

internal object PossessivePronounParser {
    fun isPossessivePronoun(builder: PsiBuilder): Boolean {
        val previousNode: LighterASTNode? = builder.latestDoneMarker
        return previousNode != null &&
            previousNode.tokenType === BUILT_IN_CONSTANT_LITERAL_EXPRESSION &&
            previousNode.toString().equals("its", ignoreCase = true)
    }
}
