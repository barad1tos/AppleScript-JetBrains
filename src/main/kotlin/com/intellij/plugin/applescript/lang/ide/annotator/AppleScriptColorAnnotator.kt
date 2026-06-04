package com.intellij.plugin.applescript.lang.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterColors
import com.intellij.plugin.applescript.lang.ide.intentions.RenameParameterLabelQuickFix
import com.intellij.plugin.applescript.psi.AppleScriptAppleScriptProperty
import com.intellij.plugin.applescript.psi.AppleScriptApplicationReference
import com.intellij.plugin.applescript.psi.AppleScriptBuiltInClassIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptCommandParameterSelector
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryClassIdentifierPlural
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryClassName
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryCommandName
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryConstant
import com.intellij.plugin.applescript.psi.AppleScriptDictionaryPropertyName
import com.intellij.plugin.applescript.psi.AppleScriptExpression
import com.intellij.plugin.applescript.psi.AppleScriptHandlerCall
import com.intellij.plugin.applescript.psi.AppleScriptHandlerParameterLabel
import com.intellij.plugin.applescript.psi.AppleScriptIncompleteExpression
import com.intellij.plugin.applescript.psi.AppleScriptNumericConstant
import com.intellij.plugin.applescript.psi.AppleScriptPropertyReference
import com.intellij.plugin.applescript.psi.AppleScriptReferenceElement
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.HANDLER_PARAMETER_LABELS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_LABELED_PARAMETERS_CALL_EXPRESSION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_LABELED_PARAMETERS_DEFINITION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_PARAMETER_LABEL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LPAREN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class AppleScriptColorAnnotator : Annotator {
    override fun annotate(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        AppleScriptAnnotationSupport.annotate(element, holder)
    }
}

private object AppleScriptAnnotationSupport {
    fun annotate(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        annotateUnresolvedHandlerCall(element, holder)
        when (element) {
            is AppleScriptHandlerCall -> annotateInterleavedHandlerCall(element, holder)
            is AppleScriptReferenceElement -> annotateMyPositionalHandlerCall(element, holder)
        }
        annotateElementColor(element, holder)
        annotateDuplicatedParameterLabels(element, holder)
    }

    private fun annotateUnresolvedHandlerCall(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        if (element !is AppleScriptHandlerCall) return
        if (element.reference.resolve() != null) return

        for (argument in element.getArguments()) {
            createInfoAnnotation(
                holder,
                argument.getArgumentSelector(),
                AppleScriptSyntaxHighlighterColors.UNRESOLVED_REFERENCE,
            )
        }
    }

    private fun annotateInterleavedHandlerCall(
        element: AppleScriptHandlerCall,
        holder: AnnotationHolder,
    ) {
        for (argument in element.getArguments()) {
            createInfoAnnotation(
                holder,
                argument.argumentSelector.selectorIdentifier,
                AppleScriptSyntaxHighlighterColors.HANDLER_CALL,
            )
        }
    }

