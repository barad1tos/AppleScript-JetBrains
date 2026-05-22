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

    override fun isRBraceToken(iterator: HighlighterIterator, fileText: CharSequence, fileType: FileType): Boolean {
        val pair = findPair(false, iterator, fileText, fileType) ?: return false
        if (pair.rightBraceType !== AppleScriptTypes.END) {
            return super.isRBraceToken(iterator, fileText, fileType)
        }

        var result = false
        var count = 0
        while (true) {
            iterator.retreat()
            count++
            if (iterator.atEnd()) break
            val eType: IElementType = iterator.tokenType
            if (eType === AppleScriptTypes.NLS || eType === AppleScriptTypes.BLOCK_BODY) {
                result = true
            } else {
                break
            }
        }
        while (count-- > 0) iterator.advance()
        return result
    }

    override fun isLBraceToken(iterator: HighlighterIterator, fileText: CharSequence, fileType: FileType): Boolean {
        val pair = findPair(true, iterator, fileText, fileType) ?: return false
        if (pair.rightBraceType !== AppleScriptTypes.END) return true

        var result = true
        var count = 0
        while (true) {
            iterator.retreat()
            count++
            if (iterator.atEnd()) break
            val eType: IElementType? = iterator.tokenType
            if (eType === TokenType.WHITE_SPACE) continue
            if (eType === AppleScriptTypes.NLS || eType == null) {
                break
            } else {
                result = false
                break
            }
        }
        while (count-- > 0) iterator.advance()
        if (!result) return false

        result = true
        count = 0
        while (true) {
            iterator.retreat()
            count++
            if (iterator.atEnd()) break
            val eType: IElementType = iterator.tokenType
            if (eType === TokenType.WHITE_SPACE) continue
            if (eType === AppleScriptTypes.END) {
                result = false
            } else {
                break
            }
        }
        while (count-- > 0) iterator.advance()
        if (!result) return false

        // Disambiguate single-line constructs that have no `end`.
        // 1. <tell> with `to` on the same line → single-statement tell, not paired.
        if (pair.leftBraceType === AppleScriptTypes.TELL) {
            count = 0
            result = true
            while (true) {
                iterator.advance()
                count++
                if (iterator.atEnd()) break
                val eType: IElementType = iterator.tokenType
                if (eType === AppleScriptTypes.TO) {
                    result = false
                } else if (eType === AppleScriptTypes.NLS) {
                    break
                }
            }
            while (count-- > 0) iterator.retreat()
            if (!result) return false
        }

        // 2. <if> with `then` followed by a statement on the same line → single-line if.
        if (pair.leftBraceType === AppleScriptTypes.IF) {
            count = 0
            result = true
            var thenKw = false
            while (true) {
                iterator.advance()
                count++
                if (iterator.atEnd()) break
                val eType: IElementType = iterator.tokenType
                if (thenKw) {
                    if (eType === AppleScriptTypes.COMMENT || eType === TokenType.WHITE_SPACE) {
                        continue
                    } else if (eType === AppleScriptTypes.NLS) {
                        break
                    } else {
                        result = false
                        break
                    }
                }
                if (eType === AppleScriptTypes.THEN) {
                    thenKw = true
                }
            }
            while (count-- > 0) iterator.retreat()
            if (!result) return false

            // `else if` is paired as the outer `if`'s brace, not its own.
            count = 0
            result = true
            while (true) {
                iterator.retreat()
                count++
                if (iterator.atEnd()) break
                if (iterator.atEnd()) break
                val eType: IElementType = iterator.tokenType
                if (eType === TokenType.WHITE_SPACE) continue
                if (eType === AppleScriptTypes.ELSE) {
                    result = false
                } else {
                    break
                }
            }
            while (count-- > 0) iterator.advance()
        }
        if (!result) return false

        // `on error` is an inline handler clause, not a block opener.
        if (pair.leftBraceType === AppleScriptTypes.ON) {
            count = 0
            result = true
            while (true) {
                iterator.advance()
                count++
                if (iterator.atEnd()) break
                val eType: IElementType = iterator.tokenType
                if (eType === TokenType.WHITE_SPACE) continue
                if (eType === AppleScriptTypes.ERROR) {
                    result = false
                } else {
                    break
                }
            }
            while (count-- > 0) iterator.retreat()
        }
        return result
    }

    private class MyPairedBraceMatcher : PairedBraceMatcher {
        override fun getPairs(): Array<BracePair> = PAIRS

        override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

        override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
    }

    private companion object {
        private val PAIRS: Array<BracePair> = arrayOf(
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
    }
}
