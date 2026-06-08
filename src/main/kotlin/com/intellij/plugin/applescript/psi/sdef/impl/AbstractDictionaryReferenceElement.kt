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
            getElement().resolveLocalFallback(canonicalText)?.let(res::add)
        }
        return AppleScriptResolveUtil.toCandidateInfoArray(res)
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val target = resolve()
        return when {
            target === element -> true
            element is DictionaryComponent &&
                element.getName() == getElement().getCompositeNameElement().getCompositeName() -> false
            else -> super.isReferenceTo(element)
        }
    }

    override fun getElement(): DictionaryCompositeElement = getMyElement()

    protected abstract fun getMyElement(): DictionaryCompositeElement

    override fun getRangeInElement(): TextRange = ReferenceRange.getRange(this)

    override fun getCanonicalText(): String = getElement().getCompositeNameElement().getCompositeName()

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier =
            getElement()
                .getCompositeNameElement()
                .getIdentifiers()
                .singleOrNull()
                ?.takeIf { getElement().canRenameLocalFallbackReference(resolve()) }
        val newIdentifier =
            identifier?.let {
                AppleScriptPsiElementFactory.createIdentifierFromText(getElement().project, newElementName)
            }
        if (identifier != null && newIdentifier != null) {
            identifier.parent.node.replaceChild(identifier.node, newIdentifier.node)
        }
        return getElement()
    }

    @Throws(IncorrectOperationException::class)
    override fun bindToElement(element: PsiElement): PsiElement? = null

    override fun getVariants(): Array<Any> = emptyArray()
}

private fun DictionaryCompositeElement.resolveLocalFallback(canonicalText: String): PsiElement? {
    val identifiers = getCompositeNameElement().getIdentifiers()
    return if (canResolveLocalFallback() && identifiers.size == 1) {
        val resolveProcessor = AppleScriptResolveProcessor(canonicalText)
        PsiTreeUtil.treeWalkUp(resolveProcessor, this, null, ResolveState.initial())
        (resolveProcessor.getResult() as? AppleScriptComponent)?.takeIf { it.isLocalDataSymbol() }
    } else {
        null
    }
}

private fun DictionaryCompositeElement.canResolveLocalFallback(): Boolean =
    when (this) {
        is AppleScriptDictionaryPropertyName -> true
        is AppleScriptDictionaryClassName,
        is AppleScriptDictionaryClassIdentifierPlural,
        -> !isObjectReferenceTypePosition()
        else -> false
    }

private fun DictionaryCompositeElement.isObjectReferenceTypePosition(): Boolean =
    PsiTreeUtil.getParentOfType(this, AppleScriptArbitraryReference::class.java, false) != null ||
        PsiTreeUtil.getParentOfType(this, AppleScriptCountCommandExpression::class.java, false) != null ||
        PsiTreeUtil.getParentOfType(this, AppleScriptEveryElemReference::class.java, false) != null ||
        PsiTreeUtil.getParentOfType(this, AppleScriptIndexReference::class.java, false) != null ||
        PsiTreeUtil.getParentOfType(this, AppleScriptIndexReferenceClassForm::class.java, false) != null ||
        PsiTreeUtil.getParentOfType(this, AppleScriptMiddleElemReference::class.java, false) != null

private fun DictionaryCompositeElement.canRenameLocalFallbackReference(resolvedTarget: PsiElement?): Boolean =
    canResolveLocalFallback() &&
        getCompositeNameElement().getIdentifiers().size == 1 &&
        resolvedTarget !is DictionaryComponent

private fun AppleScriptComponent.isLocalDataSymbol(): Boolean =
    isVariable() ||
        this is AppleScriptSimpleFormalParameter ||
        this is AppleScriptDirectParameterDeclaration ||
        this is AppleScriptLabeledParameterDeclarationPart
