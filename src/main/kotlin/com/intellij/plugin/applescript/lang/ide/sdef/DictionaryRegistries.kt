package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal class DictionaryInfoRegistry {
    private val dictionaryInfoMap: MutableMap<String, DictionaryInfo> = ConcurrentHashMap()

    val snapshot: List<DictionaryInfo>
        get() = dictionaryInfoMap.values.toList()

    val cachedApplicationNamesSnapshot: List<String>
        get() = dictionaryInfoMap.keys.toList()

    fun removeInMemory(applicationName: String) {
        dictionaryInfoMap.remove(applicationName)
    }

    fun add(info: DictionaryInfo): Boolean = dictionaryInfoMap.put(info.getApplicationName(), info) == null

    fun removeByPath(applicationPath: String): Boolean {
        val matchingKey =
            dictionaryInfoMap.entries
                .firstOrNull { entry -> entry.value.getApplicationFile()?.path == applicationPath }
                ?.key

        val isCocoaStandardPath = applicationPath.endsWith("CocoaStandard.sdef")
        return matchingKey?.let { dictionaryInfoMap.remove(it) != null }
            ?: (isCocoaStandardPath && dictionaryInfoMap.remove(ApplicationDictionary.COCOA_STANDARD_LIBRARY) != null)
    }

    fun writeToState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        state.dictionariesInfo = Array(dictionaryInfoMap.size) { DictionaryInfo.State() }
        val iterator = dictionaryInfoMap.values.iterator()
        for (index in state.dictionariesInfo.indices) {
            state.dictionariesInfo[index] = iterator.next().state
        }
    }

    fun readFromState(state: AppleScriptSystemDictionaryRegistryService.PersistedState): List<DictionaryInfo> =
        state.dictionariesInfo.mapNotNull(::dictionaryInfoFromState).onEach { info ->
            dictionaryInfoMap.remove(info.getApplicationName())
        }

    operator fun get(applicationName: String?): DictionaryInfo? = dictionaryInfoMap[applicationName]

    fun isInitialized(name: String): Boolean = dictionaryInfoMap[name]?.initialized == true
}

internal class NotScriptableApplicationRegistry {
    private val applicationNames: MutableSet<String> = ConcurrentHashMap.newKeySet()

    val snapshot: HashSet<String>
        get() = HashSet(applicationNames)

    operator fun contains(name: String): Boolean = name in applicationNames

    fun add(name: String): Boolean = applicationNames.add(name)

    fun remove(name: String): Boolean = applicationNames.remove(name)

    fun readFromState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        applicationNames.clear()
        state.notScriptableApplications?.let { applicationNames.addAll(it) }
    }

    fun writeToState(state: AppleScriptSystemDictionaryRegistryService.PersistedState) {
        if (state.notScriptableApplications == null) {
            state.notScriptableApplications = ArrayList()
        } else {
            requireNotNull(state.notScriptableApplications) {
                "notScriptableApplications non-null: this is the else branch of the == null check"
            }.clear()
        }
        requireNotNull(state.notScriptableApplications) {
            "notScriptableApplications non-null: assigned-or-cleared in the if/else above"
        }.addAll(applicationNames)
    }
}

private fun dictionaryInfoFromState(state: DictionaryInfo.State): DictionaryInfo? {
    val applicationName = state.applicationName?.takeUnless { StringUtil.isEmptyOrSpaces(it) }
    val dictionaryFile =
        state.dictionaryUrl
            ?.takeUnless { StringUtil.isEmptyOrSpaces(it) }
            ?.let(::File)
            ?.takeIf { it.exists() }
    val applicationFile =
        state.applicationUrl
            ?.takeUnless { StringUtil.isEmpty(it) }
            ?.let(::File)

    return if (applicationName != null && dictionaryFile != null) {
        DictionaryInfo(applicationName, dictionaryFile, applicationFile)
    } else {
        null
    }
}
