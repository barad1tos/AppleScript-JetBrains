package com.intellij.plugin.applescript.psi

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.psi.impl.AppleScriptExpressionImpl
import com.intellij.psi.ElementManipulators
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost

abstract class AppleScriptStringLiteralExpressionBase(node: ASTNode) :
    AppleScriptExpressionImpl(node),
    PsiLanguageInjectionHost,
    AppleScriptStringLiteralExpression {

    abstract override fun getStringLiteral(): com.intellij.psi.PsiElement

    override fun isValidHost(): Boolean = true

    override fun updateText(text: String): PsiLanguageInjectionHost =
        ElementManipulators.handleContentChange(this, text)

    override fun createLiteralTextEscaper(): LiteralTextEscaper<AppleScriptStringLiteralExpressionBase> =
        StringLiteralEscaper(this)

    class StringLiteralEscaper(host: AppleScriptStringLiteralExpressionBase) :
        LiteralTextEscaper<AppleScriptStringLiteralExpressionBase>(host) {

        private lateinit var sourceOffsets: IntArray

        override fun getRelevantTextRange(): TextRange {
            val length = myHost.textLength
            return if (length > 1) TextRange.from(1, length - 2) else super.getRelevantTextRange()
        }

        override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
            val hostText = rangeInsideHost.substring(myHost.text)
            sourceOffsets = IntArray(hostText.length + 1)
            return CodeInsightUtilCore.parseStringCharacters(hostText, outChars, sourceOffsets)
        }

        override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
            val result = if (offsetInDecoded < sourceOffsets.size) sourceOffsets[offsetInDecoded] else -1
            if (result == -1) return -1
            val clamped = if (result <= rangeInsideHost.length) result else rangeInsideHost.length
            return clamped + rangeInsideHost.startOffset
        }

        override fun isOneLine(): Boolean = false
    }
}
