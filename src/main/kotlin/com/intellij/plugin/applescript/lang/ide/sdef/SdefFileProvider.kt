package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.discovery.DeveloperToolsNotInstalledException
import com.intellij.plugin.applescript.lang.dictionary.discovery.DictionaryLoadResult
import com.intellij.plugin.applescript.lang.dictionary.discovery.NotScriptableApplicationException
import com.intellij.plugin.applescript.lang.dictionary.discovery.XcodeDetectionService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.dictionary.xml.LegacyJdomParser
import com.intellij.plugin.applescript.lang.dictionary.xml.LegacyJdomWriter
import com.intellij.plugin.applescript.lang.parser.ParsableScriptHelper
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.extensionSupported
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdom.Document
import org.jdom.Element
import org.jdom.JDOMException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val LOG: Logger = Logger.getInstance("#${SdefFileProvider::class.java.name}")

private const val DICTIONARY_GENERATION_TIMEOUT_SECONDS: Long = 5L

private val DICTIONARY_FILE_EXTENSIONS: Set<String> = setOf("xml", "sdef")

private fun scriptingAdditionFiles(): Sequence<File> =
    ApplicationDictionary.SCRIPTING_ADDITIONS_FOLDERS
        .asSequence()
        .map(::File)
        .filter { directory -> directory.isDirectory }
        .flatMap { directory -> directory.listFiles()?.asSequence() ?: emptySequence() }

private fun scriptingAdditionLibraryName(scriptingAdditionFile: File): String? {
    val fullName = scriptingAdditionFile.name
    val dotIndex = fullName.lastIndexOf('.')
    val endIndex = if (dotIndex > 0) dotIndex else fullName.length
    return fullName.substring(0, endIndex).takeUnless { libraryName -> libraryName.isEmpty() }
}

internal fun stream2file(
    input: InputStream?,
    prefix: String,
    suffix: String,
): File {
    val tempFile = File.createTempFile(prefix, suffix)
    tempFile.deleteOnExit()
    FileOutputStream(tempFile).use { out ->
        requireNotNull(input) { "InputStream for $prefix$suffix is null" }
            .use { inputStream ->
                inputStream.copyTo(out)
            }
    }
    tempFile.deleteOnExit()
    return tempFile
}

private fun findSdefForApplication(applicationIoFile: File): File? {
    val appResources = File(applicationIoFile, "/Contents/Resources")
    val files = appResources.listFiles { _, fileName -> fileName.endsWith("sdef") }
    return if (!files.isNullOrEmpty()) files[0] else null
}

/**
 * Invoke the `sdef` CLI (macOS-only) or `cat` (cross-platform .xml/.sdef passthrough)
 * to generate the dictionary file at [serializePath].
 */
