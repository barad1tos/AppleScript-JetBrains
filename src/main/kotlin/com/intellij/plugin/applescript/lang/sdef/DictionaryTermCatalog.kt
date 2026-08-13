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
        baseline: ApplicationDictionary?,
    ): List<DictionaryComponent> {
        val availableBaseline = baseline ?: return emptyList()
        return buildList {
            dictionary.dictionaryEnumeratorMap.values.filterTo(this) {
                availableBaseline.findEnumerator(it.getName()) == null
            }
            dictionary.dictionaryClassMap.values.filterTo(this) {
                availableBaseline.findClass(it.getName()) == null
            }
            dictionary.allCommands.filterTo(this) {
                availableBaseline.findCommand(it.getName()) == null
            }
            dictionary.dictionaryPropertyMap.values.filterTo(this) {
                availableBaseline.findProperty(it.getName()) == null
            }
        }
    }
}
