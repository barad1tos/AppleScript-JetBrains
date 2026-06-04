package com.intellij.plugin.applescript.lang.dictionary.index

import java.util.concurrent.ConcurrentHashMap

internal class SdefIndexStore {
    val applicationNameToClassNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationNameToClassNamePluralSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationNameToCommandNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationNameToRecordNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationNameToPropertySetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationNameToEnumerationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationNameToEnumeratorConstantNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()

    val stdClassNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val stdClassNamePluralToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val stdCommandNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val stdRecordNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val stdPropertyNameToDictionarySetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val stdEnumerationNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val stdEnumeratorConstantNameToApplicationNameListMap: MutableMap<String, MutableSet<String>> =
        ConcurrentHashMap()

    fun snapshot(): SdefIndexSnapshot =
        SdefIndexSnapshot(
            applicationNameToClassNameSet =
                applicationNameToClassNameSetMap.mapValues { it.value.toSet() },
            applicationNameToClassNamePluralSet =
                applicationNameToClassNamePluralSetMap.mapValues { it.value.toSet() },
            applicationNameToCommandNameSet =
                applicationNameToCommandNameSetMap.mapValues { it.value.toSet() },
            applicationNameToRecordNameSet =
                applicationNameToRecordNameSetMap.mapValues { it.value.toSet() },
            applicationNameToPropertySet =
                applicationNameToPropertySetMap.mapValues { it.value.toSet() },
            applicationNameToEnumerationNameSet =
                applicationNameToEnumerationNameSetMap.mapValues { it.value.toSet() },
            applicationNameToEnumeratorConstantNameSet =
                applicationNameToEnumeratorConstantNameSetMap
                    .mapValues { it.value.toSet() },
            stdClassNameToApplicationNameSet =
                stdClassNameToApplicationNameSetMap.mapValues { it.value.toSet() },
            stdClassNamePluralToApplicationNameSet =
                stdClassNamePluralToApplicationNameSetMap
                    .mapValues { it.value.toSet() },
            stdCommandNameToApplicationNameSet =
                stdCommandNameToApplicationNameSetMap.mapValues { it.value.toSet() },
            stdRecordNameToApplicationNameSet =
                stdRecordNameToApplicationNameSetMap.mapValues { it.value.toSet() },
            stdPropertyNameToDictionarySet =
                stdPropertyNameToDictionarySetMap.mapValues { it.value.toSet() },
            stdEnumerationNameToApplicationNameSet =
                stdEnumerationNameToApplicationNameSetMap
                    .mapValues { it.value.toSet() },
            stdEnumeratorConstantNameToApplicationNameList =
                stdEnumeratorConstantNameToApplicationNameListMap
                    .mapValues { it.value.toSet() },
        )
}
