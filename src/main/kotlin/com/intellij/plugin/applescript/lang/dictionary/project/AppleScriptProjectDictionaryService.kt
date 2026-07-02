package com.intellij.plugin.applescript.lang.dictionary.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.files.SdefFileProvider
import com.intellij.plugin.applescript.lang.dictionary.files.serializeDictionaryPathForApplication
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.sdef.impl.ApplicationDictionaryImpl
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import org.jetbrains.annotations.TestOnly
import java.io.File

/**
 * Manages dictionaries for the project. Dictionaries created here are cached for the session.
 */
@Service(Service.Level.PROJECT)
class AppleScriptProjectDictionaryService(
    private val project: Project,
) {
    private val dictionaryRegistryService: AppleScriptSystemDictionaryRegistryService =
        AppleScriptSystemDictionaryRegistryService.getInstance()
    private val persistenceService: SdefPersistenceService =
        SdefPersistenceService.getInstance()
    private val discoveryService: ApplicationDiscoveryService =
        ApplicationDiscoveryService.getInstance()
    private val fileProvider: SdefFileProvider =
        SdefFileProvider.getInstance()

    private val dictionaryMap: MutableMap<String, ApplicationDictionary> = HashMap()

    /** Returns the terminology available by default in every script (Scripting Additions). */
    fun getScriptingAdditionsTerminology(): ApplicationDictionary? {
        val name = ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
        return getDictionary(name) ?: createDictionary(name)
    }

    /** Returns the terminology available by default in any Cocoa scripting application (Cocoa Standard). */
    fun getCocoaStandardTerminology(): ApplicationDictionary? {
        val name = ApplicationDictionary.COCOA_STANDARD_LIBRARY
        return getDictionary(name) ?: createDictionary(name)
    }

    /**
     * Creates the dictionary PSI class for the named application. Cached dictionary files and standard
     * application paths are consulted; null if creation failed.
     */
    @Synchronized
    fun createDictionary(applicationName: String): ApplicationDictionary? {
        val dictionary =
            if (isInIgnoreList(applicationName)) {
                null
            } else {
                getDictionary(applicationName) ?: createDictionaryFromInitializedInfo(applicationName)
            }
        return dictionary
    }

    @Synchronized
    fun getOrCreateDictionaryFromCachedSources(applicationName: String): ApplicationDictionary? =
        materializeDictionaryFromCachedSources(applicationName).dictionary

    @Synchronized
    internal fun materializeDictionaryFromCachedSources(
        applicationName: String,
    ): CachedDictionaryMaterializationResult {
        if (isInIgnoreList(applicationName)) return CachedDictionaryMaterializationResult.Ignored

        val standardApplicationBundle = findStandardApplicationBundle(applicationName)
        val cachedDictionary = getDictionary(applicationName)
        val freshCachedDictionary =
            cachedDictionary?.takeUnless { it.needsBundleAwareRefresh(standardApplicationBundle) }
        if (freshCachedDictionary != null) {
            return CachedDictionaryMaterializationResult.Cached(freshCachedDictionary)
        }

        val registeredDictionary =
            createDictionaryFromRegisteredCache(
                applicationName,
                fallbackApplicationBundle = standardApplicationBundle,
            )
        if (registeredDictionary != null) {
            return CachedDictionaryMaterializationResult.Created(
                registeredDictionary,
                CachedDictionaryMaterializationResult.Source.RegisteredCache,
            )
        }

        return when (
            val generatedDictionary =
                createDictionaryFromGeneratedCacheResult(
                    applicationName,
                    applicationBundle = standardApplicationBundle,
                )
        ) {
            is GeneratedDictionaryCacheResult.Loaded ->
                CachedDictionaryMaterializationResult.Created(
                    generatedDictionary.dictionary,
                    CachedDictionaryMaterializationResult.Source.GeneratedCache,
                )

            is GeneratedDictionaryCacheResult.ParseFailed ->
                CachedDictionaryMaterializationResult.ParseFailed(
                    generatedDictionary.generatedDictionaryFile,
                    fallbackDictionary = cachedDictionary,
                )

            is GeneratedDictionaryCacheResult.MaterializationFailed ->
                CachedDictionaryMaterializationResult.MaterializationFailed(
                    generatedDictionary.generatedDictionaryFile,
                    fallbackDictionary = cachedDictionary,
                )

            GeneratedDictionaryCacheResult.Missing ->
                cachedDictionary?.let(CachedDictionaryMaterializationResult::StaleFallback)
                    ?: CachedDictionaryMaterializationResult.Missing
        }
    }

    private fun createDictionaryFromRegisteredCache(
        applicationName: String,
        fallbackApplicationBundle: File? = null,
    ): ApplicationDictionary? =
        persistenceService
            .dictionaryInfoSnapshot
            .firstOrNull { info ->
                info.getApplicationName() == applicationName && info.initialized
            }?.let { info ->
                createDictionaryFromInfo(
                    info.withApplicationBundleFallback(fallbackApplicationBundle),
                    shouldCacheInProject = false,
                )
            }

    private fun createDictionaryFromGeneratedCacheResult(
        applicationName: String,
        applicationBundle: File?,
    ): GeneratedDictionaryCacheResult {
        val generatedDictionaryFile = File(serializeDictionaryPathForApplication(applicationName))
        if (!generatedDictionaryFile.isFile) return GeneratedDictionaryCacheResult.Missing
        if (!SdefIndexService.getInstance().parseDictionaryFile(generatedDictionaryFile, applicationName)) {
            return GeneratedDictionaryCacheResult.ParseFailed(generatedDictionaryFile)
        }

        val info =
            DictionaryInfo(
                applicationName,
                generatedDictionaryFile,
                applicationBundle,
            ).also { dictionaryInfo -> dictionaryInfo.setInitialized(true) }
        return createDictionaryFromInfo(info, shouldCacheInProject = false)
            ?.let(GeneratedDictionaryCacheResult::Loaded)
            ?: GeneratedDictionaryCacheResult.MaterializationFailed(generatedDictionaryFile)
    }

    private fun ApplicationDictionary.needsBundleAwareRefresh(standardApplicationBundle: File?): Boolean =
        applicationBundle == null && standardApplicationBundle != null

    private fun DictionaryInfo.withApplicationBundleFallback(fallbackApplicationBundle: File?): DictionaryInfo {
        if (getApplicationFile() != null || fallbackApplicationBundle == null) return this
        return DictionaryInfo(getApplicationName(), getDictionaryFile(), fallbackApplicationBundle)
            .also { info -> info.setInitialized(initialized) }
    }

    private fun findStandardApplicationBundle(applicationName: String): File? =
        ApplicationDictionary.APP_BUNDLE_DIRECTORIES
            .asSequence()
            .flatMap { applicationsDirectory ->
                ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS
                    .asSequence()
                    .map { extension -> File("$applicationsDirectory/$applicationName.$extension") }
            }.firstOrNull { applicationFile -> applicationFile.exists() }

    private fun createDictionaryFromInitializedInfo(applicationName: String): ApplicationDictionary? {
        val info = dictionaryRegistryService.getInitializedInfo(applicationName)
        if (info == null) {
            LOG.warn("Failed to get initialized dictionary info for $applicationName")
        }
        return info?.let(::createDictionaryFromInfo)
    }

    private fun createDictionaryFromInfo(
        info: DictionaryInfo,
        shouldCacheInProject: Boolean = true,
    ): ApplicationDictionary? {
        val applicationName = info.getApplicationName()
        val dictionary =
            if (info.initialized) {
                val vFile = LocalFileSystem.getInstance().findFileByIoFile(info.getDictionaryFile())
                val xmlFile =
                    vFile
                        ?.takeIf { it.isValid }
                        ?.let { PsiManager.getInstance(project).findFile(it) as? XmlFile }
                if (xmlFile == null) {
                    LOG.warn(
                        "Failed to create dictionary from info for application: " +
                            "$applicationName. Reason: file is null",
                    )
                }
                xmlFile?.let {
                    ApplicationDictionaryImpl(project, it, applicationName, info.getApplicationFile())
                }
            } else {
                logUninitializedDictionaryInfo(applicationName)
                null
            }

        if (shouldCacheInProject) {
            dictionary?.let { dictionaryMap[applicationName] = it }
        }
        return dictionary
    }

    private fun logUninitializedDictionaryInfo(applicationName: String) {
        LOG.error(
            "Attempt to create dictionary for not initialized Dictionary Info for application" +
                applicationName,
        )
    }

    private fun isInIgnoreList(applicationName: String): Boolean =
        when {
            persistenceService.isNotScriptable(applicationName) -> {
                LOG.debug("Application $applicationName is not scriptable. Can not create dictionary for it.")
                true
            }

            discoveryService.isInNotFoundList(applicationName) -> {
                LOG.debug(
                    "WARNING: Application $applicationName was added to unknown list. " +
                        "Can not create dictionary for it.",
                )
                true
            }

            else -> false
        }

    /**
     * Generates the dictionary file for the application, initialises its terms for the parser, and creates
     * the [ApplicationDictionary] PSI class for the project.
     */
    @Synchronized
    fun createDictionaryFromFile(
        applicationName: String,
        applicationFile: VirtualFile,
    ): ApplicationDictionary? {
        val appIoFile = File(applicationFile.path)
        val info = fileProvider.createAndInitializeInfo(appIoFile, applicationName)
        if (info != null) {
            return createDictionaryFromInfo(info)
        }
        LOG.warn("Failed to get initialized dictionary info for $applicationName from $applicationFile")
        return null
    }

    fun getDictionary(applicationName: String): ApplicationDictionary? = dictionaryMap[applicationName]

    fun getDictionaries(): Collection<ApplicationDictionary> = dictionaryMap.values

    @TestOnly
    internal fun clearCachedDictionariesForTests() {
        dictionaryMap.clear()
    }

    @TestOnly
    fun cacheDictionaryForTests(
        applicationName: String,
        dictionary: ApplicationDictionary,
    ) {
        dictionaryMap[applicationName] = dictionary
    }

    companion object {
        private val LOG: Logger = Logger.getInstance("#${AppleScriptProjectDictionaryService::class.java.name}")
    }
}

private sealed interface GeneratedDictionaryCacheResult {
    data class Loaded(
        val dictionary: ApplicationDictionary,
    ) : GeneratedDictionaryCacheResult

    data class ParseFailed(
        val generatedDictionaryFile: File,
    ) : GeneratedDictionaryCacheResult

    data class MaterializationFailed(
        val generatedDictionaryFile: File,
    ) : GeneratedDictionaryCacheResult

    data object Missing : GeneratedDictionaryCacheResult
}
