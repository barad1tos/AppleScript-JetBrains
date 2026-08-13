package com.intellij.plugin.applescript.lang.sdef

internal object DictionaryTermCatalog {
    fun allTerms(dictionary: ApplicationDictionary): List<DictionaryComponent> =
        buildList {
            addAll(dictionary.dictionaryEnumeratorMap.values)
            addAll(dictionary.dictionaryPropertyMap.values)
            addAll(dictionary.dictionaryClassMap.values)
            addAll(dictionary.allCommands)
        }

    fun missingTerms(
        dictionary: ApplicationDictionary,
        baseline: ApplicationDictionary,
    ): List<DictionaryComponent> =
        buildList {
            dictionary.dictionaryEnumeratorMap.values.filterTo(this) {
                baseline.findEnumerator(it.getName()) == null
            }
            dictionary.dictionaryClassMap.values.filterTo(this) {
                baseline.findClass(it.getName()) == null
            }
            dictionary.allCommands.filterTo(this) {
                baseline.findCommand(it.getName()) == null
            }
            dictionary.dictionaryPropertyMap.values.filterTo(this) {
                baseline.findProperty(it.getName()) == null
            }
        }
}
