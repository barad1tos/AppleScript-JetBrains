package com.intellij.plugin.applescript.lang.ide.highlighting

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.fileTypes.FileType
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Brace matching for AppleScript's `on`/`tell`/`using`/`try`/`if`/`repeat`/`script`/`considering`
 * blocks (closed by `end`) plus the punctuation pairs `{ }` and `( )`. Because `end` is shared
 * across multiple block kinds, the LBrace/RBrace decisions look at neighbouring tokens to
 * disambiguate constructs that have no `end` (single-line `tell ... to ...`, `if ... then ...`,
 * `on error ...`).
 */
class AppleScriptPairedBraceMatcher : PairedBraceMatcherAdapter(MyPairedBraceMatcher(), AppleScriptLanguage) {
    override fun isRBraceToken(
        iterator: HighlighterIterator,
        fileText: CharSequence,
        fileType: FileType,
    ): Boolean {
        val pair = findPair(false, iterator, fileText, fileType) ?: return false
        return if (pair.rightBraceType === AppleScriptTypes.END) {
            iterator.hasEndBlockPrefix()
        } else {
            super.isRBraceToken(iterator, fileText, fileType)
        }
    }

    override fun isLBraceToken(
        iterator: HighlighterIterator,
        fileText: CharSequence,
        fileType: FileType,
    ): Boolean {
        val pair = findPair(true, iterator, fileText, fileType) ?: return false
        return pair.rightBraceType !== AppleScriptTypes.END ||
            iterator.isBlockOpeningToken(pair.leftBraceType)
    }

    private class MyPairedBraceMatcher : PairedBraceMatcher {
        override fun getPairs(): Array<BracePair> = APPLESCRIPT_BRACE_PAIRS

        override fun isPairedBracesAllowedBeforeType(
            lbraceType: IElementType,
            contextType: IElementType?,
        ): Boolean = true

        override fun getCodeConstructStart(
            file: PsiFile?,
            openingBraceOffset: Int,
        ): Int = openingBraceOffset
    }
}

private val APPLESCRIPT_BRACE_PAIRS: Array<BracePair> =
    arrayOf(
        BracePair(AppleScriptTypes.ON, AppleScriptTypes.END, true),
        BracePair(AppleScriptTypes.TELL, AppleScriptTypes.END, true),
        BracePair(AppleScriptTypes.USING, AppleScriptTypes.END, true),
        BracePair(AppleScriptTypes.TRY, AppleScriptTypes.END, true),
        BracePair(AppleScriptTypes.IF, AppleScriptTypes.END, true),
        BracePair(AppleScriptTypes.REPEAT, AppleScriptTypes.END, true),
        BracePair(AppleScriptTypes.LCURLY, AppleScriptTypes.RCURLY, true),
        BracePair(AppleScriptTypes.LPAREN, AppleScriptTypes.RPAREN, true),
        BracePair(AppleScriptTypes.SCRIPT, AppleScriptTypes.END, true),
        BracePair(AppleScriptTypes.CONSIDERING, AppleScriptTypes.END, true),
    )

private fun HighlighterIterator.isBlockOpeningToken(leftBraceType: IElementType): Boolean =
    isAtBlockOpeningPosition() &&
        !hasAdjacentTokenIgnoringWhitespace(ScanDirection.BACKWARD, AppleScriptTypes.END) &&
        when (leftBraceType) {
            AppleScriptTypes.TELL -> !hasTokenBeforeLineEnd(AppleScriptTypes.TO)
            AppleScriptTypes.IF ->
                !hasInlineStatementAfterThen() &&
                    !hasAdjacentTokenIgnoringWhitespace(ScanDirection.BACKWARD, AppleScriptTypes.ELSE)
            AppleScriptTypes.ON -> !hasAdjacentTokenIgnoringWhitespace(ScanDirection.FORWARD, AppleScriptTypes.ERROR)
            else -> true
        }

private fun HighlighterIterator.hasEndBlockPrefix(): Boolean {
    var hasPrefix = false
    scanRestoring(ScanDirection.BACKWARD) { tokenType ->
        val isBlockBoundary =
            tokenType === AppleScriptTypes.NLS ||
                tokenType === AppleScriptTypes.BLOCK_BODY
        if (isBlockBoundary) {
            hasPrefix = true
        }
        isBlockBoundary
    }
    return hasPrefix
}

private fun HighlighterIterator.isAtBlockOpeningPosition(): Boolean {
    val previousToken =
        firstToken(
            direction = ScanDirection.BACKWARD,
            skipToken = { it === TokenType.WHITE_SPACE },
        )
    return previousToken == null || previousToken === AppleScriptTypes.NLS
}

private fun HighlighterIterator.hasAdjacentTokenIgnoringWhitespace(
    direction: ScanDirection,
    expectedToken: IElementType,
): Boolean =
    firstToken(
        direction = direction,
        skipToken = { it === TokenType.WHITE_SPACE },
    ) === expectedToken

private fun HighlighterIterator.hasTokenBeforeLineEnd(tokenToFind: IElementType): Boolean {
    var hasToken = false
    scanRestoring(ScanDirection.FORWARD) { tokenType ->
        val isTargetToken = tokenType === tokenToFind
        if (isTargetToken) {
            hasToken = true
        }
        tokenType !== AppleScriptTypes.NLS && !isTargetToken
    }
    return hasToken
}

private fun HighlighterIterator.hasInlineStatementAfterThen(): Boolean {
    var isAfterThen = false
    var hasInlineStatement = false
    scanRestoring(ScanDirection.FORWARD) { tokenType ->
        val shouldContinue =
            when {
                !isAfterThen -> {
                    if (tokenType === AppleScriptTypes.THEN) {
                        isAfterThen = true
                    }
                    true
                }
                tokenType === AppleScriptTypes.COMMENT ||
                    tokenType === TokenType.WHITE_SPACE ->
                    true
                tokenType === AppleScriptTypes.NLS -> false
                else -> {
                    hasInlineStatement = true
                    false
                }
            }
        shouldContinue
    }
    return hasInlineStatement
}

private fun HighlighterIterator.firstToken(
    direction: ScanDirection,
    skipToken: (IElementType) -> Boolean,
): IElementType? {
    var foundToken: IElementType? = null
    scanRestoring(direction) { tokenType ->
        val shouldSkip = skipToken(tokenType)
        if (!shouldSkip) {
            foundToken = tokenType
        }
        shouldSkip
    }
    return foundToken
}

private fun HighlighterIterator.scanRestoring(
    direction: ScanDirection,
    visitToken: (IElementType) -> Boolean,
) {
    var steps = 0
    var shouldContinue = true
    while (shouldContinue) {
        move(direction)
        steps++
        shouldContinue = !atEnd() && visitToken(tokenType)
    }
    repeat(steps) {
        move(direction.opposite())
    }
}

private fun HighlighterIterator.move(direction: ScanDirection) {
    when (direction) {
        ScanDirection.FORWARD -> advance()
        ScanDirection.BACKWARD -> retreat()
    }
}

private enum class ScanDirection {
    FORWARD,
    BACKWARD,
}

private fun ScanDirection.opposite(): ScanDirection =
    when (this) {
        ScanDirection.FORWARD -> ScanDirection.BACKWARD
        ScanDirection.BACKWARD -> ScanDirection.FORWARD
    }
