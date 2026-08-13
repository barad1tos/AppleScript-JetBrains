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
        SdefIndexReadiness.isStandardReady() &&
            indexStore.applicationsByClassName.containsKey(name)

    fun lookupApplicationClass(
        applicationName: String,
        className: String,
    ): Boolean {
        if (!SdefIndexReadiness.isApplicationReady(applicationName)) return false
        val classNames: Set<String>? = indexStore.classNamesByApplication[applicationName]
        return classNames != null && classNames.contains(className)
    }

    fun lookupStdLibClassPluralName(pluralName: String): Boolean =
        SdefIndexReadiness.isStandardReady() &&
            indexStore.applicationsByPluralClassName.containsKey(pluralName)

    fun lookupApplicationClassPluralName(
        applicationName: String,
        pluralName: String,
    ): Boolean {
        if (!SdefIndexReadiness.isApplicationReady(applicationName)) return false
        val classNames: Set<String>? =
            indexStore.pluralClassNamesByApplication[applicationName]
        return classNames != null && classNames.contains(pluralName)
    }

    fun lookupStdClassWithPrefixExist(classNamePrefix: String): Boolean =
        SdefIndexReadiness.isStandardReady() &&
            hasNameWithPrefix(classNamePrefix, indexStore.applicationsByClassName.keys)

    fun lookupClassWithPrefixExist(
        applicationName: String,
        classNamePrefix: String,
    ): Boolean =
        SdefIndexReadiness.isApplicationReady(applicationName) &&
            hasNameWithPrefix(classNamePrefix, indexStore.classNamesByApplication[applicationName])

    fun lookupStdClassPluralWithPrefixExist(namePrefix: String): Boolean =
        SdefIndexReadiness.isStandardReady() &&
            hasNameWithPrefix(namePrefix, indexStore.applicationsByPluralClassName.keys)

    fun lookupClassPluralWithPrefixExist(
        applicationName: String,
        pluralNamePrefix: String,
    ): Boolean =
        SdefIndexReadiness.isApplicationReady(applicationName) &&
            hasNameWithPrefix(
                pluralNamePrefix,
                indexStore.pluralClassNamesByApplication[applicationName],
            )
}

internal class SdefPropertyLookup(
    private val indexStore: SdefIndexStore,
) {
    fun lookupStdProperty(name: String): Boolean =
        SdefIndexReadiness.isStandardReady() &&
            indexStore.dictionariesByPropertyName.containsKey(name)

    fun lookupStdPropertyWithPrefixExist(namePrefix: String): Boolean =
        SdefIndexReadiness.isStandardReady() &&
            hasNameWithPrefix(namePrefix, indexStore.dictionariesByPropertyName.keys)

    fun lookupApplicationProperty(
        applicationName: String,
        propertyName: String,
    ): Boolean {
        if (!SdefIndexReadiness.isApplicationReady(applicationName)) return false
        val propertyNames: Set<String>? = indexStore.propertyNamesByApplication[applicationName]
        return propertyNames != null && propertyNames.contains(propertyName)
    }

    fun lookupPropertyWithPrefixExist(
        applicationName: String,
        propertyNamePrefix: String,
    ): Boolean =
        SdefIndexReadiness.isApplicationReady(applicationName) &&
            hasNameWithPrefix(propertyNamePrefix, indexStore.propertyNamesByApplication[applicationName])
}

internal class SdefConstantLookup(
    private val indexStore: SdefIndexStore,
) {
    fun lookupStdConstant(name: String): Boolean =
        SdefIndexReadiness.isStandardReady() &&
            indexStore.applicationsByEnumeratorName.containsKey(name)

    fun lookupApplicationConstant(
        applicationName: String,
        constantName: String,
    ): Boolean {
        if (!SdefIndexReadiness.isApplicationReady(applicationName)) return false
        val constantNames: Set<String>? =
            indexStore.enumeratorNamesByApplication[applicationName]
        return constantNames != null && constantNames.contains(constantName)
    }

    fun lookupStdConstantWithPrefixExist(namePrefix: String): Boolean =
        SdefIndexReadiness.isStandardReady() &&
            hasNameWithPrefix(namePrefix, indexStore.applicationsByEnumeratorName.keys)

    fun lookupConstantWithPrefixExist(
        applicationName: String,
        constantNamePrefix: String,
    ): Boolean =
        SdefIndexReadiness.isApplicationReady(applicationName) &&
            hasNameWithPrefix(
                constantNamePrefix,
                indexStore.enumeratorNamesByApplication[applicationName],
            )
}
