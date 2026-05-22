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
        val element: NavigatablePsiElement? = element
        val result = ArrayList<StructureViewTreeElement>()
        val myComponents = HashSet<AppleScriptComponent>()
        if (element is AppleScriptFile) {
            if (!isRoot) {
                result.add(AppleScriptStructureViewElement(element, true))
            }
            AppleScriptPsiElementImpl.processDeclarationsImpl(
                element,
                AppleScriptComponentScopeProcessor(myComponents),
                ResolveState.initial(),
                null,
                element,
            )
        } else if (element is AppleScriptScriptObject) {
            myComponents.addAll(AppleScriptResolveUtil.getNamedSubComponentsFor(element))
        }

        for (component in myComponents) {
            when {
                component is AppleScriptHandlerPositionalParametersDefinition ->
                    result.add(AppleScriptStructureViewElement(component))
                component is AppleScriptScriptPropertyDeclaration ->
                    result.add(AppleScriptStructureViewElement(component))
                component is AppleScriptVarAccessDeclaration || component is AppleScriptVarDeclarationListPart ->
                    result.add(AppleScriptStructureViewElement(component as NavigatablePsiElement))
                component is AppleScriptScriptObject && component !== element ->
                    result.add(AppleScriptStructureViewElement(component, true))
                component is AppleScriptHandler ->
                    result.add(AppleScriptStructureViewElement(component))
                component.getName() != null &&
                    component !is AppleScriptHandlerInterleavedParametersSelectorPart &&
                    component !== element ->
                    result.add(AppleScriptStructureViewElement(component as NavigatablePsiElement))
            }
        }

        result.sortWith(Comparator { o1, o2 ->
            if (o1 is AppleScriptStructureViewElement && o2 is AppleScriptStructureViewElement) {
                val element1 = o1.element
                val element2 = o2.element
                if (element1 != null && element2 != null) {
                    return@Comparator element1.textOffset - element2.textOffset
                }
            }
            0
        })

        return result
    }

    override fun getPresentableText(): String? {
        val element: NavigatablePsiElement? = element
        return element?.presentation?.presentableText
    }
}
