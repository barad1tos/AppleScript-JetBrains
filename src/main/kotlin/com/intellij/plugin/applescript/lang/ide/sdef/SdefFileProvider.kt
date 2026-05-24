package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.sdef.results.DictionaryLoadResult
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
import org.jdom.input.SAXBuilder
import org.jdom.output.XMLOutputter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * Phase 4 SERVICE-04 + SERVICE-09 (plan 04-04, Wave 4): Light Service that owns the
 * dictionary file generation + caching + xi:include resolution + sdef CLI invocation +
 * Xcode detection + scripting-additions merging pipeline.
 *
 * Returns [DictionaryLoadResult] from [fetch] — call sites get exhaustive `when` and
 * compile-time variant safety (D-05). The sealed type is service-INTERNAL: it does NOT
 * cross any [com.intellij.plugin.applescript.lang.parser.ParsableScriptHelper] boundary,
 * does NOT appear on the facade's public signature, and does NOT touch PSI interfaces.
 * PSI-side sealing is deferred to v1.4 Phase 5 PSI-05.
 *
 * RESEARCH Q2 + Q3 resolutions (recorded in 04-RESEARCH.md):
 *  - **Q2**: [isXcodeInstalled] lives here because the file-generation orchestrator
 *    [createDictionaryInfoForApplication] needs it on the error-recovery path. Marked
 *    `TODO(v1.6 CLEANUP)` — the responsibility is orthogonal to SDEF parsing and could
 *    move to a dedicated XcodeDetectionService or to a plugin-startup once-per-IDE check.
 *  - **Q3**: [mergeScriptingAdditions] lives here — it generates a synthesised SDEF by
 *    merging per-scripting-addition suites via JDOM. The output is a file (file-generation
 *    concern); ingestion is downstream (SdefIndexService in Wave 5).
 *
 * Cross-service dependencies (verified by `verifyServiceDependencyGraph`):
 *  - `service<SdefPersistenceService>()` — reads the in-memory dictionary registry via
 *    [SdefPersistenceService.getDictionaryInfo], writes [DictionaryInfo] entries on
 *    successful generation via [SdefPersistenceService.addDictionaryInfo], marks
 *    applications as not-scriptable on parse failure via
 *    [SdefPersistenceService.addNotScriptable].
 *  - `service<ApplicationDiscoveryService>()` — resolves `applicationName -> bundle
 *    file` in [fetch] via [ApplicationDiscoveryService.findApplicationBundleFile];
 *    removes a name from the not-found memo on successful generation via
 *    [ApplicationDiscoveryService.removeFromNotFoundList].
 *
 * Phase 3 D-02 dispatch-context audit (closed in Wave 4 per 03-CONTEXT.md): each method
 * carries an explicit dispatcher invariant in its KDoc. The pre-Wave-4 facade had a
 * legacy `isDispatchThread` guard at former line 631 (already removed in Phase 3 Codex
 * MEDIUM 3 on `copyDictionaryFileToCacheDir`) — no remaining EDT guards needed on the
 * migrated bodies because the `@Synchronized` composite chain plus the EDT guards on
 * the bounded-wait facades (`findStdCommands` / `findApplicationCommands` per Codex
 * MEDIUM 1) make EDT entry into this code path impossible.
 *
 * Phase 3 `runBlockingCancellable` bridges (former facade lines 459/497/878/900 in the
 * pre-Wave-3 file) are preserved verbatim where they appear in migrated bodies:
 * [createDictionaryInfoForApplication] uses them to bridge from the non-suspend
 * `@Synchronized` chain into [copyDictionaryFileToCacheDir] (a `suspend` function).
 * `runBlockingCancellable` is NOT a naked-blocking bridge (different word; `verifyNoRunBlocking`
 * regex `\brunBlocking\b` does not match) — it is the Platform-blessed bridge for
 * background-thread blocking-on-suspend.
 *
 * Phase 6 `@Synchronized`: [createAndInitializeInfo] keeps `@Synchronized` — the
 * composite (generate-file → put-info → init-dictionary) is naturally serial per app;
 * `@Synchronized` is clearer than a Mutex here. Other methods rely on `ConcurrentHashMap`
 * (on the facade + upstream services) for thread-safety of their backing state.
 *
 * State migrated from the facade (Wave 4):
 *  - [xCodeApplicationFile] — lazy-cached Xcode bundle file (Wave 4).
 *  - [scriptingAdditions] — set of scripting-addition names successfully ingested.
 *    Session-only, populated by [initializeScriptingAdditions], read by
 *    [mergeScriptingAdditions] and the facade's [ParsableScriptHelper.getScriptingAdditions]
 *    trampoline.
 *
 * Light Service per [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html):
 * no `<applicationService>` entry needed in plugin.xml.
 */
