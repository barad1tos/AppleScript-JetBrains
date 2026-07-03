package com.intellij.plugin.applescript.lang.ide.annotator

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.discovery.XcodeDetectionService
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService

/**
 * Diagnosis of an `application "Name"` reference against the host state: persisted scriptability,
 * discovery results, Xcode tooling, registry readiness, and the project dictionary cache.
 *
 * The annotator renders these states; check order and severity policy live here so severity
 * tests can assert the diagnosis directly instead of driving a full highlighting pass.
 */
internal sealed interface ApplicationReferenceDiagnosis {
    /** The reference resolves (known, pending indexing, or already project-cached). */
    data object Ready : ApplicationReferenceDiagnosis

    data object NotScriptable : ApplicationReferenceDiagnosis

    data object NotFound : ApplicationReferenceDiagnosis

    data object MissingXcode : ApplicationReferenceDiagnosis

    data object Unknown : ApplicationReferenceDiagnosis
}

internal object ApplicationReferenceDiagnoser {
    fun diagnose(
        project: Project,
        applicationName: String,
    ): ApplicationReferenceDiagnosis {
        val persistenceService = SdefPersistenceService.getInstance()
        val discoveryService = ApplicationDiscoveryService.getInstance()
        val isXcodeInstalled = XcodeDetectionService.getInstance().isXcodeInstalled()

        return when {
            persistenceService.isNotScriptable(applicationName) && isXcodeInstalled -> {
                ApplicationReferenceDiagnosis.NotScriptable
            }

            discoveryService.isInNotFoundList(applicationName) -> {
                ApplicationReferenceDiagnosis.NotFound
            }

            SystemInfo.isMac && !isXcodeInstalled -> {
                ApplicationReferenceDiagnosis.MissingXcode
            }

            isKnownOrPendingApplication(applicationName, persistenceService, discoveryService) ||
                projectDictionaryExists(project, applicationName) -> {
                ApplicationReferenceDiagnosis.Ready
            }

            else -> {
                ApplicationReferenceDiagnosis.Unknown
            }
        }
    }

    private fun isKnownOrPendingApplication(
        applicationName: String,
        persistenceService: SdefPersistenceService,
        discoveryService: ApplicationDiscoveryService,
    ): Boolean =
        !AppleScriptSystemDictionaryRegistryService.getInstance().areAppDictionariesIndexed() ||
            persistenceService.isDictionaryInitialized(applicationName) ||
            discoveryService.isKnownApplication(applicationName)

    private fun projectDictionaryExists(
        project: Project,
        applicationName: String,
    ): Boolean =
        project
            .getService(AppleScriptProjectDictionaryService::class.java)
            .getDictionary(applicationName) != null
}
