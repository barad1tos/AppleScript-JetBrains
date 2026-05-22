package com.intellij.plugin.applescript.lang.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterColors
import com.intellij.plugin.applescript.lang.ide.intentions.AddApplicationDictionaryQuickFix
import com.intellij.plugin.applescript.lang.ide.intentions.RenameParameterLabelQuickFix
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
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
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.HANDLER_PARAMETER_LABELS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_LABELED_PARAMETERS_CALL_EXPRESSION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_LABELED_PARAMETERS_DEFINITION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_PARAMETER_LABEL
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiImplUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class AppleScriptColorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val elementType = element.node.elementType
        // Don't check dictionary elements for unresolved references — avoids false positives when
        // application dictionaries are not yet loaded.
        if (element is AppleScriptHandlerCall) {
            val resolveResult = element.reference?.resolve()
            if (resolveResult == null) {
                for (argument in element.getArguments()) {
                    createInfoAnnotation(holder, argument.getArgumentSelector(), AppleScriptSyntaxHighlighterColors.UNRESOLVED_REFERENCE)
                }
            }
        }
        when (element) {
            is AppleScriptDictionaryCommandName -> createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR)
            is AppleScriptCommandParameterSelector -> createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR)
            is AppleScriptDictionaryClassName, is AppleScriptDictionaryClassIdentifierPlural, is AppleScriptBuiltInClassIdentifier ->
                createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR)
            is AppleScriptDictionaryPropertyName -> createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR)
            is AppleScriptDictionaryConstant -> createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR)
            is AppleScriptApplicationReference -> annotateApplicationReference(holder, element, false)
            is AppleScriptIncompleteExpression -> {
                val expression = PsiTreeUtil.findChildOfType(element, AppleScriptExpression::class.java)
                val appRef = PsiTreeUtil.findChildOfType(expression, AppleScriptApplicationReference::class.java)
                if (appRef != null) annotateApplicationReference(holder, appRef, true)
                holder.newAnnotation(HighlightSeverity.ERROR, "Incomplete expression").range(element).create()
            }
        }

        // Duplicated labels in a labeled-parameters definition/call.
        if (elementType === HANDLER_LABELED_PARAMETERS_DEFINITION ||
            elementType === HANDLER_LABELED_PARAMETERS_CALL_EXPRESSION
        ) {
            val labelNames = ArrayList<String>()
            for (childElement in element.children) {
                if (childElement.node.elementType === HANDLER_PARAMETER_LABEL) {
                    val labelName = childElement.text
                    if (labelNames.contains(labelName)) {
                        var newLabelName: String? = null
                        for (type in HANDLER_PARAMETER_LABELS.types) {
                            if (!labelNames.contains(type.toString().lowercase())) {
                                newLabelName = type.toString().lowercase().replace("_", " ")
                            }
                        }
                        holder.newAnnotation(HighlightSeverity.ERROR, "Duplicated parameter label '$labelName'")
                            .range(childElement)
                            .withFix(RenameParameterLabelQuickFix(childElement as AppleScriptHandlerParameterLabel, newLabelName ?: ""))
                            .create()
                    }
                    labelNames.add(labelName)
                }
            }
        }
    }

    private fun annotateApplicationReference(
        holder: AnnotationHolder,
        appRef: AppleScriptApplicationReference,
        error: Boolean,
    ) {
        val appName = AppleScriptPsiImplUtil.getNameFromApplicationReference(appRef)
        val dictionaryRegistryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        if (StringUtil.isEmptyOrSpaces(appName)) return

        if (!dictionaryRegistryService.isDictionaryInitialized(appName!!)) {
            var warningReason = checkWarningReason(appName, dictionaryRegistryService)
            if (warningReason == null && !dictionaryRegistryService.ensureDictionaryInitialized(appName)) {
                println("Re-checking warning reason for $appName")
                warningReason = checkWarningReason(appName, dictionaryRegistryService)
            }
            if (!StringUtil.isEmpty(warningReason)) {
                if (error) {
                    holder.newAnnotation(HighlightSeverity.ERROR, warningReason!!)
                        .range(appRef)
                        .textAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)
                        .withFix(AddApplicationDictionaryQuickFix(appName))
                        .create()
                } else {
                    holder.newAnnotation(HighlightSeverity.WARNING, warningReason!!)
                        .range(appRef)
                        .withFix(AddApplicationDictionaryQuickFix(appName))
                        .create()
                }
            } else {
                val dictionaryProjectService = appRef.project.getService(AppleScriptProjectDictionaryService::class.java)
                val dictionary = dictionaryProjectService.getDictionary(appName)
                    ?: dictionaryProjectService.createDictionary(appName)
                if (dictionary == null) {
                    if (error) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Unknown app \"$appName\"?")
                            .range(appRef)
                            .textAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)
                            .withFix(AddApplicationDictionaryQuickFix(appName))
                            .create()
                    } else {
                        holder.newAnnotation(HighlightSeverity.WARNING, "Unknown app \"$appName\"?")
                            .range(appRef)
                            .create()
                    }
                }
            }
        } else {
            val dictionaryProjectService = appRef.project.getService(AppleScriptProjectDictionaryService::class.java)
            if (dictionaryProjectService.getDictionary(appName) == null) {
                dictionaryProjectService.createDictionary(appName)
            }
        }
    }

    private fun checkWarningReason(
        appName: String,
        dictionaryRegistryService: AppleScriptSystemDictionaryRegistryService,
    ): String? = when {
        dictionaryRegistryService.isNotScriptable(appName) && dictionaryRegistryService.isXcodeInstalled() ->
            "Application \"$appName\" is not scriptable"
        dictionaryRegistryService.isInUnknownList(appName) -> "Application \"$appName\" not found"
        !dictionaryRegistryService.isXcodeInstalled() -> "Can not create dictionary: Xcode Developer Tools are not installed"
        else -> null
    }

    private companion object {
        private fun createInfoAnnotation(
            holder: AnnotationHolder,
            element: PsiElement?,
            attributeKey: TextAttributesKey?,
        ) {
            if (element != null && attributeKey != null) {
                holder.newAnnotation(HighlightSeverity.INFORMATION, "")
                    .range(element)
                    .textAttributes(attributeKey)
                    .create()
            }
        }
    }
}
