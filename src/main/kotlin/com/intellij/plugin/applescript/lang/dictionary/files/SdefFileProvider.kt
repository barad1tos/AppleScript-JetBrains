package com.intellij.plugin.applescript.lang.dictionary.files

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

private val LOG: Logger = Logger.getInstance("#${SdefFileProvider::class.java.name}")

/**
 * Application-level owner for generated SDEF files and scripting-additions state.
 *
 * The service coordinates cache paths, application dictionary generation, bundled standard
 * suites, and merged scripting-additions dictionaries. Parsing and index ingestion stay on the
 * downstream registry/index services.
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
                    val info =
                        createAndInitializeInfo(applicationFile, applicationName)
                            ?: return@withContext DictionaryLoadResult.Empty
                    DictionaryLoadResult.Loaded(info)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: NotScriptableApplicationException) {
                    service<SdefPersistenceService>().addNotScriptable(applicationName)
                    DictionaryLoadResult.Failed(applicationName, "Application is not scriptable", e)
                } catch (e: DeveloperToolsNotInstalledException) {
                    DictionaryLoadResult.Failed(
                        applicationName,
                        "Developer Tools not installed (sdef CLI unavailable)",
                        e,
                    )
                } catch (e: IllegalStateException) {
                    LOG.error("Failed to fetch dictionary for $applicationName", e)
                    DictionaryLoadResult.Failed(applicationName, "Unexpected error: ${e.message}", e)
                } catch (e: IllegalArgumentException) {
                    LOG.error("Failed to fetch dictionary for $applicationName", e)
                    DictionaryLoadResult.Failed(applicationName, "Unexpected error: ${e.message}", e)
                }
            }

        @Synchronized
        fun createAndInitializeInfo(
            applicationIoFile: File,
            applicationName: String,
        ): DictionaryInfo? {
            val createdDictionaryInfo =
                if (!extensionSupported(applicationIoFile.extension) || !applicationIoFile.exists()) {
                    null
                } else {
                    val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
                    if (facade.getDictionaryInfoByNameInternal(applicationName) != null) {
                        LOG.warn(
                            "Dictionary for application $applicationName was already initialized. " +
                                "Generating new dictionary file any way.",
                        )
                    }
                    val dictionaryInfo =
                        SdefDictionaryFileGenerator.createDictionaryInfoForApplication(
                            this,
                            applicationName,
                            applicationIoFile,
                        )
                    dictionaryInfo?.takeIf { facade.initializeDictionaryFromInfoInternal(it) }
                }
            return createdDictionaryInfo
        }

        fun copyDictionaryFileToCacheDir(
            applicationName: String,
            applicationDictionaryFile: File,
            targetFile: File,
            rewrite: Boolean,
        ): Boolean {
            if (!targetFile.parentFile.exists()) return false

            val needsCopy = !targetFile.exists() || rewrite
            if (needsCopy) {
                try {
                    if (targetFile.exists() && targetFile.delete()) {
                        LOG.debug("Existing target file deleted: $targetFile")
                    }
                    Files.copy(
                        applicationDictionaryFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (e: IOException) {
                    LOG.error(
                        "Failed to move file $applicationDictionaryFile to cache directory: $targetFile",
                        e,
                    )
                }
            } else {
                LOG.debug("Generated file already exists for application $applicationName")
            }

            val fileMoved = targetFile.exists()
            if (fileMoved) {
                LOG.debug("Dictionary file moved to ${targetFile.parent} directory")
            }
            return fileMoved
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

        fun serializeDictionaryPathForApplication(applicationName: String): String {
            val unescaped = "$GENERATED_DICTIONARIES_SYSTEM_FOLDER/${applicationName}_generated.sdef"
            return unescaped.replace(" ", "_")
        }

        fun getDictionaryFile(applicationName: String?): File? =
            AppleScriptSystemDictionaryRegistryService
                .getInstance()
                .getDictionaryInfoByNameInternal(applicationName)
                ?.getDictionaryFile()

        fun getDictionaryInfoByApplicationPath(applicationPath: String): DictionaryInfo? {
            val cachedInfo =
                service<SdefPersistenceService>()
                    .readDictionaryInfoSnapshot()
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
            private val GENERATED_DICTIONARIES_SYSTEM_FOLDER: String = "${PathManager.getSystemPath()}/sdef"

            @JvmStatic
            fun getInstance(): SdefFileProvider =
                ApplicationManager
                    .getApplication()
                    .getService(SdefFileProvider::class.java)
        }
    }
