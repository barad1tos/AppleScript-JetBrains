package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo

internal class DictionaryPersistenceBridge(
    private val dictionaryInfoRegistry: DictionaryInfoRegistry,
    private val notScriptableRegistry: NotScriptableRegistry,
    private val markDiscoveredApplication: (String) -> Unit,
) {
    val dictionaryInfoSnapshot: List<DictionaryInfo>
        get() = dictionaryInfoRegistry.snapshot

    val cachedApplicationNamesSnapshot: List<String>
        get() = dictionaryInfoRegistry.cachedApplicationNamesSnapshot

    val notScriptableSnapshot: Set<String>
        get() = notScriptableRegistry.snapshot

    fun isDictionaryInitialized(applicationName: String): Boolean =
        dictionaryInfoRegistry.isInitialized(applicationName)

    fun isNotScriptable(applicationName: String): Boolean = applicationName in notScriptableRegistry

    fun addNotScriptable(applicationName: String): Boolean = notScriptableRegistry.add(applicationName)

    fun removeNotScriptable(applicationName: String): Boolean = notScriptableRegistry.remove(applicationName)

    fun addDictionaryInfo(info: DictionaryInfo): Boolean {
        val applicationName = info.getApplicationName()
        val wasAbsent = dictionaryInfoRegistry.add(info)
        markDiscoveredApplication(applicationName)
        notScriptableRegistry.remove(applicationName)
        return wasAbsent
    }

    fun removeDictionaryInfoByPath(path: String): Boolean = dictionaryInfoRegistry.removeByPath(path)

    fun removeDictionaryInfoByName(applicationName: String) {
        dictionaryInfoRegistry.removeInMemory(applicationName)
    }

    fun loadFromState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        notScriptableRegistry.readFromState(state)
        dictionaryInfoRegistry.readFromState(state).forEach { info ->
            addDictionaryInfo(info)
        }
    }

    fun writeToState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        dictionaryInfoRegistry.writeToState(state)
        notScriptableRegistry.writeToState(state)
    }
}
