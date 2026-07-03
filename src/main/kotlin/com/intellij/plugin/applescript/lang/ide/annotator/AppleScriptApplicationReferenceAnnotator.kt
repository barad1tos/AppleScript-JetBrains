package com.intellij.plugin.applescript.lang.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.intentions.AddApplicationDictionaryQuickFix
import com.intellij.plugin.applescript.psi.AppleScriptApplicationReference
import com.intellij.plugin.applescript.psi.impl.getNameFromApplicationReference

internal object AppleScriptApplicationReferenceAnnotator {
    fun annotate(
        holder: AnnotationHolder,
        appRef: AppleScriptApplicationReference,
        error: Boolean,
    ) {
        val appName = getApplicationName(appRef) ?: return

        val diagnosis = ApplicationReferenceDiagnoser.diagnose(appRef.project, appName)
        AppleScriptApplicationReferenceRenderer.annotate(holder, appRef, appName, diagnosis, error)
    }

    private fun getApplicationName(appRef: AppleScriptApplicationReference): String? {
        val appName = getNameFromApplicationReference(appRef)
        return appName?.takeUnless { StringUtil.isEmptyOrSpaces(it) }
    }
}

private object AppleScriptApplicationReferenceRenderer {
    fun annotate(
        holder: AnnotationHolder,
        appRef: AppleScriptApplicationReference,
        appName: String,
        diagnosis: ApplicationReferenceDiagnosis,
        error: Boolean,
    ) {
        when (diagnosis) {
            ApplicationReferenceDiagnosis.Ready -> {
                Unit
            }

            ApplicationReferenceDiagnosis.NotScriptable -> {
                annotateApplicationWarning(
                    holder,
                    appRef,
                    appName,
                    "Application \"$appName\" is not scriptable",
                    error,
                )
            }

            ApplicationReferenceDiagnosis.NotFound -> {
                annotateApplicationWarning(holder, appRef, appName, "Application \"$appName\" not found", error)
            }

            ApplicationReferenceDiagnosis.MissingXcode -> {
                annotateApplicationWarning(holder, appRef, appName, MISSING_XCODE_WARNING, error)
            }

            ApplicationReferenceDiagnosis.Unknown -> {
                annotateUnknownApplication(holder, appRef, appName, error)
            }
        }
    }

    private fun annotateApplicationWarning(
        holder: AnnotationHolder,
        appRef: AppleScriptApplicationReference,
        appName: String,
        warningReason: String,
        error: Boolean,
    ) {
        if (error) {
            holder
                .newAnnotation(HighlightSeverity.ERROR, warningReason)
                .range(appRef)
                .textAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)
                .withFix(AddApplicationDictionaryQuickFix(appName))
                .create()
            return
        }

        holder
            .newAnnotation(HighlightSeverity.WEAK_WARNING, warningReason)
            .range(appRef)
            .withFix(AddApplicationDictionaryQuickFix(appName))
            .create()
    }

    private fun annotateUnknownApplication(
        holder: AnnotationHolder,
        appRef: AppleScriptApplicationReference,
        appName: String,
        error: Boolean,
    ) {
        if (error) {
            holder
                .newAnnotation(HighlightSeverity.ERROR, "Unknown app \"$appName\"?")
                .range(appRef)
                .textAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)
                .withFix(AddApplicationDictionaryQuickFix(appName))
                .create()
            return
        }

        holder
            .newAnnotation(HighlightSeverity.WEAK_WARNING, "Unknown app \"$appName\"?")
            .range(appRef)
            .create()
    }

    private const val MISSING_XCODE_WARNING =
        "Can not create dictionary: Xcode Developer Tools are not installed"
}