    private fun annotateMyPositionalHandlerCall(
        element: AppleScriptReferenceElement,
        holder: AnnotationHolder,
    ) {
        if (!AppleScriptAnnotationPredicates.isMyPositionalHandlerCallName(element)) return

        createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.HANDLER_CALL)
    }

    private fun annotateElementColor(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        when (element) {
            is AppleScriptNumericConstant,
            is AppleScriptPropertyReference,
            -> {
                if (AppleScriptAnnotationPredicates.isDatePropertyReferenceTerm(element)) {
                    createInfoAnnotation(
                        holder,
                        element,
                        AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR,
                    )
                }
            }
            is AppleScriptDictionaryCommandName ->
                createInfoAnnotation(
                    holder,
                    element,
                    AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
                )
            is AppleScriptCommandParameterSelector ->
                createInfoAnnotation(
                    holder,
                    element,
                    AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
                )
            is AppleScriptDictionaryClassName,
            is AppleScriptDictionaryClassIdentifierPlural,
            is AppleScriptBuiltInClassIdentifier,
            -> annotateClassTerm(holder, element)
            is AppleScriptAppleScriptProperty,
            is AppleScriptDictionaryPropertyName,
            ->
                createInfoAnnotation(
                    holder,
                    element,
                    AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR,
                )
            is AppleScriptDictionaryConstant -> annotateDictionaryConstant(holder, element)
            is AppleScriptApplicationReference ->
                AppleScriptApplicationReferenceAnnotator.annotate(
                    holder,
                    element,
                    false,
                )
            is AppleScriptIncompleteExpression -> annotateIncompleteExpression(holder, element)
        }
    }

    private fun annotateIncompleteExpression(
        holder: AnnotationHolder,
        element: AppleScriptIncompleteExpression,
    ) {
        val expression = PsiTreeUtil.findChildOfType(element, AppleScriptExpression::class.java)
        val appRef = PsiTreeUtil.findChildOfType(expression, AppleScriptApplicationReference::class.java)
        if (appRef != null) {
            AppleScriptApplicationReferenceAnnotator.annotate(holder, appRef, true)
        }
        holder
            .newAnnotation(HighlightSeverity.ERROR, "Incomplete expression")
            .range(element)
            .create()
    }

    private fun annotateDuplicatedParameterLabels(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        val elementType = element.node.elementType
        if (elementType !== HANDLER_LABELED_PARAMETERS_DEFINITION &&
            elementType !== HANDLER_LABELED_PARAMETERS_CALL_EXPRESSION
        ) {
            return
        }

        val labelNames = mutableSetOf<String>()
        for (childElement in element.children) {
            if (childElement.node.elementType !== HANDLER_PARAMETER_LABEL) continue
            val labelName = childElement.text
            if (!labelNames.add(labelName)) {
                annotateDuplicatedParameterLabel(holder, childElement, labelName, labelNames)
            }
        }
    }

    private fun annotateDuplicatedParameterLabel(
        holder: AnnotationHolder,
        childElement: PsiElement,
        labelName: String,
        labelNames: Set<String>,
    ) {
        val newLabelName = firstAvailableParameterLabel(labelNames)
        holder
            .newAnnotation(HighlightSeverity.ERROR, "Duplicated parameter label '$labelName'")
            .range(childElement)
            .withFix(
                RenameParameterLabelQuickFix(
                    childElement as AppleScriptHandlerParameterLabel,
                    newLabelName,
                ),
            ).create()
    }

    private fun annotateClassTerm(
        holder: AnnotationHolder,
        element: PsiElement,
    ) {
        val attributeKey =
            if (
                AppleScriptAnnotationPredicates.hasPropertyReferenceParent(element) ||
                AppleScriptAnnotationPredicates.isClassPropertyReferenceTerm(element)
            ) {
                AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR
            } else {
                AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR
            }
        createInfoAnnotation(holder, element, attributeKey)
    }

    private fun annotateDictionaryConstant(
        holder: AnnotationHolder,
        element: AppleScriptDictionaryConstant,
    ) {
        val attributeKey =
            if (AppleScriptAnnotationPredicates.hasPropertyReferenceParent(element)) {
                AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR
            } else {
                AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR
            }
        createInfoAnnotation(holder, element, attributeKey)
    }
}

private fun firstAvailableParameterLabel(labelNames: Set<String>): String =
    HANDLER_PARAMETER_LABELS.types
        .asSequence()
        .map { it.toString().lowercase().replace("_", " ") }
        .firstOrNull { it !in labelNames }
        ?: ""

private fun createInfoAnnotation(
    holder: AnnotationHolder,
    element: PsiElement?,
    attributeKey: TextAttributesKey?,
) {
    if (element == null || attributeKey == null) return

    holder
        .newAnnotation(HighlightSeverity.INFORMATION, "")
        .range(element)
        .textAttributes(attributeKey)
        .create()
}

private object AppleScriptAnnotationPredicates {
    fun hasPropertyReferenceParent(element: PsiElement): Boolean {
        var parent = element.parent
        while (parent != null) {
            if (parent is AppleScriptPropertyReference) return true
            parent = parent.parent
        }
        return false
    }

    fun isClassPropertyReferenceTerm(element: PsiElement): Boolean =
        element.text.equals(CLASS_PROPERTY_TERM, ignoreCase = true) &&
            PsiTreeUtil.nextVisibleLeaf(element)?.node?.elementType === OF

    fun isDatePropertyReferenceTerm(element: PsiElement): Boolean =
        element.text
            .trim()
            .substringBefore(" ")
            .lowercase() in DATE_PROPERTY_TERMS &&
            isPropertyReferenceOperator(PsiTreeUtil.nextVisibleLeaf(element))

    fun isMyPositionalHandlerCallName(element: AppleScriptReferenceElement): Boolean =
        PsiTreeUtil.prevVisibleLeaf(element)?.node?.elementType === MY &&
            PsiTreeUtil.nextVisibleLeaf(element)?.node?.elementType === LPAREN

    private fun isPropertyReferenceOperator(element: PsiElement?): Boolean =
        element?.node?.elementType.let { elementType ->
            elementType === OF || elementType === IN
        }

    private const val CLASS_PROPERTY_TERM = "class"

    private val DATE_PROPERTY_TERMS =
        setOf(
            "class",
            "year",
            "month",
            "day",
            "hours",
            "minutes",
            "seconds",
        )
}
