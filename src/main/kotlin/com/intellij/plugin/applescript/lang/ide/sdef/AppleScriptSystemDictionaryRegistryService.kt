@file:Suppress("DEPRECATION", "SpellCheckingInspection")

package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.discovery.DiscoveryProgressPolicy
import com.intellij.plugin.applescript.lang.dictionary.discovery.ProgressTaskCompat
import com.intellij.plugin.applescript.lang.dictionary.discovery.ProgressTaskCompatDefault
import com.intellij.plugin.applescript.lang.dictionary.discovery.XcodeDetectionService
import com.intellij.plugin.applescript.lang.dictionary.files.SdefFileProvider
import com.intellij.plugin.applescript.lang.dictionary.filetype.SdefFileTypeRegistrar
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.CollectionBean
import com.intellij.util.xmlb.annotations.Tag
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.io.File

@Service(Service.Level.APP)
@State(
    name = AppleScriptSystemDictionaryRegistryService.COMPONENT_NAME,
    storages = [Storage(value = "appleScriptCachedDictionariesInfo.xml", roamingType = RoamingType.PER_OS)],
)
/*
 * The service intentionally keeps the legacy facade API while data ownership moves to typed
 * collaborators. Removing these methods in one step would break parser/completion call sites
 * and service ABI; keep shrinking this facade before removing the suppression.
 */
