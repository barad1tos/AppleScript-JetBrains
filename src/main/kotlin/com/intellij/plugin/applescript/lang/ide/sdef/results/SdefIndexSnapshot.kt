package com.intellij.plugin.applescript.lang.ide.sdef.results

/**
 * Phase 4 D-03: Immutable snapshot of the SDEF index state.
 *
 * Returned by [com.intellij.plugin.applescript.lang.ide.sdef.SdefIndexService.snapshot]. Callers
 * can hand this to lookup methods for hermetic-test reads OR observe it post-ingest in production.
 *
 * Modelled on Phase 2 [com.intellij.plugin.applescript.lang.sdef.DictionaryIndexes][SDEF-05] value
 * container. Each field is a read-only `Map<String, Set<String>>` — defensive snapshot, NOT the
 * live mutable index.
 *
 * Field names match the EXACT names on the pre-Wave-5 facade (post-Wave-4 baseline), so a future
 * tool that round-trips a snapshot back into the live indexes (Wave 6+) gets a 1:1 mapping.
 *
 * 14 maps total:
 *  - 7 application-scoped: class, classPlural, command, record, property, enumeration, enumeratorConstant
 *  - 7 std-scoped: class, classPlural, command, record, property, enumeration, enumeratorConstant
 */
data class SdefIndexSnapshot(
    // ── Application-scoped maps (applicationName -> set of objectNames) ──
    val applicationNameToClassNameSet: Map<String, Set<String>>,
    val applicationNameToClassNamePluralSet: Map<String, Set<String>>,
    val applicationNameToCommandNameSet: Map<String, Set<String>>,
    val applicationNameToRecordNameSet: Map<String, Set<String>>,
    val applicationNameToPropertySet: Map<String, Set<String>>,
    val applicationNameToEnumerationNameSet: Map<String, Set<String>>,
    val applicationNameToEnumeratorConstantNameSet: Map<String, Set<String>>,

    // ── Std-scoped maps (objectName -> set of applicationNames that defined it as std) ──
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
    fun isApplicationCommand(applicationName: String, commandName: String): Boolean =
        commandName in (applicationNameToCommandNameSet[applicationName] ?: emptySet())

    /** Hermetic-test convenience: returns true if `name` is present in the std class index. */
    fun isStdLibClass(name: String): Boolean = name in stdClassNameToApplicationNameSet

    /** Hermetic-test convenience: returns true if `propertyName` is present in the application's property set. */
    fun isApplicationProperty(applicationName: String, propertyName: String): Boolean =
        propertyName in (applicationNameToPropertySet[applicationName] ?: emptySet())
}
