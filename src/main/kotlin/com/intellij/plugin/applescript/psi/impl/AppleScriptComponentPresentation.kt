package com.intellij.plugin.applescript.psi.impl

import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptHandlerLabeledParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptHandlerPositionalParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiElement

class AppleScriptComponentPresentation(
    private val component: AppleScriptComponent,
) : AppleScriptElementPresentation(component) {
    override fun getPresentableText(): String =
        buildString {
            when {
                component.isScriptProperty() -> appendScriptProperty()
                component.isVariable() -> append(component.name)
                component.isHandler() -> appendHandler()
                else -> component.name?.let(::append)
            }
        }

    private fun StringBuilder.appendScriptProperty() {
        append(component.name)
        append(" : ")
        append(component.findAssignedValue()?.text)
    }

    private fun StringBuilder.appendHandler() {
        append(component.name)
        when (component) {
            is AppleScriptHandlerPositionalParametersDefinition -> appendPositionalParameters(component)
            is AppleScriptHandlerLabeledParametersDefinition -> appendLabeledParameters(component)
        }
    }

    private fun StringBuilder.appendPositionalParameters(handler: AppleScriptHandlerPositionalParametersDefinition) {
        val parameters = handler.formalParameterList?.formalParameters?.toList() ?: return
        append("(")
        append(parameters.joinToString(",") { it.name ?: "" })
        append(")")
    }

    private fun StringBuilder.appendLabeledParameters(handler: AppleScriptHandlerLabeledParametersDefinition) {
        val parameters = handler.labeledParameterDeclarationList
        val directParameter = parameters.directParameterDeclaration
        val labeledParameters = parameters.labeledParameterDeclarationPartList.toList()
        append(" : ")

        if (directParameter != null) {
            appendDirectParameter(directParameter)
        }
        for (labeledParameter in labeledParameters) {
            append(' ').append(labeledParameter.getHandlerParameterLabel().text).append(' ')
            append(labeledParameter.name)
        }
    }

    private fun StringBuilder.appendDirectParameter(directParameter: AppleScriptComponent) {
        val previousElement = directParameter.previousNonWhitespaceSibling()
        val previousType = previousElement?.node?.elementType
        if (previousType == AppleScriptTypes.ON || previousType == AppleScriptTypes.OF) {
            append(' ').append(previousElement.text)
        }
        append(' ').append(directParameter.name)
    }

    private fun PsiElement.previousNonWhitespaceSibling(): PsiElement? {
        var previousElement = prevSibling
        while (previousElement != null &&
            AppleScriptTokenTypesSets.WHITE_SPACES_SET.contains(previousElement.node.elementType)
        ) {
            previousElement = previousElement.prevSibling
        }
        return previousElement
    }
}
