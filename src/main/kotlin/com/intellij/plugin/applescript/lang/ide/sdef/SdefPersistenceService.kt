package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope

/**
 * Phase 4 SERVICE-02 (plan 04-02, Wave 2): typed API over the facade's persisted-state-tagged
 * [AppleScriptSystemDictionaryRegistryService.PersistedState] field.
 *
 * IMPORTANT — Pattern A (RESEARCH §2): this service does NOT carry the persisted-state
 * annotation, does NOT inherit from the Platform persistence base class, and does NOT
 * own any storage. The annotation MUST stay on
 * [AppleScriptSystemDictionaryRegistryService] because the storage component name
 * (`"AppleScriptSystemDictionaryRegistryComponent"`, see `COMPONENT_NAME`) is tied to
 * class identity in every existing user's `appleScriptCachedDictionariesInfo.xml` cache.
 * Moving the annotation = lost user caches (PITFALLS 4.1 BLOCKER).
 *
 * What this service owns: a typed, side-effect-explicit API over the facade's in-memory
 * dictionary registry. Callers (within the SDEF package, completion contributors,
 * annotator, actions) stop touching facade-private fields directly and instead route
 * through these typed methods. The implementation forwards to `internal fun *Internal()`
 * helpers on the facade — the facade still owns the [java.util.concurrent.ConcurrentHashMap]
 * backing fields (HOTFIX-01 invariant) and the persisted-state-driven persistence machinery.
 *
 * Wire-format contract: the SDEF-13 golden fixture (Phase 2 `PersistenceGoldenFixtureTest`)
 * regression-locks every byte of the v1.0 XML format. This service touches neither
 * [AppleScriptSystemDictionaryRegistryService.PersistedState] nor [DictionaryInfo.State]
 * annotations — Wave 2 must leave both inner classes byte-identical.
 *
 * Threading: the facade's in-memory state is `ConcurrentHashMap` / `ConcurrentHashMap.newKeySet()`
 * (HOTFIX-01); no `@Synchronized` is needed at the service layer. Persistent-state
 * synchronisation is handled by the Platform's persistence machinery on the facade
 * (RESEARCH §6).
 *
 * Constructor follows Phase 3 COROUTINE-03: constructor-injected [CoroutineScope] +
 * `@JvmOverloads` emits the single-arg `(CoroutineScope)` JVM ctor that the Platform
 * service container expects for application-level [Service]-annotated classes (per
 * `InstantiateKt.findConstructor`). No I/O is launched by this service today — the
 * scope is held for future suspending APIs (Wave 6 cleanup may add async batch writes).
 *
 * Lifecycle: Light Service, lazy-on-first-access. The facade triggers it via
 * `service<SdefPersistenceService>().loadFromState(state)` from the `loadState(state)`
 * override (after `super.loadState(state)` wires the PSC state) and via
 * `service<SdefPersistenceService>().writeToState(state)` from `updateState()`.
 *
 * Light Service per [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html):
 * no `<applicationService>` entry needed in plugin.xml.
 */
