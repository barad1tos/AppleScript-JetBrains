package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

class SuiteImpl :
    AbstractDictionaryComponent<ApplicationDictionary>,
    Suite {

    private val hidden: Boolean
    private val commandDefinitions: MutableList<AppleScriptCommand> = mutableListOf()
    private val classDefinitions: MutableList<AppleScriptClass> = mutableListOf()
    private val classDefinitionsMap: MutableMap<String, AppleScriptClass> = mutableMapOf()
    private val classDefinitionToCodeMap: MutableMap<String, AppleScriptClass> = mutableMapOf()
    private val commandDefinitionToCodeMap: MutableMap<String, AppleScriptCommand> = mutableMapOf()
    private val propertyDefinitions: MutableList<AppleScriptPropertyDefinition> = mutableListOf()
    private val dictionaryRecordList: MutableList<DictionaryRecord> = mutableListOf()
    private val dictionaryEnumerationList: MutableList<DictionaryEnumeration> = mutableListOf()
    private val dictionaryCommandMap: MutableMap<String, AppleScriptCommand> = mutableMapOf()

    constructor(
        code: String,
        name: String,
        dictionary: ApplicationDictionary,
        xmlTagSuite: XmlTag,
    ) : super(dictionary, name, code, xmlTagSuite) {
        this.hidden = false
    }

    constructor(
        dictionary: ApplicationDictionary,
        code: String,
        name: String,
        hidden: Boolean,
        description: String?,
        xmlTagSuite: XmlTag,
    ) : super(dictionary, name, code, xmlTagSuite, description) {
        this.hidden = hidden
    }

    override fun isHidden(): Boolean = hidden

    override fun getSuite(): Suite = this

    override fun getType(): String = "dictionary suite"

    override fun getDictionary(): ApplicationDictionary = myParent

    override fun getDocumentation(): String = buildString {
        append("<html>")
        append(super.getDocumentation())
        val sep = "  ===================  "
        append("<p>").append("COMMANDS").append("<br>")
        for (command in commandDefinitions) {
            append("<br>").append("<b>").append(sep).append("</b>")
            val commandDoc = command.getDocumentation()
            append("<p>").append(commandDoc.substring(commandDoc.indexOf("Command"))).append("</p>")
        }
        append("</p>")
        append("<p>").append("CLASSES").append("<br>")
        for (aClass in classDefinitions) {
            append("<br>").append("<b>").append(sep).append("</b>")
            val classDoc = aClass.getDocumentation()
            append("<p>").append(classDoc.substring(classDoc.indexOf("Class"))).append("</p>")
        }
        append("</p>")
        append("</html>")
    }

    override fun getQualifiedPath(): String = "${getDictionary().getQualifiedPath()}/${getQualifiedName()}"

    override fun getQualifiedName(): String = getType().substring(11) + ":" + getCode()

    override fun addClass(appleScriptClass: AppleScriptClass): Boolean {
        classDefinitions.add(appleScriptClass)
        classDefinitionsMap[appleScriptClass.getName()] = appleScriptClass
        appleScriptClass.getCode()?.let { classDefinitionToCodeMap[it] = appleScriptClass }
        return true
    }

    override fun getClassByName(name: String): AppleScriptClass? = classDefinitionsMap[name]

    override fun findClassByCode(code: String): AppleScriptClass? = classDefinitionToCodeMap[code]

    override fun findCommandByCode(code: String): AppleScriptCommand? = commandDefinitionToCodeMap[code]

    override fun addProperty(property: AppleScriptPropertyDefinition): Boolean = propertyDefinitions.add(property)

    override fun addEnumeration(enumeration: DictionaryEnumeration): Boolean = dictionaryEnumerationList.add(enumeration)

    override fun addRecord(record: DictionaryRecord) {
        dictionaryRecordList.add(record)
    }

    override fun addCommand(command: AppleScriptCommand): Boolean {
        command.getCode()?.let { commandDefinitionToCodeMap[it] = command }
        return commandDefinitions.add(command) && dictionaryCommandMap.put(command.getName(), command) != null
    }
}
