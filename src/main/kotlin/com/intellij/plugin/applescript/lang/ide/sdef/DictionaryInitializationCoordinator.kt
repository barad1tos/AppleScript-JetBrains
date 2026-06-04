package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.files.SdefFileProvider
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import java.io.File

internal class DictionaryInitializationCoordinator(
    private val dictionaryInfoRegistry: DictionaryInfoRegistry,
    private val notScriptableApplicationRegistry: NotScriptableApplicationRegistry,
    private val applicationDiscovery: () -> ApplicationDiscoveryService,
    private val dictionaryFiles: () -> SdefFileProvider,
    private val areAppDictionariesIndexed: () -> Boolean,
    private val log: Logger,
) {
    fun ensureDictionaryInitialized(anyApplicationName: String): Boolean {
        val canInitializeUnknownApplication =
            !StringUtil.isEmptyOrSpaces(anyApplicationName) &&
                anyApplicationName !in notScriptableApplicationRegistry &&
                !applicationDiscovery().isInNotFoundList(anyApplicationName)
        val initializedUnknownApplication =
            canInitializeUnknownApplication && getInitializedInfo(anyApplicationName) != null

        return ensureKnownApplicationDictionaryInitialized(anyApplicationName) ||
            initializedUnknownApplication
    }

    fun ensureKnownApplicationDictionaryInitialized(knownApplicationName: String): Boolean {
        val canInitialize =
            areAppDictionariesIndexed() &&
                applicationDiscovery().isKnownApplication(knownApplicationName)
        val initializedKnownApplication =
            initializeKnownDictionaryFromCache(knownApplicationName) ||
                getInitializedInfo(knownApplicationName) != null

        return canInitialize && initializedKnownApplication
    }

    fun getInitializedInfo(applicationName: String): DictionaryInfo? =
        if (shouldSkipInitializedInfoLookup(applicationName)) {
            null
        } else {
            findInitializedInfo(applicationName)
        }

    fun initializeDictionaryFromInfo(dictionaryInfo: DictionaryInfo): Boolean {
        val file = File(dictionaryInfo.getDictionaryFile().path)
        val applicationName = dictionaryInfo.getApplicationName()
        if (file.exists() && service<SdefIndexService>().parseDictionaryFile(file, applicationName)) {
            return dictionaryInfo.setInitialized(true)
        }

        log.warn("Initialization failed for application [$applicationName].")
        dictionaryInfoRegistry.removeInMemory(applicationName)
        return false
    }

    private fun initializeKnownDictionaryFromCache(applicationName: String): Boolean {
        val dictionaryInfo = getDictionaryInfo(applicationName) ?: return false
        return dictionaryInfo.initialized || initializeDictionaryFromInfo(dictionaryInfo)
    }

    private fun shouldSkipInitializedInfoLookup(applicationName: String): Boolean =
        StringUtil.isEmptyOrSpaces(applicationName) ||
            applicationName in notScriptableApplicationRegistry ||
            applicationDiscovery().isInNotFoundList(applicationName)

    private fun findInitializedInfo(applicationName: String): DictionaryInfo? =
        findSavedInitializedInfo(applicationName)
            ?: applicationDiscovery().findApplicationBundleFile(applicationName)?.let { applicationFile ->
                dictionaryFiles().createAndInitializeInfo(applicationFile, applicationName)
            }

    private fun findSavedInitializedInfo(applicationName: String): DictionaryInfo? {
        val savedDictionaryInfo = getDictionaryInfo(applicationName)
        return savedDictionaryInfo?.takeIf {
            it.initialized || initializeDictionaryFromInfo(it)
        }
    }

    private fun getDictionaryInfo(applicationName: String?): DictionaryInfo? = dictionaryInfoRegistry[applicationName]
}
