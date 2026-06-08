package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.openapi.util.TextRange
import com.intellij.plugin.applescript.lang.resolve.AppleScriptDictionaryResolveProcessor
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolveProcessor
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolveUtil
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.AppleScriptArbitraryReference
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptCountCommandExpression
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryClassIdentifierPlural
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryClassName
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryPropertyName
import com.intellij.plugin.applescript.psi.AppleScriptDirectParameterDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptEveryElemReference
import com.intellij.plugin.applescript.psi.AppleScriptIndexReference
import com.intellij.plugin.applescript.psi.AppleScriptIndexReferenceClassForm
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationPart
import com.intellij.plugin.applescript.psi.AppleScriptMiddleElemReference
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.plugin.applescript.psi.AppleScriptSimpleFormalParameter
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

    override fun resolveInner(
        incompleteCode: Boolean,
        containingFile: PsiFile,
    ): Array<ResolveResult> {
        val resolveProcessor = AppleScriptDictionaryResolveProcessor(getElement(), canonicalText)
        val maxScope = AppleScriptResolveUtil.getTellStatementResolveScope(getElement())
        val res = ArrayList<PsiElement>()
        var resolveResult: DictionaryComponent?
        if (!maxScope.isEmpty()) {
            for (el in maxScope) {
                PsiTreeUtil.treeWalkUp(resolveProcessor, getElement(), el, ResolveState.initial())
                resolveResult = resolveProcessor.getMyResult()
                if (resolveResult != null) {
                    res.add(resolveResult)
                    break
                }
            }
        }
        if (res.isEmpty()) {
            PsiTreeUtil.treeWalkUp(resolveProcessor, getElement(), null, ResolveState.initial())
            resolveResult = resolveProcessor.getMyResult()
            if (resolveResult != null) {
                res.add(resolveResult)
            }
        }
        if (res.isEmpty()) {
            resolveLocalFallback()?.let(res::add)
        }
        return AppleScriptResolveUtil.toCandidateInfoArray(res)
    }

    private fun resolveLocalFallback(): PsiElement? {
        if (!canResolveLocalFallback()) return null
        if (getElement().getCompositeNameElement().getIdentifiers().size != 1) return null
        val resolveProcessor = AppleScriptResolveProcessor(canonicalText)
        PsiTreeUtil.treeWalkUp(resolveProcessor, getElement(), null, ResolveState.initial())
        val resolved = resolveProcessor.getResult()
        return if (resolved is AppleScriptComponent && resolved.isLocalDataSymbol()) resolved else null
    }

    private fun canResolveLocalFallback(): Boolean =
        when (getElement()) {
            is AppleScriptDictionaryPropertyName -> true
            is AppleScriptDictionaryClassName,
            is AppleScriptDictionaryClassIdentifierPlural,
            -> !isObjectReferenceTypePosition()
            else -> false
        }

    private fun isObjectReferenceTypePosition(): Boolean =
        PsiTreeUtil.getParentOfType(getElement(), AppleScriptArbitraryReference::class.java, false) != null ||
            PsiTreeUtil.getParentOfType(getElement(), AppleScriptCountCommandExpression::class.java, false) != null ||
            PsiTreeUtil.getParentOfType(getElement(), AppleScriptEveryElemReference::class.java, false) != null ||
            PsiTreeUtil.getParentOfType(getElement(), AppleScriptIndexReference::class.java, false) != null ||
            PsiTreeUtil.getParentOfType(getElement(), AppleScriptIndexReferenceClassForm::class.java, false) != null ||
            PsiTreeUtil.getParentOfType(getElement(), AppleScriptMiddleElemReference::class.java, false) != null

    override fun isReferenceTo(element: PsiElement): Boolean {
        val localTarget = resolveLocalFallback()
        if (localTarget === element) return true
        if (element is DictionaryComponent &&
            element.getName() == getElement().getCompositeNameElement().getCompositeName()
        ) {
            return resolve() === element
        }
        return super.isReferenceTo(element)
    }

    override fun getElement(): DictionaryCompositeElement = getMyElement()

    protected abstract fun getMyElement(): DictionaryCompositeElement

    override fun getRangeInElement(): TextRange = ReferenceRange.getRange(this)

    override fun getCanonicalText(): String = getElement().getCompositeNameElement().getCompositeName()

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement {
        if (!canRenameLocalFallbackReference()) return getElement()
        val identifier = getElement().getCompositeNameElement().getIdentifiers().singleOrNull() ?: return getElement()
        val newIdentifier = AppleScriptPsiElementFactory.createIdentifierFromText(getElement().project, newElementName)
        if (newIdentifier != null) {
            identifier.parent.node.replaceChild(identifier.node, newIdentifier.node)
        }
        return getElement()
    }

    private fun canRenameLocalFallbackReference(): Boolean =
        canResolveLocalFallback() &&
            getElement().getCompositeNameElement().getIdentifiers().size == 1 &&
            resolve() !is DictionaryComponent

    @Throws(IncorrectOperationException::class)
    override fun bindToElement(element: PsiElement): PsiElement? = null

    override fun getVariants(): Array<Any> = emptyArray()
}

private fun AppleScriptComponent.isLocalDataSymbol(): Boolean =
    isVariable() ||
        this is AppleScriptSimpleFormalParameter ||
        this is AppleScriptDirectParameterDeclaration ||
        this is AppleScriptLabeledParameterDeclarationPart
