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
import com.intellij.plugin.applescript.lang.sdef.extensionSupported
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
    fun createDictionary(applicationName: String): ApplicationDictionary? =
        materializeDictionary(applicationName).dictionary

    /**
     * Typed variant of [createDictionary]: ignore policy, project cache, and the on-demand
     * registry path (discovery + generation + PSI construction) reported as one outcome.
     */
    @Synchronized
    internal fun materializeDictionary(applicationName: String): DictionaryMaterializationResult {
        if (isInIgnoreList(applicationName)) return DictionaryMaterializationResult.Ignored
        getDictionary(applicationName)?.let { return DictionaryMaterializationResult.Cached(it) }
        return createDictionaryFromInitializedInfo(applicationName)
    }

    @Synchronized
    fun getOrCreateDictionaryFromCachedSources(applicationName: String): ApplicationDictionary? =
        materializeDictionaryFromCachedSources(applicationName).dictionary

    @Synchronized
    internal fun materializeDictionaryFromCachedSources(applicationName: String): DictionaryMaterializationResult {
        if (isInIgnoreList(applicationName)) return DictionaryMaterializationResult.Ignored

        val standardApplicationBundle = findStandardApplicationBundle(applicationName)
        val cachedDictionary = getDictionary(applicationName)
        val freshCachedDictionary =
            cachedDictionary?.takeUnless { it.needsBundleAwareRefresh(standardApplicationBundle) }
        if (freshCachedDictionary != null) {
            return DictionaryMaterializationResult.Cached(freshCachedDictionary)
        }

        val registeredDictionary =
            createDictionaryFromRegisteredCache(
                applicationName,
                fallbackApplicationBundle = standardApplicationBundle,
            )
        if (registeredDictionary != null) {
            return DictionaryMaterializationResult.Created(
                registeredDictionary,
                DictionaryMaterializationResult.Source.RegisteredCache,
            )
        }

        return createDictionaryFromGeneratedCache(
            applicationName,
            applicationBundle = standardApplicationBundle,
            fallbackDictionary = cachedDictionary,
        )
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

    private fun createDictionaryFromGeneratedCache(
        applicationName: String,
        applicationBundle: File?,
        fallbackDictionary: ApplicationDictionary?,
    ): DictionaryMaterializationResult {
        val generatedDictionaryFile = File(serializeDictionaryPathForApplication(applicationName))
        if (!generatedDictionaryFile.isFile) {
            return fallbackDictionary?.let(DictionaryMaterializationResult::StaleFallback)
                ?: DictionaryMaterializationResult.Missing
        }
        if (!SdefIndexService.getInstance().parseDictionaryFile(generatedDictionaryFile, applicationName)) {
            LOG.warn("Failed to parse generated dictionary cache for $applicationName at $generatedDictionaryFile")
            return DictionaryMaterializationResult.ParseFailed(generatedDictionaryFile, fallbackDictionary)
        }

        val info =
            DictionaryInfo(
                applicationName,
                generatedDictionaryFile,
                applicationBundle,
            ).also { dictionaryInfo -> dictionaryInfo.setInitialized(true) }
        return materializedFromInfo(
            info,
            DictionaryMaterializationResult.Source.GeneratedCache,
            shouldCacheInProject = false,
            fallbackDictionary = fallbackDictionary,
        )
    }

    private fun ApplicationDictionary.needsBundleAwareRefresh(standardApplicationBundle: File?): Boolean =
        applicationBundle == null && standardApplicationBundle != null

    private fun DictionaryInfo.withApplicationBundleFallback(fallbackApplicationBundle: File?): DictionaryInfo {
        if (getApplicationFile() != null || fallbackApplicationBundle == null) return this
        return DictionaryInfo(
            getApplicationName(),
            getDictionaryFile(),
            fallbackApplicationBundle,
        ).also { info -> info.setInitialized(initialized) }
    }

    private fun findStandardApplicationBundle(applicationName: String): File? =
        ApplicationDictionary.APP_BUNDLE_DIRECTORIES
            .asSequence()
            .flatMap { applicationsDirectory ->
                ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS
                    .asSequence()
                    .map { extension -> File("$applicationsDirectory/$applicationName.$extension") }
            }.firstOrNull { applicationFile -> applicationFile.exists() }

    private fun createDictionaryFromInitializedInfo(applicationName: String): DictionaryMaterializationResult {
        val info = dictionaryRegistryService.getInitializedInfo(applicationName)
        if (info == null) {
            LOG.warn("Failed to get initialized dictionary info for $applicationName")
            return DictionaryMaterializationResult.Missing
        }
        return materializedFromInfo(info, DictionaryMaterializationResult.Source.RegistryInfo)
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
                    LOG.warn(describeDictionaryCreationFailure(applicationName, info.getDictionaryFile(), vFile))
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

    /**
     * Classifies why [createDictionaryFromInfo] could not build the dictionary PSI: the null
     * [XmlFile] has several causes, so the log must name the real one instead of "file is null".
     */
    internal fun describeDictionaryCreationFailure(
        applicationName: String,
        dictionaryFile: File,
        virtualFile: VirtualFile?,
    ): String {
        val reason =
            when {
                virtualFile == null -> "file not found in the virtual file system"
                !virtualFile.isValid -> "virtual file is invalid"
                else -> "file did not resolve to an XML PSI"
            }
        return "Failed to create dictionary for $applicationName from $dictionaryFile: $reason"
    }

    private fun logUninitializedDictionaryInfo(applicationName: String) {
        LOG.error(
            "Attempt to create dictionary for not initialized Dictionary Info for application " +
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

            else -> {
                false
            }
        }

    /**
     * Generates the dictionary file for the application, initialises its terms for the parser, and creates
     * the [ApplicationDictionary] PSI class for the project.
     */
    @Synchronized
    fun createDictionaryFromFile(
        applicationName: String,
        applicationFile: VirtualFile,
    ): ApplicationDictionary? = materializeDictionaryFromFile(applicationName, applicationFile).dictionary

    /** Typed variant of [createDictionaryFromFile]: file-provider generation and PSI construction as one outcome. */
    @Synchronized
    internal fun materializeDictionaryFromFile(
        applicationName: String,
        applicationFile: VirtualFile,
    ): DictionaryMaterializationResult {
        val appIoFile = File(applicationFile.path)
        val info = fileProvider.createAndInitializeInfo(appIoFile, applicationName)
        if (info == null) {
            LOG.warn("Failed to get initialized dictionary info for $applicationName from $applicationFile")
            // The provider rejects unsupported/nonexistent inputs up front (nothing to load);
            // any other null means generation or parsing of the loaded file failed. Report the
            // loaded source file — the generated cache path is deleted on generation failure.
            return if (!extensionSupported(appIoFile.extension) || !appIoFile.exists()) {
                DictionaryMaterializationResult.Missing
            } else {
                DictionaryMaterializationResult.MaterializationFailed(appIoFile)
            }
        }
        return materializedFromInfo(info, DictionaryMaterializationResult.Source.LoadedFile)
    }

    private fun materializedFromInfo(
        info: DictionaryInfo,
        source: DictionaryMaterializationResult.Source,
        shouldCacheInProject: Boolean = true,
        fallbackDictionary: ApplicationDictionary? = null,
    ): DictionaryMaterializationResult =
        createDictionaryFromInfo(info, shouldCacheInProject)
            ?.let { dictionary -> DictionaryMaterializationResult.Created(dictionary, source) }
            ?: DictionaryMaterializationResult.MaterializationFailed(info.getDictionaryFile(), fallbackDictionary)

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

    /**
     * Drives [materializedFromInfo] directly so the fallback-carrying `MaterializationFailed` leg can be covered.
     * That leg fires only when PSI construction fails inside the private generated-cache path; the public
     * cached-sources seam cannot stage it without a file that parses in JDOM yet is not detected as XML.
     */
    @TestOnly
    internal fun materializeFromInfoForTests(
        info: DictionaryInfo,
        source: DictionaryMaterializationResult.Source,
        fallbackDictionary: ApplicationDictionary?,
    ): DictionaryMaterializationResult =
        materializedFromInfo(info, source, shouldCacheInProject = false, fallbackDictionary = fallbackDictionary)

    companion object {
        private val LOG: Logger = Logger.getInstance("#${AppleScriptProjectDictionaryService::class.java.name}")
    }
}
