package com.intellij.plugin.applescript.lang.ide.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.navigation.ItemPresentation
import com.intellij.plugin.applescript.AppleScriptFile
import com.intellij.plugin.applescript.lang.resolve.AppleScriptComponentScopeProcessor
import com.intellij.plugin.applescript.lang.resolve.AppleScriptResolveUtil
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerInterleavedParametersSelectorPart
import com.intellij.plugin.applescript.psi.AppleScriptHandlerPositionalParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptScriptObject
import com.intellij.plugin.applescript.psi.AppleScriptScriptPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptVarAccessDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptVarDeclarationListPart
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiElementImpl
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.ResolveState

class AppleScriptStructureViewElement :
    PsiTreeElementBase<NavigatablePsiElement>,
    ItemPresentation,
    StructureViewTreeElement {
    private var isRoot: Boolean = false

    internal constructor(element: NavigatablePsiElement) : super(element) {
        isRoot = false
    }

    private constructor(element: NavigatablePsiElement, isRootElement: Boolean) : super(element) {
        this.isRoot = isRootElement
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        val currentElement: NavigatablePsiElement? = element
        val result = mutableListOf<StructureViewTreeElement>()

        if (currentElement is AppleScriptFile && !isRoot) {
            result.add(AppleScriptStructureViewElement(currentElement, true))
            return result
        }

        collectComponents(currentElement)
            .mapNotNull { component -> component.toStructureViewElement(currentElement) }
            .forEach(result::add)

        return result.sortedByTextOffset()
    }

    override fun getPresentableText(): String? {
        val element: NavigatablePsiElement? = element
        return element?.presentation?.presentableText
    }

    private fun collectComponents(element: NavigatablePsiElement?): Set<AppleScriptComponent> {
        val components = HashSet<AppleScriptComponent>()
        when (element) {
            is AppleScriptFile ->
                AppleScriptPsiElementImpl.processDeclarationsImpl(
                    element,
                    AppleScriptComponentScopeProcessor(components),
                    ResolveState.initial(),
                    null,
                    null,
                )
            is AppleScriptScriptObject ->
                components.addAll(AppleScriptResolveUtil.getNamedSubComponentsFor(element))
        }
        return components
    }

    private fun AppleScriptComponent.toStructureViewElement(owner: NavigatablePsiElement?): StructureViewTreeElement? =
        when {
            this is AppleScriptHandlerPositionalParametersDefinition ->
                AppleScriptStructureViewElement(this)
            this is AppleScriptScriptPropertyDeclaration ->
                AppleScriptStructureViewElement(this)
            this is AppleScriptVarAccessDeclaration || this is AppleScriptVarDeclarationListPart ->
                AppleScriptStructureViewElement(this as NavigatablePsiElement)
            this is AppleScriptScriptObject && this !== owner ->
                AppleScriptStructureViewElement(this, true)
            this is AppleScriptHandler ->
                AppleScriptStructureViewElement(this)
            shouldShowNamedComponent(owner) ->
                AppleScriptStructureViewElement(this as NavigatablePsiElement)
            else -> null
        }

    private fun AppleScriptComponent.shouldShowNamedComponent(owner: NavigatablePsiElement?): Boolean =
        name != null &&
            this !is AppleScriptHandlerInterleavedParametersSelectorPart &&
            this !== owner

    private fun Collection<StructureViewTreeElement>.sortedByTextOffset(): List<StructureViewTreeElement> =
        sortedBy { treeElement ->
            (treeElement as? AppleScriptStructureViewElement)
                ?.element
                ?.textOffset ?: 0
        }
}
