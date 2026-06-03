package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

private const val DICTIONARY_QUALIFIED_NAME_TYPE_PREFIX = "dictionary "

/**
 * Hybrid PSI + immutable value type pattern (D-01 keystone of v1.1).
 *
 *  - The class extends `AbstractDictionaryComponent` → `DictionaryComponentBase` →
 *    `FakePsiElement` so it remains a real PSI node (PsiManager caches,
 *    `areElementsEquivalent`, Find Usages, rename refactoring all keep working).
 *  - The class holds `private val data: SuiteDefinition` (immutable scalar value
 *    type). Every scalar accessor (`isHidden`, `getName` via super, `getCode`
 *    via super, `getDescription` via super) reads from `data`.
 *  - The map / list cluster (`commandDefinitions`, `classDefinitionsMap`,
 *    `classDefinitionToCodeMap`, `commandDefinitionToCodeMap`,
 *    `propertyDefinitions`, `dictionaryRecordList`, `enumerations`,
 *    `dictionaryCommandMap`) stays in `SuiteImpl` as a building/lookup concern
 *    for v1.1. The parser mutates these via `addX` methods during the two-pass
 *    walk; lifting them into a hypothetical `SuiteIndexes` value class is v1.3
 *    service-split scope (CONTEXT D-07 + ARCHITECTURE §3).
 *  - Why not `data class : FakePsiElement`? PITFALLS §1.1 BLOCKER: synthesizes
 *    `equals`/`hashCode` from primary-constructor props, overriding the
 *    platform's PSI identity contract.
 *
 * SDEF-11 fix (moved here from plan 02-06 per checker warning 5): the previous
 * `addCommand` body returned `commandDefinitions.add(command) &&
 * dictionaryCommandMap.put(name, command) != null` — the `!= null` clause
 * inverts the documented contract because `Map.put` returns the OLD value
 * (`null` on first insert). The fix matches the
 * `ApplicationDictionaryImpl.addCommand` convention at line 191 (`== null`
 * = first insert). Regression locked by `SuiteAddCommandTest`.
 */
class SuiteImpl :
    AbstractDictionaryComponent<ApplicationDictionary>,
    Suite {
    private val data: SuiteDefinition
    private val commandDefinitions: MutableList<AppleScriptCommand> = mutableListOf()
    private val classDefinitions: MutableList<AppleScriptClass> = mutableListOf()
    private val classDefinitionsMap: MutableMap<String, AppleScriptClass> = mutableMapOf()
    private val classDefinitionToCodeMap: MutableMap<String, AppleScriptClass> = mutableMapOf()
    private val commandDefinitionToCodeMap: MutableMap<String, AppleScriptCommand> = mutableMapOf()
    private val propertyDefinitions: MutableList<AppleScriptPropertyDefinition> = mutableListOf()
    private val dictionaryRecordList: MutableList<DictionaryRecord> = mutableListOf()
    private val enumerations: MutableList<DictionaryEnumeration> = mutableListOf()
    private val dictionaryCommandMap: MutableMap<String, AppleScriptCommand> = mutableMapOf()

    constructor(
        dictionary: ApplicationDictionary,
        code: String,
        name: String,
        hidden: Boolean,
        description: String?,
        xmlTagSuite: XmlTag,
    ) : super(dictionary, name, code, xmlTagSuite, description) {
        this.data =
            SuiteBuilder(name = name, code = code)
                .hidden(hidden)
                .description(description)
                .build()
    }

    override val isHidden: Boolean get() = data.hidden

    override val suite: Suite get() = this

    override val type: String get() = "dictionary suite"

    override val dictionary: ApplicationDictionary get() = myParent

    override val documentation: String
        get() =
            buildString {
                append("<html>")
                append(super.documentation)
                val sep = "  ===================  "
                append("<p>").append("COMMANDS").append("<br>")
                for (command in commandDefinitions) {
                    append("<br>").append("<b>").append(sep).append("</b>")
                    val commandDoc = command.documentation
                    append("<p>").append(commandDoc.substring(commandDoc.indexOf("Command"))).append("</p>")
                }
                append("</p>")
                append("<p>").append("CLASSES").append("<br>")
                for (aClass in classDefinitions) {
                    append("<br>").append("<b>").append(sep).append("</b>")
                    val classDoc = aClass.documentation
                    append("<p>").append(classDoc.substring(classDoc.indexOf("Class"))).append("</p>")
                }
                append("</p>")
                append("</html>")
            }

    override val qualifiedPath: String get() = "${dictionary.qualifiedPath}/$qualifiedName"

    override val qualifiedName: String
        get() = type.removePrefix(DICTIONARY_QUALIFIED_NAME_TYPE_PREFIX) + ":" + code

    override fun addClass(appleScriptClass: AppleScriptClass): Boolean {
        classDefinitions.add(appleScriptClass)
        classDefinitionsMap[appleScriptClass.getName()] = appleScriptClass
        classDefinitionToCodeMap[appleScriptClass.code] = appleScriptClass
        return true
    }

    override fun getClassByName(name: String): AppleScriptClass? = classDefinitionsMap[name]

    override fun findClassByCode(code: String): AppleScriptClass? = classDefinitionToCodeMap[code]

    override fun findCommandByCode(code: String): AppleScriptCommand? = commandDefinitionToCodeMap[code]

    override fun addProperty(property: AppleScriptPropertyDefinition): Boolean = propertyDefinitions.add(property)

    override fun addEnumeration(enumeration: DictionaryEnumeration): Boolean = enumerations.add(enumeration)

    override fun addRecord(record: DictionaryRecord) {
        dictionaryRecordList.add(record)
    }

    /**
     * SDEF-11 fix (moved from plan 02-06 per checker warning 5). Convention
     * matches ApplicationDictionaryImpl.addCommand at line 191:
     *
     *  - `true` on first insert of `(name, command)`.
     *  - `false` on duplicate (a command with the same name was already present).
     *
     * The .add to `commandDefinitions` is unconditional (mirrors the pre-fix
     * intent — the suite tracks every command it observes, even if the name-
     * keyed map already had a same-named entry from an earlier insert). The
     * code-keyed map is populated unconditionally too because callers expect
     * `findCommandByCode` to resolve both the first and any later same-name
     * command added to the suite. Regression lock: SuiteAddCommandTest.
     */
    override fun addCommand(command: AppleScriptCommand): Boolean {
        commandDefinitions.add(command)
        commandDefinitionToCodeMap[command.code] = command
        return dictionaryCommandMap.put(command.getName(), command) == null
    }
}
