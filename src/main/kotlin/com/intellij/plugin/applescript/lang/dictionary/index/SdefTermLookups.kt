package com.intellij.plugin.applescript.lang.dictionary.index

internal fun hasNameWithPrefix(
    namePrefix: String,
    names: Set<String>?,
): Boolean = names?.any { objectName -> objectName.startsWithWord(namePrefix) } == true

private fun String.startsWithWord(prefix: String): Boolean {
    if (!startsWith(prefix)) return false
    val isCompleteWord = prefix.length == length || this[prefix.length] == ' '
    return isCompleteWord
}

internal class SdefClassLookup(
    private val indexStore: SdefIndexStore,
) {
    fun lookupStdLibClass(name: String): Boolean =
        SdefIndexReadiness.isInitialized() &&
            indexStore.stdClassNameToApplicationNameSetMap.containsKey(name)

    fun lookupApplicationClass(
        applicationName: String,
        className: String,
    ): Boolean {
        if (!SdefIndexReadiness.isInitialized()) return false
        val classNameSet: Set<String>? = indexStore.applicationNameToClassNameSetMap[applicationName]
        return classNameSet != null && classNameSet.contains(className)
    }

    fun lookupStdLibClassPluralName(pluralName: String): Boolean =
        SdefIndexReadiness.isInitialized() &&
            indexStore.stdClassNamePluralToApplicationNameSetMap.containsKey(pluralName)

    fun lookupApplicationClassPluralName(
        applicationName: String,
        pluralName: String,
    ): Boolean {
        if (!SdefIndexReadiness.isInitialized()) return false
        val classNameSet: Set<String>? =
            indexStore.applicationNameToClassNamePluralSetMap[applicationName]
        return classNameSet != null && classNameSet.contains(pluralName)
    }

    fun lookupStdClassWithPrefixExist(classNamePrefix: String): Boolean =
        SdefIndexReadiness.isInitialized() &&
            hasNameWithPrefix(classNamePrefix, indexStore.stdClassNameToApplicationNameSetMap.keys)

    fun lookupClassWithPrefixExist(
        applicationName: String,
        classNamePrefix: String,
    ): Boolean =
        SdefIndexReadiness.isInitialized() &&
            hasNameWithPrefix(classNamePrefix, indexStore.applicationNameToClassNameSetMap[applicationName])

    fun lookupStdClassPluralWithPrefixExist(namePrefix: String): Boolean =
        SdefIndexReadiness.isInitialized() &&
            hasNameWithPrefix(namePrefix, indexStore.stdClassNamePluralToApplicationNameSetMap.keys)

    fun lookupClassPluralWithPrefixExist(
        applicationName: String,
        pluralNamePrefix: String,
    ): Boolean =
        SdefIndexReadiness.isInitialized() &&
            hasNameWithPrefix(
                pluralNamePrefix,
                indexStore.applicationNameToClassNamePluralSetMap[applicationName],
            )
}

internal class SdefPropertyLookup(
    private val indexStore: SdefIndexStore,
) {
    fun lookupStdProperty(name: String): Boolean =
        SdefIndexReadiness.isInitialized() &&
            indexStore.stdPropertyNameToDictionarySetMap.containsKey(name)

    fun lookupStdPropertyWithPrefixExist(namePrefix: String): Boolean =
        SdefIndexReadiness.isInitialized() &&
            hasNameWithPrefix(namePrefix, indexStore.stdPropertyNameToDictionarySetMap.keys)

    fun lookupApplicationProperty(
        applicationName: String,
        propertyName: String,
    ): Boolean {
        if (!SdefIndexReadiness.isInitialized()) return false
        val propertyNameSet: Set<String>? = indexStore.applicationNameToPropertySetMap[applicationName]
        return propertyNameSet != null && propertyNameSet.contains(propertyName)
    }

    fun lookupPropertyWithPrefixExist(
        applicationName: String,
        propertyNamePrefix: String,
    ): Boolean =
        SdefIndexReadiness.isInitialized() &&
            hasNameWithPrefix(propertyNamePrefix, indexStore.applicationNameToPropertySetMap[applicationName])
}

internal class SdefConstantLookup(
    private val indexStore: SdefIndexStore,
) {
    fun lookupStdConstant(name: String): Boolean =
        SdefIndexReadiness.isInitialized() &&
            indexStore.stdEnumeratorConstantNameToApplicationNameListMap.containsKey(name)

    fun lookupApplicationConstant(
        applicationName: String,
        constantName: String,
    ): Boolean {
        if (!SdefIndexReadiness.isInitialized()) return false
        val constantNameSet: Set<String>? =
            indexStore.applicationNameToEnumeratorConstantNameSetMap[applicationName]
        return constantNameSet != null && constantNameSet.contains(constantName)
    }

    fun lookupStdConstantWithPrefixExist(namePrefix: String): Boolean =
        SdefIndexReadiness.isInitialized() &&
            hasNameWithPrefix(namePrefix, indexStore.stdEnumeratorConstantNameToApplicationNameListMap.keys)

    fun lookupConstantWithPrefixExist(
        applicationName: String,
        constantNamePrefix: String,
    ): Boolean =
        SdefIndexReadiness.isInitialized() &&
            hasNameWithPrefix(
                constantNamePrefix,
                indexStore.applicationNameToEnumeratorConstantNameSetMap[applicationName],
            )
}
