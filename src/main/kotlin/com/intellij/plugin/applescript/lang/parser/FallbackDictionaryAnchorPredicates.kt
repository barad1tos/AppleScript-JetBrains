package com.intellij.plugin.applescript.lang.parser

import com.intellij.plugin.applescript.psi.AppleScriptTypes.AS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DATE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DOES_NOT_CONTAIN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ENDS_WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EQ
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EVENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FILE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_CONTAIN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_NOT_IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LIST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SCRIPT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STARTS_BEGINS_WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TAB
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

internal object FallbackDictionaryAnchorPredicates {
    fun isClassDirectReferenceAnchor(tokenType: IElementType?): Boolean =
        tokenType === AS ||
            tokenType === OF ||
            tokenType === IN ||
            tokenType === RPAREN ||
            tokenType === NLS ||
            tokenType === COMMENT

    fun isPropertyComparisonAnchor(tokenType: IElementType?): Boolean =
        tokenType === EQ ||
            tokenType === NE ||
            tokenType === LT ||
            tokenType === GT ||
            tokenType === LE ||
            tokenType === GE ||
            tokenType === STARTS_BEGINS_WITH ||
            tokenType === ENDS_WITH ||
            tokenType === DOES_NOT_CONTAIN ||
            tokenType === IS_IN ||
            tokenType === IS_NOT_IN ||
            tokenType === IS_CONTAIN

    fun isSpecifierTerm(tokenType: IElementType?): Boolean =
        tokenType === EVENT || tokenType === TAB || tokenType === FILE || tokenType === DATE

    // Words that can compose a multi-word, dictionary-style noun phrase: generic
    // application-object terms (`computer list`, `lock screen task`) and multi-word record
    // labels (`showing output`). LIST/SCRIPT are keywords that double as plain nouns here.
    fun isMultiWordNounWord(tokenType: IElementType?): Boolean =
        isSpecifierTerm(tokenType) ||
            tokenType === VAR_IDENTIFIER ||
            tokenType === LIST ||
            tokenType === SCRIPT
}