@Service(Service.Level.APP)
class SdefFileProvider @JvmOverloads constructor(
    @Suppress("unused") private val serviceScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    // Migrated from facade (Wave 4): lazy-cached Xcode bundle file. Null means "not yet
    // detected"; a non-null File with name "null" means "previously detected as absent"
    // (sentinel preserved byte-for-byte from the pre-Wave-4 facade body — do NOT change
    // the sentinel encoding without re-running the AppleScriptColorAnnotator path on a
    // dev machine without Xcode installed).
    private var xCodeApplicationFile: File? = null

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
    suspend fun fetch(applicationName: String): DictionaryLoadResult = withContext(ioDispatcher) {
        val discoveryService = service<ApplicationDiscoveryService>()
        val applicationFile = discoveryService.findApplicationBundleFile(applicationName)
            ?: return@withContext DictionaryLoadResult.Empty
        try {
            val info = createAndInitializeInfo(applicationFile, applicationName)
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
        } catch (t: Throwable) {
            LOG.error("Failed to fetch dictionary for $applicationName", t)
            DictionaryLoadResult.Failed(applicationName, "Unexpected error: ${t.message}", t)
        }
    }

    /**
     * Composite per-app load: generate dictionary file → register [DictionaryInfo] →
     * return. `@Synchronized` serialises file-generation per app to avoid interleaved
     * races on the cache directory (PITFALLS reduction §6).
     *
     * Dispatcher invariant: non-suspend (`@Synchronized` is incompatible with `suspend`).
     * Reaches into `suspend` helpers via [runBlockingCancellable] bridges — production
     * call sites are off-EDT by construction (EDT guards on `findStdCommands` /
     * `findApplicationCommands` per Codex MEDIUM 1; parser-util via
     * [AppleScriptSystemDictionaryRegistryService.getInitializedInfo] is off-EDT).
     *
     * Body migrated byte-for-byte from the pre-Wave-4 facade
     * `createAndInitializeInfo(File, String)` — same extension check, same exists check,
     * same warning on duplicate, same delegation to [createDictionaryInfoForApplication]
     * + the parse step (deferred to the facade's `initializeDictionaryFromInfo`, which
     * stays on the facade because it touches the parser map cluster — Wave 5 territory).
     */
    @Synchronized
    fun createAndInitializeInfo(applicationIoFile: File, applicationName: String): DictionaryInfo? {
        val appExtension: String = applicationIoFile.extension
        if (!extensionSupported(appExtension)) return null
        if (!applicationIoFile.exists()) return null
        val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
        if (facade.getDictionaryInfoByNameInternal(applicationName) != null) {
            LOG.warn(
                "Dictionary for application $applicationName was already initialized. " +
                    "Generating new dictionary file any way.",
            )
        }
        val createdDictionaryInfo = createDictionaryInfoForApplication(applicationName, applicationIoFile)
        if (createdDictionaryInfo != null) {
            if (facade.initializeDictionaryFromInfoInternal(createdDictionaryInfo)) return createdDictionaryInfo
        }
        return null
    }

    /**
     * Copy [applicationDictionaryFile] into the IDE's cache directory at [targetFile].
     *
     * Dispatcher invariant: `suspend` with explicit `withContext(ioDispatcher)` around the
     * file I/O. Plan 03-04 / D-02 (Codex MEDIUM 3) — legacy dispatch-thread early-exit
     * guard was already removed in Phase 3; structured-concurrency dispatch supersedes
     * the manual guard. Body migrated byte-for-byte from the pre-Wave-4 facade.
     *
     * Phase 3 Pattern A error handling preserved: `LOG.error("...", e)` (not
     * `e.printStackTrace()`) so the exception chain is captured in IDE log routing.
     */
    suspend fun copyDictionaryFileToCacheDir(
        applicationName: String,
        applicationDictionaryFile: File,
        targetFile: File,
        rewrite: Boolean,
    ): Boolean {
        if (!targetFile.parentFile.exists()) return false

        val needsCopy = !targetFile.exists() || rewrite
        if (needsCopy) {
            // File I/O on ioDispatcher — Pattern C: NEVER the Main dispatcher.
            withContext(ioDispatcher) {
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
                    LOG.error("Failed to move file $applicationDictionaryFile to cache directory: $targetFile", e)
                }
            }
        } else {
            LOG.debug("Generated file already exists for application $applicationName")
        }
        if (targetFile.exists()) {
            LOG.debug("Dictionary file moved to ${targetFile.parent} directory")
            return true
        }
        return false
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
        for (stdLibFolder in ApplicationDictionary.SCRIPTING_ADDITIONS_FOLDERS) {
            val dir = File(stdLibFolder)
            if (!dir.isDirectory) continue
            val stdLibs = dir.listFiles() ?: continue
            if (stdLibs.isEmpty()) continue
            for (stdLib in stdLibs) {
                var libraryName = stdLib.name
                val last = libraryName.lastIndexOf(".")
                libraryName = libraryName.substring(0, if (last > 0) last else libraryName.length - 1)
                if (StringUtil.isEmpty(libraryName)) continue
                var dInfo = facade.getDictionaryInfoByNameInternal(libraryName)
                if (dInfo != null) {
                    facade.initializeDictionaryFromInfoInternal(dInfo)
                } else if (stdLib.exists()) {
                    dInfo = createAndInitializeInfo(stdLib, libraryName)
                }
                if (dInfo != null) {
                    scriptingAdditions.add(dInfo.getApplicationName())
                } else {
                    LOG.warn(
                        "Can not initialize scripting addition library from file: $stdLib. Will copy bundled lib.",
                    )
                    try {
                        dInfo = initStdTerms(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
                        if (dInfo != null) scriptingAdditions.add(dInfo.getApplicationName())
                    } catch (e: IOException) {
                        LOG.warn("Can not initialize scripting addition library from bundle: ${e.message}")
                    }
                }
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
     * it explicitly — e.g. [initStandardSuite]).
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
            val libPathResource: String? = when (stdLibName) {
                ApplicationDictionary.COCOA_STANDARD_LIBRARY -> ApplicationDictionary.COCOA_STANDARD_FILE
                ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY -> ApplicationDictionary.STANDARD_ADDITIONS_FILE
                else -> null
            } ?: return null

            val isStd: InputStream? = javaClass.getResourceAsStream(libPathResource)
            val tmpFile = stream2file(isStd, stdLibName.replace(" ", "_"), ".sdef")
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
     * XXE hardening: uses [SdefIndexService.newSecureSaxBuilderForFileProvider] — Wave 5
     * co-located the factory with its primary consumer ([SdefIndexService.parseDictionaryFile])
     * and exposes a typed cross-service accessor for this caller.
     *
     * Body migrated byte-for-byte from the pre-Wave-4 facade `mergeScriptingAdditions`.
     */
    fun mergeScriptingAdditions(): DictionaryInfo? {
        val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
        try {
            val dictionaryFiles = ArrayList<File>()
            val libName = ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
            val mergedFile = File.createTempFile(libName, ".sdef")
            for (scriptingAddition in scriptingAdditions) {
                val di = facade.getDictionaryInfoByNameInternal(scriptingAddition) ?: continue
                dictionaryFiles.add(File(di.getDictionaryFile().path))
            }
            val iterator = dictionaryFiles.iterator()
            if (!iterator.hasNext()) return null
            val first = iterator.next()
            val builder = SdefIndexService.newSecureSaxBuilderForFileProvider()
            val firstDocument: Document = builder.build(first)
            val firstRoot: Element = firstDocument.rootElement
            while (iterator.hasNext()) {
                val second = iterator.next()
                val secondDocument: Document = builder.build(second)
                val secondRoot: Element = secondDocument.rootElement
                val suiteElements: List<Element> = secondRoot.getChildren("suite")
                for (originalSuite in suiteElements) {
                    val suite = originalSuite.clone()
                    suite.detach()
                    firstRoot.addContent(suite)
                }
            }
            val outputter = XMLOutputter()
            FileOutputStream(mergedFile).use { out ->
                outputter.output(firstDocument, out)
                out.flush()
            }
            return createAndInitializeInfo(mergedFile, libName)
        } catch (e: JDOMException) {
            LOG.warn("Can not parse scripting additions file: ${e.message}")
            e.printStackTrace()
        } catch (e: IOException) {
            LOG.warn("Can not merge scripting additions: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            LOG.warn("Can not merge scripting additions: ${e.message}")
            e.printStackTrace()
        }
        return null
    }

    /**
     * Defensive snapshot of the scripting-additions name set. Returns a [HashSet]
     * (NOT the live backing `ConcurrentHashMap.KeySet`). Backs the facade's
     * [ParsableScriptHelper.getScriptingAdditions] trampoline.
     */
    fun getScriptingAdditions(): HashSet<String> = HashSet(scriptingAdditions)

    /**
     * RESEARCH Q2 resolution: stays in [SdefFileProvider] because the file-generation
     * orchestrator [createDictionaryInfoForApplication] needs it on the
     * [DeveloperToolsNotInstalledException] recovery path.
     *
     * TODO(v1.6 CLEANUP): isXcodeInstalled is orthogonal to SDEF parsing — its responsibility
     * (detect a sibling JetBrains-incompatible developer-tool install) could move to a
     * dedicated XcodeDetectionService or to a plugin-startup once-per-IDE check. Tracked
     * under v1.6 CLEANUP-* requirements.
     *
     * Dispatcher invariant: non-suspend; safe on any thread. Reads [xCodeApplicationFile]
     * (`@Volatile` semantics not strictly needed because the only writes are inside this
     * method and the field is only consulted by [createDictionaryInfoForApplication]; the
     * pre-Wave-4 facade did not annotate `@Volatile` either, so Wave 4 preserves byte-for-byte).
     *
     * Body migrated byte-for-byte from the pre-Wave-4 facade `isXcodeInstalled`.
     */
    fun isXcodeInstalled(): Boolean {
        if (!SystemInfo.isMac) return false
        val cached = xCodeApplicationFile
        if (cached != null && cached.exists()) return true
        // "null" name means that Xcode was not found previously.
        if (cached != null && "null" == cached.name) return false

        val xCodeApp = File("/Applications/Xcode.app")
        if (xCodeApp.exists()) {
            xCodeApplicationFile = xCodeApp
            return true
        }
        try {
            val engineManager = ScriptEngineManager()
            val engine = engineManager.getEngineByName("AppleScriptEngine")
            if (engine != null) {
                val script = "try\n" +
                    "tell application \"Finder\" to return POSIX path of (get application file id \"com.apple.dt.Xcode\" " +
                    "as alias)\n" + "on error\n" + "  return \"null\"\n" + "end try"
                val scriptResult = engine.eval(script)
                if (scriptResult != null) {
                    val result = File(scriptResult.toString())
                    xCodeApplicationFile = result
                    return result.exists()
                }
            }
        } catch (e: ScriptException) {
            LOG.error("Error evaluating applescript: ${e.message}")
        }
        xCodeApplicationFile = File("null")
        return false
    }

    /**
     * Find a `.sdef` file under [applicationIoFile]`/Contents/Resources/`. Used as the
     * recovery path in [createDictionaryInfoForApplication] when the sdef CLI fails
     * because Developer Tools are not installed.
     *
     * Dispatcher invariant: non-suspend; safe on any background thread (NOT EDT —
     * `File.listFiles` is blocking I/O).
     *
     * Body migrated byte-for-byte from the pre-Wave-4 facade `findSdefForApplication`.
     */
    fun findSdefForApplication(applicationIoFile: File): File? {
        val appResources = File(applicationIoFile, "/Contents/Resources")
        val files = appResources.listFiles { _, s -> s.endsWith("sdef") }
        if (files != null && files.isNotEmpty()) {
            return files[0]
        }
        return null
    }

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
        AppleScriptSystemDictionaryRegistryService.getInstance()
            .getDictionaryInfoByNameInternal(applicationName)?.getDictionaryFile()

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
        val infos = service<SdefPersistenceService>().readDictionaryInfoSnapshot()
        for (dInfo in infos) {
            val appFile = dInfo.getApplicationFile()
            if (appFile != null && appFile.path == applicationPath) return dInfo
        }
        // Standard libraries do not have an application path.
        if (applicationPath.endsWith("CocoaStandard.sdef")) {
            return AppleScriptSystemDictionaryRegistryService.getInstance()
                .getDictionaryInfoByNameInternal(ApplicationDictionary.COCOA_STANDARD_LIBRARY)
        }
        return null
    }

    /**
     * Generates the dictionary file and creates a [DictionaryInfo] for the application.
     * Either invokes the `sdef` CLI (macOS) or copies an existing `.xml`/`.sdef` directly
     * (cross-platform). On [DeveloperToolsNotInstalledException], falls back to searching
     * `<app>/Contents/Resources` for a bundled `.sdef`.
     *
     * Dispatcher invariant: non-suspend; bridges to `suspend` helpers
     * ([copyDictionaryFileToCacheDir]) via [runBlockingCancellable] (Phase 3 Codex MEDIUM 3
     * — the bridge is the documented pattern for non-EDT background threads, NOT a naked
     * blocking call; the `verifyNoRunBlocking` regex on the matching unqualified word
     * does not match the qualified `runBlockingCancellable` identifier).
     *
     * Cross-service writes:
     *  - On [NotScriptableApplicationException]: marks the app via
     *    [SdefPersistenceService.addNotScriptable].
     *  - On [DeveloperToolsNotInstalledException] with no bundled `.sdef`: same.
     *  - On successful generation: registers the [DictionaryInfo] via
     *    [SdefPersistenceService.addDictionaryInfo] (which itself idempotently adds the
     *    app to the discovered set via [ApplicationDiscoveryService] inside the facade's
     *    `addDictionaryInfoInternal`).
     *  - Always (success path): clears the name from the discovery service's not-found
     *    memo via [ApplicationDiscoveryService.removeFromNotFoundList].
     *
     * Body migrated byte-for-byte from the pre-Wave-4 facade
     * `createDictionaryInfoForApplication`, with cross-service writes routed through the
     * typed APIs (was direct facade-private set mutation pre-Wave-4).
     */
    private fun createDictionaryInfoForApplication(
        applicationName: String,
        applicationIoFile: File,
    ): DictionaryInfo? {
        // Kotlin stdlib File.extension: suffix after the last '.' WITHOUT the dot.
        val appExtension = applicationIoFile.extension
        if (!SystemInfo.isMac && !("xml" == appExtension || "sdef" == appExtension)) return null
        LOG.debug("=== Caching Dictionary for application [$applicationName] ===")
        val serializePath = serializeDictionaryPathForApplication(applicationName)
        var fileGenerated = false
        val targetFile = File(serializePath)
        if (!targetFile.parentFile.exists() && !targetFile.parentFile.mkdirs()) return null
        try {
            if (!SystemInfo.isMac && ("xml" == appExtension || "sdef" == appExtension)) {
                // Plan 03-04 / D-02: copyDictionaryFileToCacheDir is `suspend` and dispatches
                // its I/O onto ioDispatcher. Bridge via runBlockingCancellable since this caller is
                // a non-suspend, @Synchronized chain reachable from non-EDT background threads.
                // The EDT guards on findStdCommands / findApplicationCommands (Codex MEDIUM 1)
                // prevent EDT entry to the synchronous facade surface; non-EDT entry paths are
                // safe for runBlockingCancellable.
                fileGenerated = runBlockingCancellable {
                    copyDictionaryFileToCacheDir(applicationName, applicationIoFile, targetFile, true)
                }
            } else if (SystemInfo.isMac) {
                val cmdName = if ("xml" == appExtension || "sdef" == appExtension) "cat" else "sdef"
                fileGenerated = doGenerateDictionaryFile(applicationName, serializePath, cmdName, applicationIoFile.path)
            }
        } catch (e: NotScriptableApplicationException) {
            LOG.warn("Generation failed: ${e.message}. Adding to ignore list")
            service<SdefPersistenceService>().addNotScriptable(e.getApplicationName())
        } catch (e: DeveloperToolsNotInstalledException) {
            LOG.warn("Generation failed: ${e.message}")
            // Codex MEDIUM 3 / Plan 03-04: the legacy dispatch-thread guard here is REMOVED.
            // copyDictionaryFileToCacheDir routes through `withContext(ioDispatcher)`, so the
            // recovery path is always available regardless of caller thread.
            LOG.warn("Will try to find application scripting definition file...")
            val sdefFile = findSdefForApplication(applicationIoFile)
            if (sdefFile != null && sdefFile.exists()) {
                fileGenerated = runBlockingCancellable {
                    copyDictionaryFileToCacheDir(applicationName, sdefFile, targetFile, true)
                }
            } else {
                LOG.warn("Scripting definition was not found for application ${applicationIoFile.absolutePath}")
                service<SdefPersistenceService>().addNotScriptable(applicationName)
            }
        } finally {
            if (!fileGenerated) {
                LOG.warn("Error occurred while generating file.")
                if (targetFile.delete()) LOG.warn("Created file was deleted")
            } else if (service<ApplicationDiscoveryService>().removeFromNotFoundList(applicationName)) {
                // Wave 3 (SERVICE-03): notFoundApplicationList is owned by ApplicationDiscoveryService.
                LOG.debug("Application was removed from ignored list")
            }
        }
        if (fileGenerated && targetFile.exists()) {
            val applicationBundle =
                if (ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS.contains(appExtension)) applicationIoFile else null
            val dInfo = DictionaryInfo(applicationName, targetFile, applicationBundle)
            // Wave 4 (SERVICE-04): route the registry write through the typed persistence API
            // (which itself forwards to the facade's `addDictionaryInfoInternal` — Pattern A).
            service<SdefPersistenceService>().addDictionaryInfo(dInfo)
            LOG.debug("Dictionary file generated for application [$applicationName]$targetFile")
            return dInfo
        }
        return null
    }

    /**
     * Invoke the `sdef` CLI (macOS-only) or `cat` (cross-platform .xml/.sdef passthrough)
     * to generate the dictionary file at [serializePath]. Throws
     * [NotScriptableApplicationException] when the CLI invocation times out AND Xcode is
     * present (signal: app is genuinely non-scriptable); throws
     * [DeveloperToolsNotInstalledException] when Xcode is absent (signal: CLI unavailable).
     *
     * Dispatcher invariant: non-suspend; safe on any background thread. Uses
     * [Runtime.exec] + [Process.waitFor] with a 5-second timeout (preserved byte-for-byte
     * from the pre-Wave-4 facade — Phase 8 invariant).
     *
     * Body migrated byte-for-byte from the pre-Wave-4 facade `doGenerateDictionaryFile`.
     */
    @Throws(NotScriptableApplicationException::class, DeveloperToolsNotInstalledException::class)
    private fun doGenerateDictionaryFile(
        applicationName: String,
        serializePath: String,
        cmdName: String,
        appFilePath: String,
    ): Boolean {
        try {
            val shellCommand = arrayOf("/bin/bash", "-c", " $cmdName \"$appFilePath\" > $serializePath")
            LOG.debug("executing command: ${Arrays.toString(shellCommand)}")
            val timeout = 5L
            val execStart = System.currentTimeMillis()
            val exitStatus = Runtime.getRuntime().exec(shellCommand).waitFor(timeout, TimeUnit.SECONDS)
            val execEnd = System.currentTimeMillis()
            if (!exitStatus) {
                if (isXcodeInstalled()) {
                    throw NotScriptableApplicationException(
                        applicationName,
                        "Waiting time elapsed for command ${Arrays.toString(shellCommand)}. " +
                            "Seems that application \"$applicationName\" is not scriptable.",
                    )
                } else {
                    throw DeveloperToolsNotInstalledException()
                }
            }
            LOG.debug("Waiting time elapsed. Execution time: ${execEnd - execStart} ms.")
            return true
        } catch (e: InterruptedException) {
            LOG.error(
                "Failed to create dictionary file for application [$applicationName] Command:$cmdName " +
                    "target path: $appFilePath Reason: ${e.cause}",
            )
            e.printStackTrace()
        } catch (e: IOException) {
            LOG.error(
                "Failed to create dictionary file for application [$applicationName] Command:$cmdName " +
                    "target path: $appFilePath Reason: ${e.cause}",
            )
            e.printStackTrace()
        }
        return false
    }

    companion object {
        private val LOG: Logger = Logger.getInstance("#${SdefFileProvider::class.java.name}")

        /**
         * Cache directory for generated `.sdef` files: `${PathManager.getSystemPath()}/sdef`.
         * Migrated from the pre-Wave-4 facade companion. Path layout preserved byte-for-byte
         * (`<systemPath>/sdef/<App_Name>_generated.sdef` after underscore-escaping).
         */
        private val GENERATED_DICTIONARIES_SYSTEM_FOLDER: String = "${PathManager.getSystemPath()}/sdef"

        @JvmStatic
        fun getInstance(): SdefFileProvider =
            ApplicationManager.getApplication().getService(SdefFileProvider::class.java)

        /**
         * Extract an [InputStream] into a temp file. Used by [initStdTerms] (bundled-resource
         * extraction). `deleteOnExit` flagged twice in the pre-Wave-4 facade — preserved
         * verbatim (defensive double-registration of the JVM shutdown hook).
         *
         * Migrated from the pre-Wave-4 facade `stream2file` companion helper.
         */
        @Throws(IOException::class)
        internal fun stream2file(input: InputStream?, prefix: String, suffix: String): File {
            val tempFile = File.createTempFile(prefix, suffix)
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use { out ->
                requireNotNull(input) { "InputStream for $prefix$suffix is null" }
                var c = input.read()
                while (c != -1) {
                    out.write(c)
                    c = input.read()
                }
                input.close()
            }
            tempFile.deleteOnExit()
            return tempFile
        }
    }
}
