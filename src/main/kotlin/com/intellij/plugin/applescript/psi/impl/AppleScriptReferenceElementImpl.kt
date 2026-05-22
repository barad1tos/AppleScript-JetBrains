package com.intellij.plugin.applescript.psi.impl

import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UnfairTextRange
import com.intellij.plugin.applescript.lang.AppleScriptComponentType
import com.intellij.plugin.applescript.lang.resolve.AppleScriptComponentScopeResolver
import com.intellij.plugin.applescript.lang.resolve.AppleScriptDictionaryResolveProcessor
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolveUtil
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolver
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.lang.util.ScopeUtil
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptHandlerPositionalParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.plugin.applescript.psi.AppleScriptReferenceElement
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

abstract class AppleScriptReferenceElementImpl(node: ASTNode) :
    AppleScriptExpressionImpl(node),
    AppleScriptReferenceElement,
    PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> = multiResolveInner(incompleteCode)

    protected open fun multiResolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val elements = ResolveCache.getInstance(project)
            .resolveWithCaching(this, AppleScriptResolver, true, incompleteCode)
        return AppleScriptResolveUtil.toCandidateInfoArray(elements)
    }

    override fun getReference(): PsiReference = this

    override fun getElement(): PsiElement = this

    override fun getRangeInElement(): TextRange {
        val textRange = textRange
        val refs: Array<out AppleScriptReferenceElement>? =
            PsiTreeUtil.getChildrenOfType(this, AppleScriptReferenceElement::class.java)
        if (refs != null && refs.isNotEmpty()) {
            val last = refs[refs.size - 1].textRange
            return UnfairTextRange(
                last.startOffset - textRange.startOffset,
                last.endOffset - textRange.endOffset,
            )
        }
        return UnfairTextRange(0, textRange.endOffset - textRange.startOffset)
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(true)
        return when {
            results.isEmpty() -> null
            !results[0].isValidResult -> null
            else -> results[0].element
        }
    }

    override fun getCanonicalText(): String = text

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = PsiTreeUtil.getChildOfType(this, AppleScriptIdentifier::class.java)
        val identifierNew = AppleScriptPsiElementFactory.createIdentifierFromText(project, newElementName)
        if (identifierNew != null && identifier != null) {
            node.replaceChild(identifier.node, identifierNew.node)
        }
        return this
    }

    @Throws(IncorrectOperationException::class)
    override fun bindToElement(element: PsiElement): PsiElement = this

    override fun isReferenceTo(element: PsiElement): Boolean {
        val target = resolve()
        if (target is AppleScriptTargetVariable) {
            val theirName = target.name
            val ourName = canonicalText
            if (ourName == theirName) {
                val ourScopeOwner: PsiElement? = ScopeUtil.getMaxLocalScopeForTargetOrReference(getElement())
                val theirScopeOwner: PsiElement? = ScopeUtil.getMaxLocalScopeForTargetOrReference(element)
                if (ourScopeOwner === theirScopeOwner) return true
            }
        }
        return target === element
    }

    override fun getVariants(): Array<Any> {
        val elements: List<PsiElement>? = ResolveCache.getInstance(project)
            .resolveWithCaching(this, AppleScriptComponentScopeResolver, true, true)

        val resolveProcessor = AppleScriptDictionaryResolveProcessor(project, canonicalText)
        val state = ResolveState.initial()
            .put(AppleScriptDictionaryResolveProcessor.COLLECT_ALL_DECLARATIONS, true)
        PsiTreeUtil.treeWalkUp(resolveProcessor, getElement(), null, state)
        val dictionaryComponents: List<DictionaryComponent> = resolveProcessor.getFilteredResult()

        val lookupElements = mutableListOf<LookupElement>()
        elements?.forEach { addLookupElement(lookupElements, it) }
        dictionaryComponents.forEach { addLookupElement(lookupElements, it) }
        addAppleScriptCommandsSuite(lookupElements)
        @Suppress("UNCHECKED_CAST")
        return if (lookupElements.isNotEmpty()) lookupElements.toTypedArray<Any>() else LookupElement.EMPTY_ARRAY as Array<Any>
    }

    private fun addAppleScriptCommandsSuite(lookupElements: MutableList<LookupElement>) {
        lookupElements.add(
            LookupElementBuilder.create(AppleScriptTypes.LAUNCH.toString().lowercase()).bold()
                .withTypeText("command", true)
                .withIcon(AppleScriptComponentType.HANDLER.icon),
        )
        lookupElements.add(
            LookupElementBuilder.create(AppleScriptTypes.ACTIVATE.toString().lowercase()).bold()
                .withTypeText("command", true)
                .withIcon(AppleScriptComponentType.HANDLER.icon),
        )
    }

    private fun addLookupElement(lookupElements: MutableList<LookupElement>, el: PsiElement) {
        if (!el.isValid) return
        var builder: LookupElementBuilder = when (el) {
            is DictionaryComponent -> {
                val dName = el.getDictionary().name
                LookupElementBuilder.createWithIcon(el).appendTailText("   $dName", true)
            }

            is AppleScriptComponent -> {
                var b = LookupElementBuilder.createWithIcon(el)
                if (el is AppleScriptHandlerPositionalParametersDefinition) {
                    b = b.withInsertHandler(
                        if (el.formalParameterList != null) {
                            ParenthesesInsertHandler.WITH_PARAMETERS
                        } else {
                            ParenthesesInsertHandler.NO_PARAMETERS
                        },
                    )
                }
                b
            }

            else -> LookupElementBuilder.create(el)
        }
        val componentType = AppleScriptComponentType.typeOf(el)
        val typeText = componentType?.toString()?.lowercase()
        builder = builder.withTypeText(typeText, null, true)
        lookupElements.add(builder)
    }

    override fun isSoft(): Boolean = false
}
