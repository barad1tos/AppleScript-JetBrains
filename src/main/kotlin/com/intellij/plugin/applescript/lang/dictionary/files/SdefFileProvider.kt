package com.intellij.plugin.applescript.lang.dictionary.files

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.discovery.DeveloperToolsNotInstalledException
import com.intellij.plugin.applescript.lang.dictionary.discovery.DictionaryLoadResult
import com.intellij.plugin.applescript.lang.dictionary.discovery.NotScriptableApplicationException
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.extensionSupported
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private val LOG: Logger = Logger.getInstance("#${SdefFileProvider::class.java.name}")

/**
 * Application-level owner for generated SDEF files and scripting-additions state.
 *
 * The service coordinates application dictionary generation, bundled standard suites, and
 * merged scripting-additions dictionaries. Parsing and index ingestion stay on the downstream
 * registry/index services.
 */
@Service(Service.Level.APP)
class SdefFileProvider
    @JvmOverloads
    constructor(
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        private val scriptingAdditions: MutableSet<String> = ConcurrentHashMap.newKeySet()

        suspend fun fetch(applicationName: String): DictionaryLoadResult =
            withContext(ioDispatcher) {
                val discoveryService = ApplicationDiscoveryService.getInstance()
                val applicationFile =
                    discoveryService.findApplicationBundleFile(applicationName)
                        ?: return@withContext DictionaryLoadResult.Empty
                try {
                    loadDictionary(applicationFile, applicationName)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IllegalStateException) {
                    LOG.error("Failed to fetch dictionary for $applicationName", e)
                    DictionaryLoadResult.Failed(applicationName, "Unexpected error: ${e.message}", e)
                } catch (e: IllegalArgumentException) {
                    LOG.error("Failed to fetch dictionary for $applicationName", e)
                    DictionaryLoadResult.Failed(applicationName, "Unexpected error: ${e.message}", e)
                }
            }

        @Synchronized
        internal fun loadDictionary(
            applicationIoFile: File,
            applicationName: String,
            generateDictionaryFile: (String, File) -> File? = SdefDictionaryFileGenerator::generateDictionaryFile,
            recoverDictionaryFile: (String, File) -> File? = { name, file ->
                SdefDictionaryFileGenerator.recoverDictionaryFile(name, file)
            },
        ): DictionaryLoadResult {
            if (!extensionSupported(applicationIoFile.extension) || !applicationIoFile.exists()) {
                return DictionaryLoadResult.Empty
            }
            val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
            if (facade.getDictionaryInfoByNameInternal(applicationName) != null) {
                LOG.warn(
                    "Dictionary for application $applicationName was already initialized. " +
                        "Generating new dictionary file any way.",
                )
            }

            return try {
                val dictionaryFile =
                    generateFile(
                        applicationName,
                        applicationIoFile,
                        generateDictionaryFile,
                        recoverDictionaryFile,
                    ) ?: return DictionaryLoadResult.Failed(
                        applicationName,
                        "Dictionary file generation failed",
                    )
                initializeDictionary(
                    facade,
                    registerDictionary(applicationName, applicationIoFile, dictionaryFile),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotScriptableApplicationException) {
                service<SdefPersistenceService>().addNotScriptable(e.applicationName)
                DictionaryLoadResult.Failed(applicationName, "Application is not scriptable", e)
            } catch (e: DeveloperToolsNotInstalledException) {
                failedDeveloperToolsLoad(applicationName, e)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                LOG.warn("Dictionary generation was interrupted for $applicationName", e)
                DictionaryLoadResult.Failed(applicationName, "Dictionary generation interrupted", e)
            } catch (e: IOException) {
                LOG.warn("Dictionary file loading failed for $applicationName", e)
                DictionaryLoadResult.Failed(applicationName, "Dictionary file I/O error: ${e.message}", e)
            }
        }

        @Synchronized
        fun createAndInitializeInfo(
            applicationIoFile: File,
            applicationName: String,
        ): DictionaryInfo? = (loadDictionary(applicationIoFile, applicationName) as? DictionaryLoadResult.Loaded)?.info

        private fun generateFile(
            applicationName: String,
            applicationFile: File,
            generate: (String, File) -> File?,
            recover: (String, File) -> File?,
        ): File? =
            try {
                generate(applicationName, applicationFile)
            } catch (e: DeveloperToolsNotInstalledException) {
                recover(applicationName, applicationFile) ?: throw e
            }

        private fun registerDictionary(
            applicationName: String,
            applicationFile: File,
            dictionaryFile: File,
        ): DictionaryInfo {
            val applicationBundle =
                applicationFile.takeIf {
                    ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS.contains(applicationFile.extension)
                }
            val info = DictionaryInfo(applicationName, dictionaryFile, applicationBundle)
            if (ApplicationDiscoveryService.getInstance().removeFromNotFoundList(applicationName)) {
                LOG.debug("Application was removed from ignored list")
            }
            service<SdefPersistenceService>().addDictionaryInfo(info)
            LOG.debug("Dictionary file generated for application [$applicationName]$dictionaryFile")
            return info
        }

        private fun initializeDictionary(
            facade: AppleScriptSystemDictionaryRegistryService,
            info: DictionaryInfo,
        ): DictionaryLoadResult =
            if (facade.initializeDictionaryFromInfoInternal(info)) {
                DictionaryLoadResult.Loaded(info)
            } else {
                DictionaryLoadResult.Failed(info.getApplicationName(), "Dictionary initialization failed")
            }

        private fun failedDeveloperToolsLoad(
            applicationName: String,
            cause: DeveloperToolsNotInstalledException,
        ): DictionaryLoadResult.Failed {
            service<SdefPersistenceService>().addNotScriptable(applicationName)
            return DictionaryLoadResult.Failed(
                applicationName,
                "Developer Tools not installed (sdef CLI unavailable)",
                cause,
            )
        }

        fun initializeScriptingAdditions() {
            val facade = AppleScriptSystemDictionaryRegistryService.getInstance()

            fun initializeBundledScriptingAddition(stdLib: File): DictionaryInfo? {
                LOG.warn(
                    "Can not initialize scripting addition library from file: $stdLib. Will copy bundled lib.",
                )
                return try {
                    initStdTerms(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
                } catch (e: IOException) {
                    LOG.warn("Can not initialize scripting addition library from bundle", e)
                    null
                }
            }

            scriptingAdditionFiles()
                .mapNotNull { stdLib ->
                    scriptingAdditionLibraryName(stdLib)?.let { libraryName -> libraryName to stdLib }
                }.forEach { (libraryName, stdLib) ->
                    val dictionaryInfo =
                        facade
                            .getDictionaryInfoByNameInternal(libraryName)
                            ?.also { facade.initializeDictionaryFromInfoInternal(it) }
                            ?: stdLib
                                .takeIf { it.exists() }
                                ?.let { createAndInitializeInfo(it, libraryName) }
                            ?: initializeBundledScriptingAddition(stdLib)

                    if (dictionaryInfo != null) {
                        scriptingAdditions.add(dictionaryInfo.getApplicationName())
                    }
                }
        }

        @Throws(IOException::class)
        fun initStdTerms(stdLibName: String): DictionaryInfo? {
            val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
            var stdDInfo = facade.getDictionaryInfoByNameInternal(stdLibName)
            if (stdDInfo != null) {
                facade.initializeDictionaryFromInfoInternal(stdDInfo)
            } else {
                val libPathResource: String =
                    when (stdLibName) {
                        ApplicationDictionary.COCOA_STANDARD_LIBRARY -> ApplicationDictionary.COCOA_STANDARD_FILE
                        ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY ->
                            ApplicationDictionary.STANDARD_ADDITIONS_FILE
                        else -> null
                    } ?: return null

                val standardLibraryStream = javaClass.getResourceAsStream(libPathResource)
                val tmpFile = stream2file(standardLibraryStream, stdLibName.replace(" ", "_"), ".sdef")
                if (tmpFile.exists() && tmpFile.isFile) {
                    stdDInfo = createAndInitializeInfo(tmpFile, stdLibName)
                } else {
                    LOG.warn("Can not find standard suite dictionary in the classpath")
                }
            }
            return stdDInfo
        }

        fun mergeScriptingAdditions(): DictionaryInfo? =
            ScriptingAdditionsMerger.mergeAndInitialize(
                scriptingAdditions,
                ::createAndInitializeInfo,
            )

        fun getScriptingAdditions(): HashSet<String> = HashSet(scriptingAdditions)

        fun getDictionaryFile(applicationName: String?): File? =
            AppleScriptSystemDictionaryRegistryService
                .getInstance()
                .getDictionaryInfoByNameInternal(applicationName)
                ?.getDictionaryFile()

        fun getDictionaryInfoByApplicationPath(applicationPath: String): DictionaryInfo? {
            val cachedInfo =
                service<SdefPersistenceService>()
                    .dictionaryInfoSnapshot
                    .firstOrNull { dInfo ->
                        dInfo.getApplicationFile()?.path == applicationPath
                    }
            val standardInfo =
                if (applicationPath.endsWith("CocoaStandard.sdef")) {
                    AppleScriptSystemDictionaryRegistryService
                        .getInstance()
                        .getDictionaryInfoByNameInternal(ApplicationDictionary.COCOA_STANDARD_LIBRARY)
                } else {
                    null
                }
            return cachedInfo ?: standardInfo
        }

        companion object {
            @JvmStatic
            fun getInstance(): SdefFileProvider =
                ApplicationManager
                    .getApplication()
                    .getService(SdefFileProvider::class.java)
        }
    }
