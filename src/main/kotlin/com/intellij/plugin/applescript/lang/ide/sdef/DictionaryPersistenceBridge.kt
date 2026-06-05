package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo

internal class DictionaryPersistenceBridge(
    private val dictionaryInfoRegistry: DictionaryInfoRegistry,
    private val notScriptableApplicationRegistry: NotScriptableApplicationRegistry,
    private val markDiscoveredApplication: (String) -> Unit,
) {
    val dictionaryInfoSnapshot: List<DictionaryInfo>
        get() = dictionaryInfoRegistry.snapshot

    val cachedApplicationNamesSnapshot: List<String>
        get() = dictionaryInfoRegistry.cachedApplicationNamesSnapshot

    val notScriptableSnapshot: Set<String>
        get() = notScriptableApplicationRegistry.snapshot

    fun isDictionaryInitialized(applicationName: String): Boolean {
        return dictionaryInfoRegistry.isInitialized(applicationName)
    }

    fun isNotScriptable(applicationName: String): Boolean = applicationName in notScriptableApplicationRegistry

    fun addNotScriptable(applicationName: String): Boolean = notScriptableApplicationRegistry.add(applicationName)

    fun removeNotScriptable(applicationName: String): Boolean = notScriptableApplicationRegistry.remove(applicationName)

    fun addDictionaryInfo(info: DictionaryInfo): Boolean {
        val applicationName = info.getApplicationName()
        val wasAbsent = dictionaryInfoRegistry.add(info)
        markDiscoveredApplication(applicationName)
        notScriptableApplicationRegistry.remove(applicationName)
        return wasAbsent
    }

    fun removeDictionaryInfoByPath(path: String): Boolean = dictionaryInfoRegistry.removeByPath(path)

    fun loadFromState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        notScriptableApplicationRegistry.readFromState(state)
        dictionaryInfoRegistry.readFromState(state).forEach { info ->
            addDictionaryInfo(info)
        }
    }

    fun writeToState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        dictionaryInfoRegistry.writeToState(state)
        notScriptableApplicationRegistry.writeToState(state)
    }
}
