package com.intellij.plugin.applescript.lang.dictionary.index

/**
 * Immutable snapshot of the SDEF index state.
 *
 * Each map is a defensive copy. Callers can use the snapshot for hermetic reads without mutating
 * the live indexes. Property names retain the established public API.
 */
data class SdefIndexSnapshot(
    val applicationNameToClassNameSet: Map<String, Set<String>>,
    val applicationNameToClassNamePluralSet: Map<String, Set<String>>,
    val applicationNameToCommandNameSet: Map<String, Set<String>>,
    val applicationNameToRecordNameSet: Map<String, Set<String>>,
    val applicationNameToPropertySet: Map<String, Set<String>>,
    val applicationNameToEnumerationNameSet: Map<String, Set<String>>,
    val applicationNameToEnumeratorConstantNameSet: Map<String, Set<String>>,
    val stdClassNameToApplicationNameSet: Map<String, Set<String>>,
    val stdClassNamePluralToApplicationNameSet: Map<String, Set<String>>,
    val stdCommandNameToApplicationNameSet: Map<String, Set<String>>,
    val stdRecordNameToApplicationNameSet: Map<String, Set<String>>,
    val stdPropertyNameToDictionarySet: Map<String, Set<String>>,
    val stdEnumerationNameToApplicationNameSet: Map<String, Set<String>>,
    val stdEnumeratorConstantNameToApplicationNameList: Map<String, Set<String>>,
) {
    /** Hermetic-test convenience: returns true if `name` is present in the std command index. */
    fun isStdCommand(name: String): Boolean = name in stdCommandNameToApplicationNameSet

    /** Hermetic-test convenience: returns true if `commandName` is present in the application's command set. */
    fun isApplicationCommand(
        applicationName: String,
        commandName: String,
    ): Boolean = commandName in (applicationNameToCommandNameSet[applicationName] ?: emptySet())

    /** Hermetic-test convenience: returns true if `name` is present in the std class index. */
    fun isStdLibClass(name: String): Boolean = name in stdClassNameToApplicationNameSet

    /** Hermetic-test convenience: returns true if `propertyName` is present in the application's property set. */
    fun isApplicationProperty(
        applicationName: String,
        propertyName: String,
    ): Boolean = propertyName in (applicationNameToPropertySet[applicationName] ?: emptySet())
}
