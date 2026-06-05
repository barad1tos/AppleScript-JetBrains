package com.intellij.plugin.applescript.lang.dictionary.persistence

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService

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
 * What this service owns: a typed, side-effect-explicit API over the facade's persistence bridge.
 * Callers (within the SDEF package, completion contributors, annotator, actions) stop touching
 * facade-private fields directly and instead route through these typed methods. The facade still
 * owns the persisted-state identity, while the bridge owns in-memory registry mutations.
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
 * Constructor is intentionally no-arg: this service does not launch coroutine work,
 * and Platform state persistence is owned by the facade.
 *
 * Lifecycle: Light Service, lazy-on-first-access. The facade triggers [loadFromState]
 * from the `loadState(state)` override after `super.loadState(state)` wires the PSC state.
 * The Platform serialises the facade state on its normal persistence cadence.
 *
 * Light Service per [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html):
 * no `<applicationService>` entry needed in plugin.xml.
 */
@Service(Service.Level.APP)
class SdefPersistenceService {
    private val facade: AppleScriptSystemDictionaryRegistryService
        get() = AppleScriptSystemDictionaryRegistryService.getInstance()
    private val bridge
        get() = facade.persistenceBridge

    /**
     * Defensive snapshot of the in-memory [DictionaryInfo] collection. Returns a [List]
     * (not the live backing `Collection`), so callers cannot accidentally mutate the
     * facade's registry through the returned reference.
     */
    val dictionaryInfoSnapshot: List<DictionaryInfo>
        get() = bridge.dictionaryInfoSnapshot

    /**
     * Defensive snapshot of cached application names backed by the dictionary registry keys.
     */
    val cachedApplicationNamesSnapshot: List<String>
        get() = bridge.cachedApplicationNamesSnapshot

    /**
     * O(1) initialized-dictionary lookup for annotator and completion hot paths.
     */
    fun isDictionaryInitialized(applicationName: String): Boolean = bridge.isDictionaryInitialized(applicationName)

    /**
     * Defensive snapshot of the notScriptable application names. Returns a [Set]
     * (not the live backing `ConcurrentHashMap.KeySet`).
     */
    val notScriptableSnapshot: Set<String>
        get() = bridge.notScriptableSnapshot

    /**
     * O(1) membership test on the persisted notScriptable set.
     */
    fun isNotScriptable(applicationName: String): Boolean = bridge.isNotScriptable(applicationName)

    /**
     * Add an application to the persisted notScriptable set; returns `true` if the
     * name was newly added (idempotent). Does NOT immediately call [writeToState] —
     * the Platform's PSC machinery serialises on its own cadence (per `getState()`),
     * matching the v1.0 behaviour byte-for-byte (SDEF-13 fixture invariant).
     */
    fun addNotScriptable(applicationName: String): Boolean = bridge.addNotScriptable(applicationName)

    /**
     * Remove an application from the persisted notScriptable set; returns `true` if
     * the name was present and removed.
     */
    fun removeNotScriptable(applicationName: String): Boolean = bridge.removeNotScriptable(applicationName)

    /**
     * Register a [DictionaryInfo] in the facade's in-memory registry; returns `true` if
     * the entry is newly registered (existing entries are overwritten and `true` is
     * returned by convention, matching the matrix in 04-02-PLAN). Also removes the
     * application from the notScriptable list, mirroring the historical
     * `addDictionaryInfo` private helper semantics on the facade.
     */
    fun addDictionaryInfo(info: DictionaryInfo): Boolean = bridge.addDictionaryInfo(info)

    /**
     * Remove a [DictionaryInfo] by its application path. Returns `true` if a matching
     * entry was found and removed. The lookup walks the facade's registry comparing
     * each entry's `applicationFile.path`; standard libraries (which have no
     * application bundle file) are matched by the well-known `CocoaStandard.sdef`
     * suffix (mirroring the facade's existing `getDictionaryInfoByApplicationPath`
     * resolution).
     */
    fun removeDictionaryInfo(path: String): Boolean = bridge.removeDictionaryInfoByPath(path)

    /**
     * Populates the facade's in-memory [DictionaryInfo] collection from the just-loaded
     * [AppleScriptSystemDictionaryRegistryService.PersistedState]. Called from the
     * facade's `loadState(state)` override AFTER `super.loadState(state)` has wired the
     * PSC state field.
     *
     * Preserves byte-for-byte the v1.0 wire format.
     */
    fun loadFromState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        bridge.loadFromState(state)
    }

    /**
     * Writes the facade's in-memory [DictionaryInfo] collection back into
     * `PersistedState.dictionariesInfo` and the notScriptable set into
     * `PersistedState.notScriptableApplications`.
     */
    fun writeToState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        bridge.writeToState(state)
    }

    companion object {
        private val application
            get() = ApplicationManager.getApplication()

        @JvmStatic
        fun getInstance(): SdefPersistenceService = application.getService(SdefPersistenceService::class.java)
    }
}
