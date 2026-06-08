package com.intellij.plugin.applescript.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptAssignmentStatement
import com.intellij.plugin.applescript.psi.AppleScriptBlockBody
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptFormalParameterList
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerCall
import com.intellij.plugin.applescript.psi.AppleScriptHandlerLabeledParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptHandlerPositionalParametersCallExpression
import com.intellij.plugin.applescript.psi.AppleScriptHandlerSelectorPart
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationList
import com.intellij.plugin.applescript.psi.AppleScriptObjectPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptObjectTargetPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.AppleScriptUseStatement
import com.intellij.plugin.applescript.psi.AppleScriptVarDeclarationList
import com.intellij.plugin.applescript.psi.sdef.ApplicationDictionaryDeclarator
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor

open class AppleScriptPsiElementImpl(
    node: ASTNode,
) : ASTWrapperPsiElement(node),
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

            val declarations = collectDeclarations(context, lastParent, referencingElement)
            return processDeclarations(declarations, processor, state, referencingElement)
        }

        private fun collectDeclarations(
            context: PsiElement,
            lastParent: PsiElement?,
            referencingElement: PsiElement?,
        ): Set<AppleScriptPsiElement> {
            val result = LinkedHashSet<AppleScriptPsiElement>()
            for (child in context.children) {
                if (child === lastParent && child is AppleScriptBlockBody) continue

                result.addDeclarationsFrom(child)
            }

            dictionaryDeclaratorDeclaration(context, referencingElement)?.let(result::add)

            return result
        }

        private fun MutableSet<AppleScriptPsiElement>.addDeclarationsFrom(child: PsiElement) {
            when (child) {
                is AppleScriptVarDeclarationList -> {
                    add(child.varAccessDeclaration)
                    addAll(child.varDeclarationListPartList)
                }

                is AppleScriptFormalParameterList ->
                    addAll(child.formalParameters)

                is AppleScriptHandlerSelectorPart ->
                    addAll(child.findParameters())

                is AppleScriptLabeledParameterDeclarationList ->
                    addAll(child.componentList)

                is AppleScriptObjectTargetPropertyDeclaration ->
                    child.targetVariable
                        ?.takeIf { child.context.isHandlerDefinitionContext() }
                        ?.let(::add)

                is AppleScriptAssignmentStatement ->
                    addAll(child.targets)

                is AppleScriptComponent ->
                    add(child)

                is AppleScriptUseStatement ->
                    add(child)
            }
        }

        private fun PsiElement?.isHandlerDefinitionContext(): Boolean =
            this is AppleScriptHandlerLabeledParametersDefinition || this is AppleScriptHandler

        private fun dictionaryDeclaratorDeclaration(
            context: PsiElement,
            referencingElement: PsiElement?,
        ): ApplicationDictionaryDeclarator? =
            if (
                referencingElement != null &&
                context is ApplicationDictionaryDeclarator &&
                context !is AppleScriptUseStatement
            ) {
                context
            } else {
                null
            }

        private fun processDeclarations(
            declarations: Set<AppleScriptPsiElement>,
            processor: PsiScopeProcessor,
            state: ResolveState,
            referencingElement: PsiElement?,
        ): Boolean =
            declarations.all { component ->
                if (referencingElement == null || canBeReferenced(referencingElement, component)) {
                    processor.execute(component, state)
                } else {
                    true
                }
            }

        private fun canBeReferenced(
            referencingElement: PsiElement,
            component: AppleScriptPsiElement,
        ): Boolean {
            val dictionaryElementMatches =
                component is ApplicationDictionaryDeclarator &&
                    referencingElement is AppleScriptPsiElement &&
                    isBefore(component, referencingElement, true)

            return referencingElement is AppleScriptObjectPropertyDeclaration ||
                referencingElement is AppleScriptHandlerCall ||
                referencingElement.parent is AppleScriptHandlerPositionalParametersCallExpression ||
                isBefore(component, referencingElement, true) ||
                dictionaryElementMatches
        }
    }
}
