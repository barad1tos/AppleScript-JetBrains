package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER

internal object FallbackDictionaryPropertyPatterns {
    fun isContextualPropertyPairWithAnchor(builder: PsiBuilder): Boolean =
        builder.tokenType === VAR_IDENTIFIER &&
            FallbackDictionaryTermPredicates.isContextualPropertyTerm(builder.lookAhead(1)) &&
            FallbackDictionaryTermPredicates.isFallbackAnchorForProperty(builder.lookAhead(2))

    fun isContextualPropertyPairWithTerminator(builder: PsiBuilder): Boolean =
        FallbackDictionaryTermPredicates.isContextualPropertyTerm(builder.lookAhead(1)) &&
            FallbackDictionaryTermPredicates.isPropertyTerminatorAnchor(builder.lookAhead(2))

    fun isIdentifierPairWithTerminator(builder: PsiBuilder): Boolean =
        builder.lookAhead(1) === VAR_IDENTIFIER &&
            FallbackDictionaryTermPredicates.isPropertyTerminatorAnchor(builder.lookAhead(2))

    fun isTwoWordIdentifierWithAnchor(builder: PsiBuilder): Boolean =
        builder.lookAhead(1) === VAR_IDENTIFIER &&
            FallbackDictionaryTermPredicates.isFallbackAnchorForProperty(builder.lookAhead(2))
}
