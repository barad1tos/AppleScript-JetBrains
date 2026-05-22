package com.intellij.plugin.applescript.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptAssignmentStatement
import com.intellij.plugin.applescript.psi.AppleScriptBlockBody
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptFormalParameterList
import com.intellij.plugin.applescript.psi.AppleScriptHandlerLabeledParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerSelectorPart
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationList
import com.intellij.plugin.applescript.psi.AppleScriptObjectTargetPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.AppleScriptUseStatement
import com.intellij.plugin.applescript.psi.AppleScriptVarDeclarationList
import com.intellij.plugin.applescript.psi.sdef.ApplicationDictionaryDeclarator
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor

open class AppleScriptPsiElementImpl(node: ASTNode) :
    ASTWrapperPsiElement(node),
    AppleScriptPsiElement {

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement,
    ): Boolean =
        processDeclarationsImpl(this, processor, state, lastParent, place) &&
            super.processDeclarations(processor, state, lastParent, place)

    companion object {

        @JvmStatic
        fun processDeclarationsImpl(
            context: PsiElement?,
            processor: PsiScopeProcessor,
            state: ResolveState,
            lastParent: PsiElement?,
            referencingElement: PsiElement?,
        ): Boolean {
            if (context == null) return true

            val result: MutableSet<AppleScriptPsiElement> = LinkedHashSet()

            for (child in context.children) {
                if (child === lastParent && child is AppleScriptBlockBody) continue

                when (child) {
                    is AppleScriptVarDeclarationList -> {
                        result.add(child.varAccessDeclaration)
                        result.addAll(child.varDeclarationListPartList)
                    }

                    is AppleScriptFormalParameterList ->
                        result.addAll(child.formalParameters)

                    is AppleScriptHandlerSelectorPart ->
                        result.addAll(child.findParameters())

                    is AppleScriptLabeledParameterDeclarationList ->
                        result.addAll(child.componentList)

                    is AppleScriptObjectTargetPropertyDeclaration -> {
                        val ctx = child.context
                        if (ctx is AppleScriptHandlerLabeledParametersDefinition || ctx is AppleScriptHandler) {
                            child.targetVariable?.let { result.add(it) }
                        }
                    }

                    is AppleScriptAssignmentStatement ->
                        result.addAll(child.targets)

                    is AppleScriptComponent ->
                        result.add(child)

                    is AppleScriptUseStatement ->
                        result.add(child)
                }
            }

            if (referencingElement != null &&
                context is ApplicationDictionaryDeclarator &&
                context !is AppleScriptUseStatement
            ) {
                result.add(context)
            }

            for (component in result) {
                if (referencingElement == null || canBeReferenced(referencingElement, component)) {
                    if (!processor.execute(component, state)) return false
                }
            }
            return true
        }

        private fun canBeReferenced(
            referencingElement: PsiElement,
            component: AppleScriptPsiElement,
        ): Boolean {
            val dictionaryElementMatches = component is ApplicationDictionaryDeclarator &&
                referencingElement is AppleScriptPsiElement &&
                AppleScriptPsiImplUtil.isBefore(component, referencingElement, true)

            return referencingElement is com.intellij.plugin.applescript.psi.AppleScriptObjectPropertyDeclaration ||
                referencingElement is com.intellij.plugin.applescript.psi.AppleScriptHandlerCall ||
                referencingElement.parent is com.intellij.plugin.applescript.psi.AppleScriptHandlerPositionalParametersCallExpression ||
                AppleScriptPsiImplUtil.isBefore(component, referencingElement, true) ||
                dictionaryElementMatches
        }
    }
}
