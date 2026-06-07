package com.intellij.plugin.applescript.lang.formatter

import com.intellij.formatting.Block
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.psi.impl.isWhiteSpaceOrNls
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.psi.formatter.FormattingDocumentModelImpl
import com.intellij.psi.formatter.PsiBasedFormattingModel
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType

class AppleScriptFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val element = formattingContext.psiElement
        val settings = formattingContext.codeStyleSettings
        val containingFile =
            checkNotNull(element.containingFile.viewProvider.getPsi(AppleScriptLanguage)) {
                element.containingFile
            }
        val astNode = checkNotNull(containingFile.node)
        val rootBlock = AppleScriptBlock(astNode, null, null, settings)
        return AppleScriptFormattingModel(
            containingFile,
            rootBlock,
            FormattingDocumentModelImpl.createOn(containingFile),
        )
    }

    override fun getRangeAffectingIndent(
        file: PsiFile?,
        offset: Int,
        elementAtOffset: ASTNode?,
    ): TextRange? = null

    private class AppleScriptFormattingModel(
        file: PsiFile,
        rootBlock: Block,
        documentModel: FormattingDocumentModelImpl,
    ) : PsiBasedFormattingModel(file, rootBlock, documentModel) {
        override fun replaceWithPsiInLeaf(
            textRange: TextRange,
            whiteSpace: String,
            leafElement: ASTNode,
        ): String? {
            if (!myCanModifyAllWhiteSpaces && leafElement.elementType === TokenType.WHITE_SPACE) return null

            var elementTypeToUse: IElementType =
                if (isWhiteSpaceOrNls(leafElement)) leafElement.elementType else TokenType.WHITE_SPACE
            val prevNode = TreeUtil.prevLeaf(leafElement)
            if (prevNode != null && isWhiteSpaceOrNls(prevNode)) {
                elementTypeToUse = prevNode.elementType
            }
            FormatterUtil.replaceWhiteSpace(whiteSpace, leafElement, elementTypeToUse, textRange)
            return whiteSpace
        }
    }
}
