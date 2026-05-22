package com.intellij.plugin.applescript.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolveProcessor
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolveUtil
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerArgument
import com.intellij.plugin.applescript.psi.AppleScriptHandlerCall
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ReferenceRange
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.resolve.reference.impl.PsiPolyVariantCachingReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

abstract class AbstractAppleScriptHandlerCall(node: ASTNode) :
    AppleScriptPsiElementImpl(node),
    AppleScriptHandlerCall {

    private val mySelectorReference = SelectorReference()

    override fun getHandlerSelector(): String = buildString {
        for (argument in getArguments()) {
            append(argument.argumentSelector.selectorName)
        }
    }

    override fun getReference(): PsiReference = mySelectorReference

    override fun getArguments(): List<AppleScriptHandlerArgument> =
        findChildrenByType(AppleScriptTypes.HANDLER_ARGUMENT)

    private inner class SelectorReference :
        PsiPolyVariantCachingReference(),
        MultiRangeReference,
        PsiPolyVariantReference {

        override fun getRanges(): List<TextRange> {
            val parentOffset = -getElement().textRange.startOffset
            val result = mutableListOf<TextRange>()
            for (argument in getArguments()) {
                val id = argument.argumentSelector.selectorIdentifier ?: continue
                val argumentRange = id.textRange
                if (!argumentRange.isEmpty) {
                    result.add(argumentRange.shiftRight(parentOffset))
                }
            }
            return result
        }

        override fun resolveInner(incompleteCode: Boolean, containingFile: PsiFile): Array<ResolveResult> {
            val resolveProcessor = AppleScriptResolveProcessor(getHandlerSelector())
            PsiTreeUtil.treeWalkUp(resolveProcessor, this@AbstractAppleScriptHandlerCall, null, ResolveState.initial())
            return AppleScriptResolveUtil.toCandidateInfoArray(listOf(resolveProcessor.getResult()))
        }

        override fun isReferenceTo(element: PsiElement): Boolean {
            val target = resolve()
            return if (element is AppleScriptHandler && target != null) {
                target === element
            } else {
                super.isReferenceTo(element)
            }
        }

        override fun getElement(): PsiElement = this@AbstractAppleScriptHandlerCall

        override fun getRangeInElement(): TextRange = ReferenceRange.getRange(this)

        override fun getCanonicalText(): String = getHandlerSelector()

        @Throws(IncorrectOperationException::class)
        override fun handleElementRename(newElementName: String): PsiElement {
            val selectorParts = getArguments().size
            val newParts = newElementName.split(":")
            if (newParts.size == selectorParts) {
                for (index in 0 until selectorParts) {
                    val myId: AppleScriptIdentifier? = getArguments()[index].argumentSelector.selectorIdentifier
                    val newId = AppleScriptPsiElementFactory.createIdentifierFromText(project, newParts[index])
                    if (newId != null && myId != null) myId.replace(newId)
                }
            } else {
                val myIdentifier: AppleScriptIdentifier? = getArguments()[0].argumentSelector.selectorIdentifier
                val identifierNew = AppleScriptPsiElementFactory.createIdentifierFromText(project, newElementName)
                if (identifierNew != null && myIdentifier != null) myIdentifier.replace(identifierNew)
            }
            return this@AbstractAppleScriptHandlerCall
        }

        @Throws(IncorrectOperationException::class)
        override fun bindToElement(element: PsiElement): PsiElement? = null

        override fun getVariants(): Array<Any> = emptyArray()
    }
}
