package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.lang.resolve.AppleScriptDictionaryResolveProcessor
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolveUtil
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeElement
import com.intellij.plugin.applescript.psi.sdef.DictionaryReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.ReferenceRange
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.resolve.reference.impl.PsiPolyVariantCachingReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

abstract class AbstractDictionaryReferenceElement :
    PsiPolyVariantCachingReference(),
    DictionaryReference {

    override fun getRanges(): List<TextRange> {
        val result = ArrayList<TextRange>()
        val parentOffset = -getElement().textRange.startOffset
        for (id in getElement().getCompositeNameElement().getIdentifiers()) {
            val argumentRange = id.textRange
            if (!argumentRange.isEmpty) {
                result.add(argumentRange.shiftRight(parentOffset))
            }
        }
        return result
    }

    override fun resolveInner(incompleteCode: Boolean, containingFile: PsiFile): Array<ResolveResult> {
        val resolveProcessor = AppleScriptDictionaryResolveProcessor(getElement(), canonicalText)
        val maxScope = AppleScriptResolveUtil.getTellStatementResolveScope(getElement())
        val res = ArrayList<PsiElement>()
        var resolveResult: DictionaryComponent?
        if (!maxScope.isEmpty()) {
            for (el in maxScope) {
                PsiTreeUtil.treeWalkUp(resolveProcessor, getElement(), el, ResolveState.initial())
                resolveResult = resolveProcessor.myResult
                if (resolveResult != null) {
                    res.add(resolveResult)
                    break
                }
            }
        }
        if (res.isEmpty()) {
            PsiTreeUtil.treeWalkUp(resolveProcessor, getElement(), null, ResolveState.initial())
            resolveResult = resolveProcessor.myResult
            if (resolveResult != null) {
                res.add(resolveResult)
            }
        }
        return AppleScriptResolveUtil.toCandidateInfoArray(res)
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is DictionaryComponent) {
            if (element.getName() == getElement().getCompositeNameElement().getCompositeName()) {
                return resolve() === element
            }
        }
        return super.isReferenceTo(element)
    }

    override fun getElement(): DictionaryCompositeElement = getMyElement()

    protected abstract fun getMyElement(): DictionaryCompositeElement

    override fun getRangeInElement(): TextRange = ReferenceRange.getRange(this)

    override fun getCanonicalText(): String = getElement().getCompositeNameElement().getCompositeName()

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement = getElement()

    @Throws(IncorrectOperationException::class)
    override fun bindToElement(element: PsiElement): PsiElement? = null

    override fun getVariants(): Array<Any> = emptyArray()
}
