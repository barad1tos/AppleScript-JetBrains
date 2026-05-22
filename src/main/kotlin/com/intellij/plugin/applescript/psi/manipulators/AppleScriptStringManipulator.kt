package com.intellij.plugin.applescript.psi.manipulators

import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.plugin.applescript.psi.AppleScriptStringLiteralExpression
import com.intellij.psi.AbstractElementManipulator
import com.intellij.util.IncorrectOperationException

class AppleScriptStringManipulator : AbstractElementManipulator<AppleScriptStringLiteralExpression>() {

    override fun getRangeInElement(element: AppleScriptStringLiteralExpression): TextRange {
        val length = element.textLength
        return if (length > 1) TextRange.from(1, length - 2) else super.getRangeInElement(element)
    }

    @Throws(IncorrectOperationException::class)
    override fun handleContentChange(
        element: AppleScriptStringLiteralExpression,
        range: TextRange,
        newContent: String,
    ): AppleScriptStringLiteralExpression {
        val escaped = newContent.replace("\"", "\\\"")
        val stringLiteral = AppleScriptPsiElementFactory.createStringLiteral(element.project, escaped)
        return element.replace(stringLiteral) as AppleScriptStringLiteralExpression
    }
}
