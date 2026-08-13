package com.intellij.plugin.applescript.lang.dictionary.index

import java.util.concurrent.ConcurrentHashMap

internal class SdefIndexStore {
    val classNamesByApplication: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val pluralClassNamesByApplication: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val commandNamesByApplication: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val recordNamesByApplication: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val propertyNamesByApplication: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val enumerationNamesByApplication: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val enumeratorNamesByApplication: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()

    val applicationsByClassName: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationsByPluralClassName: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationsByCommandName: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationsByRecordName: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val dictionariesByPropertyName: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationsByEnumerationName: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    val applicationsByEnumeratorName: MutableMap<String, MutableSet<String>> =
        ConcurrentHashMap()

    fun snapshot(): SdefIndexSnapshot =
        SdefIndexSnapshot(
            applicationNameToClassNameSet =
                classNamesByApplication.mapValues { it.value.toSet() },
            applicationNameToClassNamePluralSet =
                pluralClassNamesByApplication.mapValues { it.value.toSet() },
            applicationNameToCommandNameSet =
                commandNamesByApplication.mapValues { it.value.toSet() },
            applicationNameToRecordNameSet =
                recordNamesByApplication.mapValues { it.value.toSet() },
            applicationNameToPropertySet =
                propertyNamesByApplication.mapValues { it.value.toSet() },
            applicationNameToEnumerationNameSet =
                enumerationNamesByApplication.mapValues { it.value.toSet() },
            applicationNameToEnumeratorConstantNameSet =
                enumeratorNamesByApplication
                    .mapValues { it.value.toSet() },
            stdClassNameToApplicationNameSet =
                applicationsByClassName.mapValues { it.value.toSet() },
            stdClassNamePluralToApplicationNameSet =
                applicationsByPluralClassName
                    .mapValues { it.value.toSet() },
            stdCommandNameToApplicationNameSet =
                applicationsByCommandName.mapValues { it.value.toSet() },
            stdRecordNameToApplicationNameSet =
                applicationsByRecordName.mapValues { it.value.toSet() },
            stdPropertyNameToDictionarySet =
                dictionariesByPropertyName.mapValues { it.value.toSet() },
            stdEnumerationNameToApplicationNameSet =
                applicationsByEnumerationName
                    .mapValues { it.value.toSet() },
            stdEnumeratorConstantNameToApplicationNameList =
                applicationsByEnumeratorName
                    .mapValues { it.value.toSet() },
        )
}