@Throws(NotScriptableApplicationException::class, DeveloperToolsNotInstalledException::class)
private fun doGenerateDictionaryFile(
    applicationName: String,
    serializePath: String,
    cmdName: String,
    appFilePath: String,
): Boolean {
    fun logFailure(cause: Throwable) {
        LOG.error(
            "Failed to create dictionary file for application [$applicationName] Command:$cmdName " +
                "target path: $appFilePath Reason: ${cause.cause}",
            cause,
        )
    }

    var isGenerated = false
    try {
        val shellCommand = arrayOf("/bin/bash", "-c", " $cmdName \"$appFilePath\" > $serializePath")
        LOG.debug("executing command: ${shellCommand.contentToString()}")
        val execStart = System.currentTimeMillis()
        val exitStatus =
            Runtime
                .getRuntime()
                .exec(shellCommand)
                .waitFor(DICTIONARY_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val execEnd = System.currentTimeMillis()
        if (!exitStatus) {
            if (service<XcodeDetectionService>().isXcodeInstalled()) {
                throw NotScriptableApplicationException(
                    applicationName,
                    "Waiting time elapsed for command ${shellCommand.contentToString()}. " +
                        "Seems that application \"$applicationName\" is not scriptable.",
                )
            } else {
                throw DeveloperToolsNotInstalledException()
            }
        }
        LOG.debug("Waiting time elapsed. Execution time: ${execEnd - execStart} ms.")
        isGenerated = true
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        logFailure(e)
    } catch (e: IOException) {
        logFailure(e)
    }
    return isGenerated
}

private fun generateDictionaryFileForApplication(request: DictionaryGenerationRequest): Boolean =
    if (SystemInfo.isMac) {
        val cmdName = if (request.isDictionaryFile) "cat" else "sdef"
        doGenerateDictionaryFile(
            request.applicationName,
            request.serializePath,
            cmdName,
            request.applicationIoFile.path,
        )
    } else {
        request.provider.copyDictionaryFileToCacheDir(
            request.applicationName,
            request.applicationIoFile,
            request.targetFile,
            true,
        )
    }

private data class DictionaryGenerationRequest(
    val provider: SdefFileProvider,
    val applicationName: String,
    val applicationIoFile: File,
    val targetFile: File,
    val serializePath: String,
    val isDictionaryFile: Boolean,
)

private fun recoverDictionaryFileFromBundledSdef(
    provider: SdefFileProvider,
    applicationName: String,
    applicationIoFile: File,
    targetFile: File,
): Boolean {
    // Review MEDIUM 3 / Plan 03-04: the legacy dispatch-thread guard here is REMOVED.
    // copyDictionaryFileToCacheDir routes through `withContext(ioDispatcher)`, so the
    // recovery path is always available regardless of caller thread.
    LOG.warn("Will try to find application scripting definition file...")
    val sdefFile = findSdefForApplication(applicationIoFile)
    return if (sdefFile != null && sdefFile.exists()) {
        provider.copyDictionaryFileToCacheDir(applicationName, sdefFile, targetFile, true)
    } else {
        LOG.warn(
            "Scripting definition was not found for application " +
                applicationIoFile.absolutePath,
        )
        service<SdefPersistenceService>().addNotScriptable(applicationName)
        false
    }
}

private fun registerGeneratedDictionaryInfo(
    applicationName: String,
    targetFile: File,
    applicationIoFile: File,
    appExtension: String,
): DictionaryInfo {
    val applicationBundle =
        applicationIoFile.takeIf {
            ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS.contains(appExtension)
        }
    val dictionaryInfo = DictionaryInfo(applicationName, targetFile, applicationBundle)
    service<SdefPersistenceService>().addDictionaryInfo(dictionaryInfo)
    LOG.debug("Dictionary file generated for application [$applicationName]$targetFile")
    return dictionaryInfo
}

private fun createDictionaryInfoForApplication(
    provider: SdefFileProvider,
    applicationName: String,
    applicationIoFile: File,
): DictionaryInfo? {
    fun finishGeneration(
        targetFile: File,
        fileGenerated: Boolean,
    ) {
        if (!fileGenerated) {
            LOG.warn("Error occurred while generating file.")
            if (targetFile.delete()) LOG.warn("Created file was deleted")
        } else if (ApplicationDiscoveryService.getInstance().removeFromNotFoundList(applicationName)) {
            LOG.debug("Application was removed from ignored list")
        }
    }

    // Kotlin stdlib File.extension: suffix after the last '.' WITHOUT the dot.
    val appExtension = applicationIoFile.extension
    val isDictionaryFile = appExtension in DICTIONARY_FILE_EXTENSIONS
    val serializePath = provider.serializeDictionaryPathForApplication(applicationName)
    val targetFile = File(serializePath)
    val parentDirectoryReady = targetFile.parentFile.exists() || targetFile.parentFile.mkdirs()
    if ((!SystemInfo.isMac && !isDictionaryFile) || !parentDirectoryReady) return null

    LOG.debug("=== Caching Dictionary for application [$applicationName] ===")
    var fileGenerated = false
    try {
        fileGenerated =
            generateDictionaryFileForApplication(
                DictionaryGenerationRequest(
                    provider,
                    applicationName,
                    applicationIoFile,
                    targetFile,
                    serializePath,
                    isDictionaryFile,
                ),
            )
    } catch (e: NotScriptableApplicationException) {
        LOG.warn("Generation failed: ${e.message}. Adding to ignore list")
        service<SdefPersistenceService>().addNotScriptable(e.applicationName)
    } catch (e: DeveloperToolsNotInstalledException) {
        LOG.warn("Generation failed: ${e.message}")
        fileGenerated =
            recoverDictionaryFileFromBundledSdef(
                provider,
                applicationName,
                applicationIoFile,
                targetFile,
            )
    } finally {
        finishGeneration(targetFile, fileGenerated)
    }

    return targetFile
        .takeIf { fileGenerated && it.exists() }
        ?.let {
            // Wave 4 (SERVICE-04): route the registry write through the typed
            // persistence API (which itself forwards to the facade's
            // `addDictionaryInfoInternal` — Pattern A).
            registerGeneratedDictionaryInfo(applicationName, it, applicationIoFile, appExtension)
        }
}

/**
 * Phase 4 SERVICE-04 + SERVICE-09 (plan 04-04, Wave 4): Light Service that owns the
 * dictionary file generation + caching + xi:include resolution + sdef CLI invocation +
 * scripting-additions merging pipeline.
 *
 * Phase 7 D-05: Xcode detection moved out to [XcodeDetectionService] (its own seam) — this
 * provider consults it via `service<XcodeDetectionService>()` on the developer-tools-missing
 * recovery path in [doGenerateDictionaryFile].
 *
 * Returns [DictionaryLoadResult] from [fetch] — call sites get exhaustive `when` and
 * compile-time variant safety (D-05). The sealed type is service-INTERNAL: it does NOT
 * cross any [com.intellij.plugin.applescript.lang.parser.ParsableScriptHelper] boundary,
 * does NOT appear on the facade's public signature, and does NOT touch PSI interfaces.
 * PSI-side sealing is deferred to v1.4 Phase 5 PSI-05.
 *
 * RESEARCH Q2 + Q3 resolutions (recorded in 04-RESEARCH.md):
 *  - **Q2**: Xcode detection used to live here (error-recovery path of
 *    [createDictionaryInfoForApplication]). Phase 7 D-05 closed that v1.6-cleanup marker
 *    by extracting it to [XcodeDetectionService] — the responsibility (probe for a
 *    developer-tools install) is orthogonal to SDEF parsing and now has its own owner.
 *  - **Q3**: [mergeScriptingAdditions] lives here — it generates a synthesised SDEF by
 *    merging per-scripting-addition suites via JDOM. The output is a file (file-generation
 *    concern); ingestion is downstream (SdefIndexService in Wave 5).
 *
 * Cross-service dependencies (verified by `verifyServiceDependencyGraph`):
 *  - `service<SdefPersistenceService>()` — reads the in-memory dictionary registry via
 *    [SdefPersistenceService.readDictionaryInfoSnapshot], writes [DictionaryInfo] entries
 *    on successful generation via [SdefPersistenceService.addDictionaryInfo], marks
 *    applications as not-scriptable on parse failure via
 *    [SdefPersistenceService.addNotScriptable].
 *  - `service<ApplicationDiscoveryService>()` — resolves `applicationName -> bundle
 *    file` in [fetch] via [ApplicationDiscoveryService.findApplicationBundleFile];
 *    removes a name from the not-found memo on successful generation via
 *    [ApplicationDiscoveryService.removeFromNotFoundList].
 *
 * Phase 3 D-02 dispatch-context audit (closed in Wave 4 per 03-CONTEXT.md): each method
 * carries an explicit dispatcher invariant in its KDoc. The pre-Wave-4 facade had a
 * legacy `isDispatchThread` guard at former line 631 (already removed in Phase 3 Review
 * MEDIUM 3 on `copyDictionaryFileToCacheDir`) — no remaining EDT guards needed on the
 * migrated bodies because the `@Synchronized` composite chain plus the EDT guards on
 * the bounded-wait facades (`findStdCommands` / `findApplicationCommands` per Review
 * MEDIUM 1) make EDT entry into this code path impossible.
 *
 * Phase 3 coroutine bridges (former facade lines 459/497/878/900 in the pre-Wave-3
 * file) are no longer needed here: [copyDictionaryFileToCacheDir] is synchronous and
 * callers reach this provider from background dictionary-loading paths.
 *
 * Phase 6 `@Synchronized`: [createAndInitializeInfo] keeps `@Synchronized` — the
 * composite (generate-file → put-info → init-dictionary) is naturally serial per app;
 * `@Synchronized` is clearer than a Mutex here. Other methods rely on `ConcurrentHashMap`
 * (on the facade + upstream services) for thread-safety of their backing state.
 *
 * State migrated from the facade (Wave 4):
 *  - [scriptingAdditions] — set of scripting-addition names successfully ingested.
 *    Session-only, populated by [initializeScriptingAdditions], read by
 *    [mergeScriptingAdditions] and the facade's [ParsableScriptHelper.getScriptingAdditions]
 *    trampoline.
 *
 * Light Service per [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html):
 * no `<applicationService>` entry needed in plugin.xml.
 */
@Service(Service.Level.APP)
class SdefFileProvider
    @JvmOverloads
    constructor(
        @Suppress("unused") private val serviceScope: CoroutineScope,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        // Migrated from facade (Wave 4): set of scripting-addition application names that
        // have been successfully ingested. Populated by [initializeScriptingAdditions],
        // consumed by [mergeScriptingAdditions] and by the facade's
        // [com.intellij.plugin.applescript.lang.parser.ParsableScriptHelper.getScriptingAdditions]
        // trampoline. Backed by `ConcurrentHashMap.newKeySet` per HOTFIX-01 (Phase 1).
        private val scriptingAdditions: MutableSet<String> = ConcurrentHashMap.newKeySet()

        /**
         * Top-level fetch entry — returns [DictionaryLoadResult] for an `applicationName`.
         * Resolves the bundle file via [ApplicationDiscoveryService.findApplicationBundleFile]
         * then delegates to [createAndInitializeInfo] for the full load chain.
         *
         * Dispatcher invariant: `suspend` with explicit `withContext(ioDispatcher)` switch —
         * defence-in-depth even when the caller is already off-EDT.
         *
         * Result variants:
         *  - [DictionaryLoadResult.Empty] — bundle file not found OR createAndInitializeInfo
         *    returned null (unsupported extension, file does not exist, generation failed).
         *  - [DictionaryLoadResult.Loaded] — dictionary file generated/cached and persisted.
         *  - [DictionaryLoadResult.Failed] — recoverable error (sdef CLI missing,
         *    NotScriptableApplicationException, unexpected throwable). Carries the original
         *    cause when available.
         */
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
                    // Pattern B compliance (RECURRING_PITFALLS.md): re-throw structured cancellation.
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

        /**
         * Composite per-app load: generate dictionary file → register [DictionaryInfo] →
         * return. `@Synchronized` serialises file-generation per app to avoid interleaved
         * races on the cache directory (PITFALLS reduction §6).
         *
         * Dispatcher invariant: non-suspend (`@Synchronized` is incompatible with `suspend`).
         * Production call sites are off-EDT by construction (EDT guards on `findStdCommands` /
         * `findApplicationCommands` per Review MEDIUM 1; parser-util via
         * [AppleScriptSystemDictionaryRegistryService.getInitializedInfo] is off-EDT).
         *
         * Body migrated byte-for-byte from the pre-Wave-4 facade
         * `createAndInitializeInfo(File, String)` — same extension check, same exists check,
         * same warning on duplicate, same delegation to [createDictionaryInfoForApplication]
         * + the parse step (deferred to the facade's `initializeDictionaryFromInfo`, which
         * stays on the facade because it touches the parser map cluster — Wave 5 territory).
         */
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
                    createDictionaryInfoForApplication(this, applicationName, applicationIoFile)
                        ?.takeIf { dictionaryInfo ->
                            facade.initializeDictionaryFromInfoInternal(dictionaryInfo)
                        }
                }
            return createdDictionaryInfo
        }

        /**
         * Copy [applicationDictionaryFile] into the IDE's cache directory at [targetFile].
         *
         * Dispatcher invariant: synchronous file I/O; callers reach this provider from
         * background dictionary-loading paths. Plan 03-04 / D-02 (Review MEDIUM 3) —
         * legacy dispatch-thread early-exit
         * guard was already removed in Phase 3; structured-concurrency dispatch supersedes
         * the manual guard. Body migrated byte-for-byte from the pre-Wave-4 facade.
         *
         * Phase 3 Pattern A error handling preserved: `LOG.error("...", e)` (not
         * `e.printStackTrace()`) so the exception chain is captured in IDE log routing.
         */
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
                    // Guava's Files.copy(File, File) defaulted to overwrite; java.nio
                    // does not — pass REPLACE_EXISTING explicitly to preserve semantics
                    // (D-11). The pre-delete above also guards the read-after-write race.
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

        /**
         * Initialise scripting-additions libraries from
         * [ApplicationDictionary.SCRIPTING_ADDITIONS_FOLDERS]. For each `.sdef` file found,
         * tries the registry first (already cached), then [createAndInitializeInfo] (extract
         * + generate), with a final fallback to [initStdTerms] (bundled-resource extraction).
         *
         * Dispatcher invariant: non-suspend; safe on any background thread. Internal calls
         * reach [createAndInitializeInfo] which carries `@Synchronized` per-app serialisation.
         *
         * Body migrated byte-for-byte from the pre-Wave-4 facade `initializeScriptingAdditions`.
         */
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

        /**
         * Bootstrap a standard-library suite ([ApplicationDictionary.COCOA_STANDARD_LIBRARY]
         * or [ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY]) from the bundled
         * plugin resource. Falls back to a tmp-file extraction via [stream2file] when the
         * library is not yet cached.
         *
         * Dispatcher invariant: non-suspend; throws [IOException] on bundle-resource extraction
         * failure (preserved from the pre-Wave-4 facade signature for call sites that catch
         * it explicitly — e.g. the facade's standard-suite initialization path).
         *
         * Body migrated byte-for-byte from the pre-Wave-4 facade `initStdTerms`.
         */
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

                val standardLibraryStream: InputStream? = javaClass.getResourceAsStream(libPathResource)
                val tmpFile = stream2file(standardLibraryStream, stdLibName.replace(" ", "_"), ".sdef")
                if (tmpFile.exists() && tmpFile.isFile) {
                    stdDInfo = createAndInitializeInfo(tmpFile, stdLibName)
                } else {
                    LOG.warn("Can not find standard suite dictionary in the classpath")
                }
            }
            return stdDInfo
        }

        /**
         * Merge every scripting-additions suite into a single synthesised `.sdef` and register
         * it under [ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY].
         *
         * RESEARCH Q3 resolution: this stays in [SdefFileProvider] — it generates a synthesised
         * SDEF file (file-generation concern); ingestion (parser pass to populate index maps) is
         * downstream in SdefIndexService (Wave 5).
         *
         * Dispatcher invariant: non-suspend; safe on any background thread. JDOM I/O is
         * synchronous file-based; the resulting `.sdef` is registered via [createAndInitializeInfo]
         * which itself runs synchronously.
         *
         * XXE hardening: uses the same legacy JDOM parser bridge as
         * [SdefIndexService.parseDictionaryFile].
         *
         * Body migrated byte-for-byte from the pre-Wave-4 facade `mergeScriptingAdditions`.
         */
        fun mergeScriptingAdditions(): DictionaryInfo? {
            fun appendSuites(
                firstRoot: Element,
                secondRoot: Element,
            ) {
                secondRoot.getChildren("suite").forEach { originalSuite ->
                    val suite = originalSuite.clone()
                    suite.detach()
                    firstRoot.addContent(suite)
                }
            }

            return try {
                val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
                val libName = ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
                val dictionaryFiles =
                    scriptingAdditions.mapNotNull { scriptingAddition ->
                        facade
                            .getDictionaryInfoByNameInternal(scriptingAddition)
                            ?.getDictionaryFile()
                            ?.let { dictionaryFile -> File(dictionaryFile.path) }
                    }

                if (dictionaryFiles.isEmpty()) return null

                val mergedFile = File.createTempFile(libName, ".sdef")
                val firstDocument: Document = LegacyJdomParser.build(dictionaryFiles.first())
                val firstRoot: Element = firstDocument.rootElement
                dictionaryFiles.drop(1).forEach { second ->
                    appendSuites(firstRoot, LegacyJdomParser.build(second).rootElement)
                }
                FileOutputStream(mergedFile).use { out ->
                    LegacyJdomWriter.write(firstDocument, out)
                    out.flush()
                }
                createAndInitializeInfo(mergedFile, libName)
            } catch (e: JDOMException) {
                LOG.warn("Can not parse scripting additions file", e)
                null
            } catch (e: IOException) {
                LOG.warn("Can not merge scripting additions", e)
                null
            }
        }

        /**
         * Defensive snapshot of the scripting-additions name set. Returns a [HashSet]
         * (NOT the live backing `ConcurrentHashMap.KeySet`). Backs the facade's
         * [ParsableScriptHelper.getScriptingAdditions] trampoline.
         */
        fun getScriptingAdditions(): HashSet<String> = HashSet(scriptingAdditions)

        /**
         * Resolve the path where the cached `.sdef` for [applicationName] would live (or
         * does live) under `${PathManager.getSystemPath()}/sdef/`. The path-escape
         * (`name.replace(" ", "_")`) preserves the v1.0 cache layout byte-for-byte —
         * existing user caches use the underscored filenames.
         *
         * Dispatcher invariant: non-suspend; pure computation (no I/O).
         *
         * Body migrated byte-for-byte from the pre-Wave-4 facade
         * `serializeDictionaryPathForApplication`.
         */
        fun serializeDictionaryPathForApplication(applicationName: String): String {
            val unescaped = "$GENERATED_DICTIONARIES_SYSTEM_FOLDER/${applicationName}_generated.sdef"
            return unescaped.replace(" ", "_")
        }

        /**
         * Lookup the cached `.sdef` [File] for [applicationName] via the in-memory
         * [DictionaryInfo] registry (owned by the facade through [SdefPersistenceService]).
         * Returns `null` if no entry exists for the name (e.g. discovery sweep has not yet
         * registered the app).
         *
         * Dispatcher invariant: non-suspend; O(1) HashMap lookup via
         * [AppleScriptSystemDictionaryRegistryService.getDictionaryInfoByNameInternal].
         *
         * Body migrated byte-for-byte from the pre-Wave-4 facade `getDictionaryFile(String?)`.
         */
        fun getDictionaryFile(applicationName: String?): File? =
            AppleScriptSystemDictionaryRegistryService
                .getInstance()
                .getDictionaryInfoByNameInternal(applicationName)
                ?.getDictionaryFile()

        /**
         * Reverse lookup: given an application-bundle path (e.g.
         * `/System/Applications/Finder.app`), find the [DictionaryInfo] entry whose
         * `applicationFile.path` matches. Standard libraries (no `applicationFile`) are
         * matched by the well-known `CocoaStandard.sdef` suffix.
         *
         * Dispatcher invariant: non-suspend; iterates the in-memory registry snapshot via
         * [SdefPersistenceService.readDictionaryInfoSnapshot].
         *
         * Body migrated from the pre-Wave-4 facade `getDictionaryInfoByApplicationPath`
         * — iteration shape preserved verbatim (linear scan); reads through the typed
         * persistence API rather than reaching into the facade's private map directly.
         */
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
            /**
             * Cache directory for generated `.sdef` files: `${PathManager.getSystemPath()}/sdef`.
             * Migrated from the pre-Wave-4 facade companion. Path layout preserved byte-for-byte
             * (`<systemPath>/sdef/<App_Name>_generated.sdef` after underscore-escaping).
             */
            private val GENERATED_DICTIONARIES_SYSTEM_FOLDER: String = "${PathManager.getSystemPath()}/sdef"

            @JvmStatic
            fun getInstance(): SdefFileProvider =
                ApplicationManager
                    .getApplication()
                    .getService(SdefFileProvider::class.java)
        }
    }
