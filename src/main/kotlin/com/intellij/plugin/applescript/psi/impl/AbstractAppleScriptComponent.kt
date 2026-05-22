package com.intellij.plugin.applescript.psi.impl

import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.plugin.applescript.lang.AppleScriptComponentType
import com.intellij.plugin.applescript.psi.AppleScriptAssignmentStatement
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptExpression
import com.intellij.plugin.applescript.psi.AppleScriptHandlerInterleavedParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptHandlerLabeledParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptHandlerPositionalParametersDefinition
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationList
import com.intellij.plugin.applescript.psi.AppleScriptPropertyReference
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.plugin.applescript.psi.AppleScriptRecordLiteralExpression
import com.intellij.plugin.applescript.psi.AppleScriptScriptPropertyDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptTargetRecordLiteral
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.plugin.applescript.psi.AppleScriptVarDeclarationListPart
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.IncorrectOperationException
import javax.swing.Icon

/**
 * Base class for AppleScript components — declarations, definitions, variables, handlers.
 * Subclasses provide `getIdentifier()`.
 */
abstract class AbstractAppleScriptComponent(node: ASTNode) :
    AppleScriptPsiElementImpl(node),
    AppleScriptComponent {

    override fun toString(): String = node.elementType.toString()

    override fun isObjectProperty(): Boolean =
        context is AppleScriptRecordLiteralExpression || context is AppleScriptTargetRecordLiteral

    override fun getOriginalDeclaration(): PsiElement? = reference?.resolve()

    override fun isHandler(): Boolean = this is AppleScriptHandlerPositionalParametersDefinition ||
        this is AppleScriptHandlerLabeledParametersDefinition ||
        this is AppleScriptHandlerInterleavedParametersDefinition

    override fun isScriptProperty(): Boolean =
        findChildByType<PsiElement>(AppleScriptTypes.PROP) != null ||
            findChildByType<PsiElement>(AppleScriptTypes.PROPERTY) != null ||
            getOriginalDeclaration() is AppleScriptScriptPropertyDeclaration

    override fun isVariable(): Boolean =
        (
            findChildByType<PsiElement>(AppleScriptTypes.LOCAL) != null ||
                findChildByType<PsiElement>(AppleScriptTypes.GLOBAL) != null
            ) ||
            (this is AppleScriptTargetVariable && firstChild is AppleScriptIdentifier) ||
            this is AppleScriptVarDeclarationListPart ||
            context is AppleScriptLabeledParameterDeclarationList

    override fun findAssignedValue(): AppleScriptExpression? {
        if (isScriptProperty() && this is AppleScriptScriptPropertyDeclaration) {
            return expression
        }
        // Parent may be AppleScriptAssignmentStatement — assignment expression not yet wired up.
        @Suppress("UNUSED_VARIABLE")
        val parentAssignment = parent as? AppleScriptAssignmentStatement
        return null
    }

    @Throws(IncorrectOperationException::class)
    override fun setName(newElementName: String): PsiElement {
        val identifier = getIdentifier()
        val identifierNew = AppleScriptPsiElementFactory.createIdentifierFromText(project, newElementName)
        if (identifierNew != null) {
            node.replaceChild(identifier.node, identifierNew.node)
        }
        return this
    }

    override fun getReference(): PsiReference? = if (this is AppleScriptTargetVariable) {
        AppleScriptTargetReferenceImpl(this)
    } else {
        null
    }

    override fun getReferences(): Array<PsiReference> = super.getReferences()

    override fun getName(): String? {
        val nameIdentifier = getNameIdentifier()
        return nameIdentifier?.text ?: node.text
    }

    override fun getTextOffset(): Int =
        getNameIdentifier()?.textOffset ?: super.getTextOffset()

    override fun getNameIdentifier(): PsiElement? = getIdentifier()

    abstract override fun getIdentifier(): AppleScriptIdentifier

    override fun getIcon(flags: Int): Icon =
        AppleScriptComponentType.typeOf(this)?.icon ?: AllIcons.General.Ellipsis

    override fun getPresentation(): ItemPresentation = object : AppleScriptElementPresentation(this) {
        override fun getPresentableText(): String {
            val result = StringBuilder()
            val thisComponent = getElement() as AppleScriptComponent

            when {
                isScriptProperty() -> {
                    val valueText = findAssignedValue()?.text
                    result.append(name).append(" : ").append(valueText)
                }

                isVariable() -> result.append(name)

                isHandler() -> {
                    result.append(name)
                    when (thisComponent) {
                        is AppleScriptHandlerPositionalParametersDefinition ->
                            appendPositionalParameters(result, thisComponent)

                        is AppleScriptHandlerLabeledParametersDefinition ->
                            appendLabeledParameters(result, thisComponent)
                    }
                }

                else -> name?.let { result.append(it) }
            }
            return result.toString()
        }

        private fun appendPositionalParameters(
            result: StringBuilder,
            handler: AppleScriptHandlerPositionalParametersDefinition,
        ) {
            val parameterList = handler.formalParameterList ?: return
            val params = parameterList.formalParameters
            result.append("(")
            result.append(params.joinToString(",") { it.name ?: "" })
            result.append(")")
        }

        private fun appendLabeledParameters(
            result: StringBuilder,
            handler: AppleScriptHandlerLabeledParametersDefinition,
        ) {
            val parameters = handler.labeledParameterDeclarationList
            val directParameter = parameters.directParameterDeclaration
            val labeledParams = parameters.labeledParameterDeclarationPartList
            result.append(" : ")

            if (directParameter != null) {
                var prevElement: PsiElement? = directParameter.prevSibling
                while (prevElement != null &&
                    AppleScriptTokenTypesSets.WHITE_SPACES_SET.contains(prevElement.node.elementType)
                ) {
                    prevElement = prevElement.prevSibling
                }
                val prevType = prevElement?.node?.elementType
                if (prevType == AppleScriptTypes.ON || prevType == AppleScriptTypes.OF) {
                    result.append(' ').append(prevElement.text)
                }
                result.append(' ').append(directParameter.name)
            }
            for (labeledParam in labeledParams) {
                result.append(' ').append(labeledParam.handlerParameterLabel.text).append(' ')
                result.append(labeledParam.name)
            }
            setTextAttributesKey(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
        }
    }
}
