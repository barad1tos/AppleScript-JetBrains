package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CLASS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMA
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COUNT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIGITS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.END
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HOURS_CONSTANT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ID
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MINUTES_CONSTANT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NAMED
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SECONDS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SET
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THROUGH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THRU
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHERE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHOSE
import com.intellij.psi.tree.IElementType

internal object FallbackDictionaryTermPredicates {
    fun isFallbackAnchorForProperty(tokenType: IElementType?): Boolean =
        tokenType === OF || tokenType === IN || FallbackDictionaryAnchorPredicates.isPropertyComparisonAnchor(tokenType)

    fun isFallbackAnchorForClass(tokenType: IElementType?): Boolean =
        tokenType === OF ||
            tokenType === IN ||
            tokenType === DIGITS ||
            tokenType === WHOSE ||
            tokenType === WHERE

    // A keyword that may legitimately appear as the SECOND word of a dictionary class noun phrase
    // (`rule set`, `containing set`). `set` lexes as the SET keyword, not VAR_IDENTIFIER, so the
    // generic two-word path rejects it. Kept narrow (SET only) and consumed only in operand position
    // — never widen `isFallbackAnchorForClass` with SET, which would break the `set` assignment statement.
    fun isClassContinuationKeyword(tokenType: IElementType?): Boolean = tokenType === SET

    fun isClassTermToken(type: IElementType?): Boolean =
        isVariableIdentifier(type) || FallbackDictionaryAnchorPredicates.isSpecifierTerm(type)

    fun isClassDirectSelectorAnchor(type: IElementType?) = type === STRING_LITERAL || type === NAMED

    fun isProcessClassDirectReference(builder: PsiBuilder): Boolean =
        builder.tokenText.equals("process", ignoreCase = true) &&
            builder.lookAhead(1) === VAR_IDENTIFIER &&
            isClassDirectReferenceAnchor(builder.lookAhead(2))

    fun isPropertyTerminatorAnchor(type: IElementType?) =
        type === NLS || type === RPAREN || type === COMMA || type === RCURLY

    fun isClassRangeAnchor(tokenType: IElementType?): Boolean =
        tokenType === THRU || tokenType === THROUGH || tokenType === FROM || tokenType === TO

    fun isContextualPropertyTerm(tokenType: IElementType?): Boolean =
        tokenType === CLASS ||
            tokenType === COUNT ||
            tokenType === HOURS_CONSTANT ||
            tokenType === ID ||
            tokenType === MINUTES_CONSTANT ||
            tokenType === SECONDS ||
            tokenType === END ||
            FallbackDictionaryAnchorPredicates.isSpecifierTerm(tokenType)

    private fun isClassDirectReferenceAnchor(tokenType: IElementType?): Boolean =
        FallbackDictionaryAnchorPredicates.isClassDirectReferenceAnchor(tokenType)

    private fun isVariableIdentifier(tokenType: IElementType?): Boolean = tokenType === VAR_IDENTIFIER
}