@Service(Service.Level.APP)
class SdefPersistenceService constructor(
    @Suppress("unused") private val serviceScope: CoroutineScope,
) {

    private fun facade(): AppleScriptSystemDictionaryRegistryService =
        AppleScriptSystemDictionaryRegistryService.getInstance()

    /**
     * Defensive snapshot of the in-memory [DictionaryInfo] collection. Returns a [List]
     * (not the live backing `Collection`), so callers cannot accidentally mutate the
     * facade's registry through the returned reference.
     */
    fun readDictionaryInfoSnapshot(): List<DictionaryInfo> =
        facade().dictionaryInfoSnapshotInternal()

    /**
     * Atomically replace the facade's in-memory dictionary collection with [infos] and
     * trigger [writeToState] so the change is persisted. Used by callers that batch-import
     * dictionaries (e.g., a future `LoadDictionaryAction` rewrite); not used in Wave 2
     * production code paths, but the API is part of the typed contract that completes
     * the read/write surface.
     */
    fun persistDictionaryInfoSnapshot(infos: Collection<DictionaryInfo>) {
        facade().replaceDictionaryInfoCollectionInternal(infos)
    }

    /**
     * Defensive snapshot of the notScriptable application names. Returns a [Set]
     * (not the live backing `ConcurrentHashMap.KeySet`).
     */
    fun readNotScriptableSnapshot(): Set<String> =
        facade().notScriptableSnapshotInternal()

    /**
     * O(1) membership test on the persisted notScriptable set.
     */
    fun isNotScriptable(applicationName: String): Boolean =
        facade().isNotScriptableInternal(applicationName)

    /**
     * O(1) membership test on the in-memory "not found" list (apps that the discovery
     * walk recorded as missing from APP_BUNDLE_DIRECTORIES). This list is NOT persisted —
     * it is rebuilt per IDE session. Routing it through this service centralises the
     * read surface so future refactors (Wave 3 ApplicationDiscoveryService) can migrate
     * the owner without touching every caller.
     */
    fun isInUnknownList(applicationName: String): Boolean =
        facade().isInUnknownListInternal(applicationName)

    /**
     * Add an application to the persisted notScriptable set; returns `true` if the
     * name was newly added (idempotent). Does NOT immediately call [writeToState] —
     * the Platform's PSC machinery serialises on its own cadence (per `getState()`),
     * matching the v1.0 behaviour byte-for-byte (SDEF-13 fixture invariant).
     */
    fun addNotScriptable(applicationName: String): Boolean =
        facade().addNotScriptableInternal(applicationName)

    /**
     * Remove an application from the persisted notScriptable set; returns `true` if
     * the name was present and removed.
     */
    fun removeNotScriptable(applicationName: String): Boolean =
        facade().removeNotScriptableInternal(applicationName)

    /**
     * Register a [DictionaryInfo] in the facade's in-memory registry; returns `true` if
     * the entry is newly registered (existing entries are overwritten and `true` is
     * returned by convention, matching the matrix in 04-02-PLAN). Also removes the
     * application from the notScriptable list, mirroring the historical
     * `addDictionaryInfo` private helper semantics on the facade.
     */
    fun addDictionaryInfo(info: DictionaryInfo): Boolean =
        facade().addDictionaryInfoInternal(info)

    /**
     * Remove a [DictionaryInfo] by its application path. Returns `true` if a matching
     * entry was found and removed. The lookup walks the facade's registry comparing
     * each entry's `applicationFile.path`; standard libraries (which have no
     * application bundle file) are matched by the well-known `CocoaStandard.sdef`
     * suffix (mirroring the facade's existing `getDictionaryInfoByApplicationPath`
     * resolution).
     */
    fun removeDictionaryInfo(applicationPath: String): Boolean =
        facade().removeDictionaryInfoByPathInternal(applicationPath)

    /**
     * Populates the facade's in-memory [DictionaryInfo] collection from the just-loaded
     * [AppleScriptSystemDictionaryRegistryService.PersistedState]. Called from the
     * facade's `loadState(state)` override AFTER `super.loadState(state)` has wired the
     * PSC state field.
     *
     * Preserves byte-for-byte the v1.0 wire format — the implementation simply forwards
     * to the existing `initDictionariesInfoFromCacheInternal` helper on the facade,
     * which does the same work as the pre-Wave-2 private method.
     */
    fun loadFromState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        facade().initDictionariesInfoFromCacheInternal(state)
    }

    /**
     * Writes the facade's in-memory [DictionaryInfo] collection back into
     * [state.dictionariesInfo][AppleScriptSystemDictionaryRegistryService.PersistedState.dictionariesInfo]
     * and the notScriptable set into
     * [state.notScriptableApplications][AppleScriptSystemDictionaryRegistryService.PersistedState.notScriptableApplications].
     * Called from the facade's `updateState()` method.
     */
    fun writeToState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        facade().writeToStateInternal(state)
    }

    companion object {
        @Suppress("unused")
        private val LOG: Logger = Logger.getInstance("#${SdefPersistenceService::class.java.name}")

        @JvmStatic
        fun getInstance(): SdefPersistenceService =
            ApplicationManager.getApplication().getService(SdefPersistenceService::class.java)
    }
}