@Suppress("TooManyFunctions")
class AppleScriptSystemDictionaryRegistryService
    @JvmOverloads
    constructor(
        // serviceScope is exposed `internal` so ServiceScopeLifecycleIntegrationTest can read its Job
        // tree. Same-module test code naturally accesses `internal` members — no @VisibleForTesting
        // needed on constructor parameters (annotation does not target value parameters in Kotlin).
        internal val serviceScope: CoroutineScope,
        // @JvmOverloads on the primary constructor instructs the Kotlin compiler to emit the
        // `(CoroutineScope)` JVM overload expected by the Platform service container for
        // `@Service(Service.Level.APP)` services (per `InstantiateKt.findConstructor`).
        // Without @JvmOverloads, Platform-instantiated `getInstance()` calls fail with
        // InstantiationException; tests that construct the service manually with a
        // `StandardTestDispatcher` still get the 2-arg overload for runCurrent / advanceUntilIdle
        // determinism (Review HIGH 2). Discovered during Phase 03 gap closure (DEBUG.md REVISION).
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        progressTaskCompat: ProgressTaskCompat = ProgressTaskCompatDefault(),
        daemonRestartScheduler: () -> Unit = DictionaryDaemonRestartScheduler::restartOpenProjectDaemons,
    ) : SimplePersistentStateComponent<AppleScriptSystemDictionaryRegistryService.PersistedState>(PersistedState()) {
        private val dictionaryInfoRegistry = DictionaryInfoRegistry()
        private val notScriptableApplicationRegistry = NotScriptableApplicationRegistry()
        private val persistence: SdefPersistenceService
            get() = SdefPersistenceService.getInstance()
        private val discovery: ApplicationDiscoveryService
            get() = ApplicationDiscoveryService.getInstance()
        private val dictionaryFiles: SdefFileProvider
            get() = SdefFileProvider.getInstance()
        private val readiness = DictionaryReadinessTracker()
        internal val persistenceBridge =
            DictionaryPersistenceBridge(
                dictionaryInfoRegistry = dictionaryInfoRegistry,
                notScriptableApplicationRegistry = notScriptableApplicationRegistry,
                markDiscoveredApplication = { applicationName ->
                    discovery.addDiscoveredApplicationName(applicationName)
                },
            )
        private val initializationCoordinator =
            DictionaryInitializationCoordinator(
                dictionaryInfoRegistry = dictionaryInfoRegistry,
                notScriptableApplicationRegistry = notScriptableApplicationRegistry,
                applicationDiscovery = { discovery },
                dictionaryFiles = { dictionaryFiles },
                areAppDictionariesIndexed = ::areAppDictionariesIndexed,
                parseDictionaryFile = { file, applicationName ->
                    SdefIndexService.getInstance().parseDictionaryFile(file, applicationName)
                },
            )
        private val startupPipeline =
            DictionaryStartupPipeline(
                ioDispatcher = ioDispatcher,
                readiness = readiness,
                actions =
                    DictionaryStartupActions(
                        registerFileTypes = { service<SdefFileTypeRegistrar>().register() },
                        loadCachedDictionaries = { persistenceBridge.loadFromState(state) },
                        initializeStandardDictionaries = {
                            StandardDictionaryInitializer(this@AppleScriptSystemDictionaryRegistryService).initialize()
                        },
                        discoverInstalledApplicationNames = { discoverInstalledApplicationNames() },
                        restartOpenProjectDaemons = daemonRestartScheduler,
                    ),
            )

        // Phase 4 SERVICE-03 (plan 04-03, Wave 3): the `notFoundApplicationList` and
        // `discoveredApplicationNames` ConcurrentHashMap-backed sets migrated to
        // [ApplicationDiscoveryService]. Internal callers within this facade route through
        // `service<ApplicationDiscoveryService>().X()` (init-time hot paths accept the
        // O(1) service-lookup overhead; service<X>() is a static lookup, not a re-instantiation).

        // Phase 4 SERVICE-04 (plan 04-04, Wave 4): the `xCodeApplicationFile` and
        // `scriptingAdditions` fields migrated to [SdefFileProvider]. The
        // `GENERATED_DICTIONARIES_SYSTEM_FOLDER` constant moved to the SdefFileProvider companion.
        // External callers continue to route through facade trampolines (see
        // `isXcodeInstalled`, `getScriptingAdditions`, `getDictionaryFile`, etc. below).

        // Phase 4 SERVICE-05 (plan 04-05, Wave 5): the 14 parser-index ConcurrentHashMap fields
        // (applicationNameTo*Map + std*Map; class, classPlural, command, record, property,
        // enumeration, enumeratorConstant — 7 application-scoped + 7 std-scoped) migrated to
        // [SdefIndexService]. The parser-facing lookup methods now route through
        // ParsableScriptSuiteRegistryHelper directly to the index service. The XML parsing pipeline
        // (parseDictionaryFile + parseSuiteElementForApplication + parseSuiteElementForScriptingAdditions
        // + parseClassElement + 7 companion helpers + newSecureSaxBuilder XXE-hardened factory)
        // also migrated to the service.

        /**
         * Two-stage gating primitives replacing the Phase 1 [java.util.concurrent.CountDownLatch] (D-01, D-04).
         *
         * Typed as `CompletableDeferred<Result<Unit>>` per Review HIGH 1 so failed init is communicated via
         * `complete(Result.failure(...))` (NOT `completeExceptionally(...)`). `isCompleted` alone is NOT
         * a success signal — readers must additionally check `getCompleted().isSuccess`. The two facades
         * [isInitialized] and [areAppDictionariesIndexed] encapsulate this success-semantic predicate.
         *
         * Exposed `@VisibleForTesting internal` so AppCommandGatingTest + DeferredFailureSemanticsTest +
         * ServiceScopeLifecycleIntegrationTest can drive / inspect them deterministically.
         */
        @get:VisibleForTesting
        internal val standardReady: CompletableDeferred<Result<Unit>>
            get() = readiness.standardReady

        @get:VisibleForTesting
        internal val appsReady: CompletableDeferred<Result<Unit>>
            get() = readiness.appsReady

        init {
            // Constructor returns immediately (COROUTINE-05 non-blocking-init invariant). The launch is
            // fire-and-forget; structured concurrency guarantees cancellation on plugin unload / app
            // shutdown via the injected [serviceScope] (RESEARCH §3 verified). `ioDispatcher` is injected
            // (Review HIGH 2) so tests pass `StandardTestDispatcher` for deterministic runCurrent /
            // advanceUntilIdle control of init progression.
            serviceScope.launch {
                startupPipeline.run()
            }

            // D-04 hybrid silent->visible UX (Plan 03-05). Sibling launch on serviceScope so it shares
            // the same structured-concurrency parent as the startup pipeline — both children auto-cancel on
            // plugin unload. The policy stays silent for the first `visibilityThreshold` (2s default,
            // see DiscoveryProgressPolicy); if `appsReady` has not completed by then, surfaces a
            // Task.Backgroundable indicator titled "AppleScript: indexing dictionaries…" with a
            // cancel button. User cancel on the indicator does NOT cancel dictionary startup — they are
            // sibling children (T-03-cancel-leak mitigation per the plan's STRIDE register).
            //
            // Review HIGH 3 — the timing decision + indicator surfacing lives in DiscoveryProgressPolicy,
            // testable via a RecordingFake ProgressTaskCompat. Production uses
            // `Task.Backgroundable(null, ...)` for application-scope progress.
            serviceScope.launch {
                val policy = DiscoveryProgressPolicy(taskCompat = progressTaskCompat)
                policy.runOrTrackProgress("AppleScript: indexing dictionaries…") {
                    readiness.awaitAppsReady()
                }
            }
        }

        /**
         * Returns `true` only when the standard SDEF suite (StandardAdditions + CocoaStandard) has been
         * parsed AND indexed successfully. A completed-but-failed [standardReady] (init threw before the
         * `Result.success(Unit)` line) returns `false` — readers see "not ready" rather than a
         * false-positive "ready" for a failed init.
         *
         * Distinct from [areAppDictionariesIndexed]: this facade reflects the parser fast path readiness
         * (standard-library suite only), while [areAppDictionariesIndexed] reflects the full
         * `/Applications` discovery sweep.
         *
         * @return `true` if [standardReady] completed successfully; `false` if pending OR failed.
         */
        fun isInitialized(): Boolean = readiness.isStandardReady()

        /**
         * Returns `true` only when the full application catalog discovery has completed successfully.
         * Completion contributors and the annotator gate on this facade. A failed [appsReady] returns
         * `false` — readers see "not ready" rather than a false-positive "ready" for a failed
         * app-discovery sweep.
         *
         * Distinct from [isInitialized]: this facade reflects the full app-discovery pipeline, while
         * [isInitialized] reflects only the standard-library readiness.
         *
         * @return `true` if [appsReady] completed successfully; `false` if pending OR failed.
         */
        fun areAppDictionariesIndexed(): Boolean = readiness.areAppsReady()

        /**
         * Bounded-wait helper for standard dictionary readiness.
         */
        internal suspend fun awaitStandardReadyInternal(): Result<Unit> = readiness.awaitStandardReady()

        /**
         * Bounded-wait helper for application dictionary readiness.
         */
        internal suspend fun awaitAppsReadyInternal(): Result<Unit> = readiness.awaitAppsReady()

        // ---------------------------------------------------------------------------------------------
        // Phase 4 SERVICE-02 (plan 04-02, Wave 2) — persistence trampolines used by
        // [SdefPersistenceService].
        //
        // Pattern A from RESEARCH §2: the @State annotation, PersistedState inner class, and
        // SimplePersistentStateComponent inheritance ALL stay on this facade. The service offers a
        // typed API over the persistence bridge. External callers continue to use the public
        // query trampolines (getNotScriptableApplicationList / isNotScriptable /
        // isInUnknownList). SDEF-13 golden fixture regression-locks the wire format — neither
        // PersistedState nor DictionaryInfo.State is touched by this wave.
        // ---------------------------------------------------------------------------------------------

        /**
         * Trampoline (Phase 4 SERVICE-02): defers to
         * [SdefPersistenceService.readNotScriptableSnapshot]. Returns `HashSet<String>` for
         * back-compat with existing call sites (annotator, completion contributors).
         */
        fun getNotScriptableApplicationList(): HashSet<String> = HashSet(persistence.readNotScriptableSnapshot())

        // Phase 4 SERVICE-04 (Wave 4) trampoline: scripting-additions set lives on
        // [SdefFileProvider] (populated by initializeScriptingAdditions, consumed by
        // mergeScriptingAdditions). The defensive-snapshot semantics are preserved by the
        // service's `getScriptingAdditions()` implementation.
        fun getScriptingAdditions(): HashSet<String> = dictionaryFiles.getScriptingAdditions()

        override fun loadState(state: PersistedState) {
            super.loadState(state)
            runCatching {
                // Phase 4 SERVICE-02 trampoline — routes through SdefPersistenceService for clean
                // single-source-of-truth on the load path. The service's loadFromState delegates
                // to DictionaryPersistenceBridge while this facade keeps the persisted component
                // identity and XML state class unchanged.
                persistence.loadFromState(state)
            }.onFailure { LOG.error("Error while loading state for AppleScript dictionaries", it) }
        }

        /**
         * Walks the standard application-bundle directories (Phase 8 invariant — `APP_BUNDLE_DIRECTORIES`
         * preserves `/System/Applications`, `/System/Applications/Utilities`, `~/Applications`) and
         * registers any discovered `.app` / `.osax` / `.sdef` bundles into the discovery service's
         * in-memory name set.
         *
         * Phase 4 SERVICE-03 (Wave 3) trampoline: routes through
         * [ApplicationDiscoveryService.discoverInstalledApplicationNames]. The body now lives on the
         * service; the explicit `withContext(ioDispatcher)` boundary remains there (defence-in-depth
         * even when the startup pipeline already launches on `ioDispatcher`). Public method
         * name preserved verbatim — startup callers and any future external
         * callers see the same signature.
         */
        suspend fun discoverInstalledApplicationNames() {
            discovery.discoverInstalledApplicationNames()
        }

        fun ensureKnownApplicationDictionaryInitialized(knownApplicationName: String): Boolean =
            initializationCoordinator.ensureKnownApplicationDictionaryInitialized(knownApplicationName)

        /**
         * Initialises dictionary information for [applicationName] either from a previously generated cached
         * dictionary file or by generating one. Standard folders are searched for the application's location.
         *
         * @return the [DictionaryInfo] of the generated and cached dictionary for the application, or null
         */
        fun getInitializedInfo(applicationName: String): DictionaryInfo? {
            val coordinator = initializationCoordinator
            return coordinator.getInitializedInfo(applicationName)
        }

        /**
         * Phase 4 SERVICE-03 (Wave 3) trampoline: routes through
         * [ApplicationDiscoveryService.findApplicationBundleFile]. The body now lives on the
         * discovery service (including the EDT guard added in Wave 3 per RESEARCH Open Question 1
         * + Phase 3 Review MEDIUM 1). The pre-Wave-3 visibility was `private`; Wave 3 promotes
         * to `public` to match the typed-API contract — no external callers existed pre-Wave-3,
         * so the visibility widening is harmless.
         *
         * Internal callers within this facade ([getInitializedInfo]) continue to invoke this
         * trampoline (NOT a `*Internal` helper) — the additional `service<X>()` lookup hop is
         * acceptable on this code path (the recursive VFS walk dominates wall-clock time).
         */
        fun findApplicationBundleFile(name: String): File? = discovery.findApplicationBundleFile(name)

        /**
         * Initialises dictionary information for an application from its bundle file (or `.xml`/`.sdef` file)
         * and persists the generated dictionary for later use by the [ApplicationDictionary] PSI class.
         *
         * Phase 4 SERVICE-04 (Wave 4) trampoline: body migrated to
         * [SdefFileProvider.createAndInitializeInfo] which keeps the `@Synchronized` per-app
         * serialisation invariant (the composite chain generate-file → put-info →
         * init-dictionary is naturally serial per app). External callers see the same
         * `(File, String) -> DictionaryInfo?` signature byte-for-byte.
         *
         * @param applicationIoFile file of the application bundle or dictionary file (.app, .osax, .xml, .sdef)
         * @param applicationName name of the macOS application
         * @return the [DictionaryInfo] of the generated, cached and initialised dictionary, or null
         */
        fun createAndInitializeInfo(
            applicationIoFile: File,
            applicationName: String,
        ): DictionaryInfo? = dictionaryFiles.createAndInitializeInfo(applicationIoFile, applicationName)

        /**
         * Phase 4 SERVICE-04 (Wave 4) internal accessor: exposes the in-memory
         * [DictionaryInfo] registry lookup by name for [SdefFileProvider]. The facade still
         * owns the persisted-state-tagged [dictionaryInfoRegistry] (Pattern A — annotation is tied
         * to class identity by `COMPONENT_NAME`); the service reads via this typed accessor
         * to avoid duplicating the map. Returns `null` when no entry exists.
         */
        internal fun getDictionaryInfoByNameInternal(name: String?): DictionaryInfo? = dictionaryInfoRegistry[name]

        /**
         * Phase 4 SERVICE-04 (Wave 4) internal helper: exposes
         * [DictionaryInitializationCoordinator.initializeDictionaryFromInfo] for migrated
         * dictionary-initialization call sites. The coordinator owns parse-and-mark cleanup.
         */
        internal fun initializeDictionaryFromInfoInternal(dictionaryInfo: DictionaryInfo): Boolean =
            initializationCoordinator.initializeDictionaryFromInfo(dictionaryInfo)

        /** Persistent state for the application-level service. Field names are XML attribute names. */
        class PersistedState : BaseState() {
            @JvmField
            @Tag("applicationsInfo")
            @AbstractCollection(surroundWithTag = false)
            var dictionariesInfo: Array<DictionaryInfo.State> = emptyArray()

            @JvmField
            @CollectionBean
            var notScriptableApplications: MutableList<String>? = ArrayList()
        }

        /**
         * Phase 7 D-05 trampoline: routes through [XcodeDetectionService.isXcodeInstalled] (the
         * extracted Xcode-detection seam — was [SdefFileProvider] pre-Phase-7). The body,
         * including the lazy detection cache, lives on that service. External callers
         * ([com.intellij.plugin.applescript.lang.ide.annotator.AppleScriptColorAnnotator]) see
         * the same `() -> Boolean` signature byte-for-byte.
         */
        fun isXcodeInstalled(): Boolean = service<XcodeDetectionService>().isXcodeInstalled()

        /**
         * @return true if `/usr/bin/sdef` invocation previously failed to generate a dictionary
         *
         * Phase 4 SERVICE-02 trampoline: routes through [SdefPersistenceService.isNotScriptable]
         * for external callers (annotator, completion contributors).
         */
        fun isNotScriptable(applicationName: String): Boolean = persistence.isNotScriptable(applicationName)

        /**
         * @return true if the application file was not found earlier in [findApplicationBundleFile]
         *
         * Phase 4 SERVICE-03 trampoline (Wave 3 re-route): routes through
         * [ApplicationDiscoveryService.isInNotFoundList]. Wave 2 originally parked
         * `isInUnknownList` on [SdefPersistenceService] as a temporary spot before
         * [ApplicationDiscoveryService] existed; Wave 3 returns it to its rightful owner —
         * the not-found list is a discovery artifact (NOT persisted, rebuilt per IDE session),
         * so it belongs on the discovery service.
         *
         * External callers ([com.intellij.plugin.applescript.lang.ide.annotator.AppleScriptColorAnnotator])
         * see the same signature byte-for-byte; the re-route is invisible at the call site.
         */
        fun isInUnknownList(applicationName: String): Boolean = discovery.isInNotFoundList(applicationName)

        /**
         * Phase 4 SERVICE-04 (Wave 4) trampoline: routes through
         * [SdefFileProvider.getDictionaryFile]. External callers
         * ([com.intellij.plugin.applescript.lang.sdef.parser.SdefParser]) see the same
         * `(String?) -> File?` signature.
         */
        fun getDictionaryFile(applicationName: String?): File? = dictionaryFiles.getDictionaryFile(applicationName)

        /**
         * Phase 4 SERVICE-04 (Wave 4) trampoline: routes through
         * [SdefFileProvider.getDictionaryInfoByApplicationPath]. External callers
         * ([com.intellij.plugin.applescript.lang.sdef.parser.SdefParser]) see the same
         * `(String) -> DictionaryInfo?` signature.
         */
        fun getDictionaryInfoByApplicationPath(applicationPath: String): DictionaryInfo? =
            dictionaryFiles.getDictionaryInfoByApplicationPath(applicationPath)

        fun getCachedApplicationNames(): Collection<String> = dictionaryInfoRegistry.cachedApplicationNames

        /**
         * Phase 4 SERVICE-03 (Wave 3) trampoline: routes through
         * [ApplicationDiscoveryService.getDiscoveredApplicationNames]. Defensive snapshot
         * semantics preserved (service returns a fresh `HashSet` per call). External callers
         * ([com.intellij.plugin.applescript.lang.ide.completion.ApplicationNameCompletionContributor])
         * see the same signature.
         */
        fun getDiscoveredApplicationNames(): HashSet<String> = discovery.getDiscoveredApplicationNames()

        fun isKnownApplication(appName: String): Boolean = discovery.isKnownApplication(appName)

        fun isDictionaryInitialized(name: String): Boolean = dictionaryInfoRegistry.isInitialized(name)

        // Phase 4 SERVICE-05 (Wave 5): parseDictionaryFile + parseSuiteElementForApplication +
        // parseSuiteElementForScriptingAdditions + parseClassElement migrated to
        // [SdefIndexService]. The XXE-hardened SAXBuilder factory (newSecureSaxBuilder) also
        // moved; the Wave 4 facade-side `newSecureSaxBuilderInternal` is GONE — its single
        // consumer ([SdefFileProvider.mergeScriptingAdditions]) now calls
        // [SdefIndexService.newSecureSaxBuilderForFileProvider] directly.

        companion object {
            private val LOG: Logger =
                Logger.getInstance("#${AppleScriptSystemDictionaryRegistryService::class.java.name}")

            const val COMPONENT_NAME: String = "AppleScriptSystemDictionaryRegistryComponent"

            @JvmStatic
            fun getInstance(): AppleScriptSystemDictionaryRegistryService =
                ApplicationManager.getApplication().getService(AppleScriptSystemDictionaryRegistryService::class.java)

            // Phase 4 SERVICE-05 (Wave 5): the XXE-hardened SAXBuilder factory + 7 XML element
            // helpers + `startsWithWord` migrated to [SdefIndexService] companion. See
            // [SdefIndexService.newSecureSaxBuilderForFileProvider] for the cross-service surface
            // consumed by [SdefFileProvider.mergeScriptingAdditions].
        }
    }
