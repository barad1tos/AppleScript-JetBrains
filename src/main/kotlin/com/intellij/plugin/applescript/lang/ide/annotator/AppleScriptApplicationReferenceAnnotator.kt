package com.intellij.plugin.applescript.lang.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.discovery.XcodeDetectionService
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.ide.intentions.AddApplicationDictionaryQuickFix
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.psi.AppleScriptApplicationReference
import com.intellij.plugin.applescript.psi.impl.getNameFromApplicationReference

internal object AppleScriptApplicationReferenceAnnotator {
    fun annotate(
        holder: AnnotationHolder,
        appRef: AppleScriptApplicationReference,
        error: Boolean,
    ) {
        val appName = getApplicationName(appRef) ?: return

        val annotationState = AppleScriptApplicationReferenceProbe.resolve(appRef, appName)
        AppleScriptApplicationReferenceRenderer.annotate(holder, appRef, appName, annotationState, error)
    }

    private fun getApplicationName(appRef: AppleScriptApplicationReference): String? {
        val appName = getNameFromApplicationReference(appRef)
        return appName?.takeUnless { StringUtil.isEmptyOrSpaces(it) }
    }
}

private sealed interface ApplicationReferenceAnnotationState {
    data object Resolved : ApplicationReferenceAnnotationState

    data class Warning(
        val reason: String,
    ) : ApplicationReferenceAnnotationState

    data object Unknown : ApplicationReferenceAnnotationState
}

private object AppleScriptApplicationReferenceRenderer {
    fun annotate(
        holder: AnnotationHolder,
        appRef: AppleScriptApplicationReference,
        appName: String,
        state: ApplicationReferenceAnnotationState,
        error: Boolean,
    ) {
        when (state) {
            ApplicationReferenceAnnotationState.Resolved -> Unit
            is ApplicationReferenceAnnotationState.Warning ->
                annotateApplicationWarning(holder, appRef, appName, state.reason, error)
            ApplicationReferenceAnnotationState.Unknown ->
                annotateUnknownApplication(holder, appRef, appName, error)
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
}

private object AppleScriptApplicationReferenceProbe {
    fun resolve(
        appRef: AppleScriptApplicationReference,
        appName: String,
    ): ApplicationReferenceAnnotationState {
        val dictionaryRegistryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        val persistenceService = SdefPersistenceService.getInstance()
        val discoveryService = ApplicationDiscoveryService.getInstance()
        val warningReason =
            checkWarningReason(
                appName = appName,
                persistenceService = persistenceService,
                discoveryService = discoveryService,
            )
        val isKnownOrPendingApplication =
            !dictionaryRegistryService.areAppDictionariesIndexed() ||
                persistenceService.isDictionaryInitialized(appName) ||
                discoveryService.isKnownApplication(appName)

        return if (!warningReason.isNullOrEmpty()) {
            ApplicationReferenceAnnotationState.Warning(warningReason)
        } else if (isKnownOrPendingApplication || projectDictionaryExists(appRef, appName)) {
            ApplicationReferenceAnnotationState.Resolved
        } else {
            ApplicationReferenceAnnotationState.Unknown
        }
    }

    private fun projectDictionaryExists(
        appRef: AppleScriptApplicationReference,
        appName: String,
    ): Boolean {
        val dictionaryProjectService = appRef.project.getService(AppleScriptProjectDictionaryService::class.java)
        return dictionaryProjectService.getDictionary(appName) != null
    }

    private fun checkWarningReason(
        appName: String,
        persistenceService: SdefPersistenceService,
        discoveryService: ApplicationDiscoveryService,
    ): String? {
        val isXcodeInstalled = XcodeDetectionService.getInstance().isXcodeInstalled()
        return when {
            persistenceService.isNotScriptable(appName) && isXcodeInstalled ->
                "Application \"$appName\" is not scriptable"
            discoveryService.isInNotFoundList(appName) -> "Application \"$appName\" not found"
            SystemInfo.isMac && !isXcodeInstalled -> MISSING_XCODE_WARNING
            else -> null
        }
    }

    private const val MISSING_XCODE_WARNING =
        "Can not create dictionary: Xcode Developer Tools are not installed"
}
