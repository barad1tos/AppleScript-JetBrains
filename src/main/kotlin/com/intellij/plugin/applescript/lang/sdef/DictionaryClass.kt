package com.intellij.plugin.applescript.lang.sdef

import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.AppleScriptDocHelper
import com.intellij.psi.xml.XmlTag

class DictionaryClass :
    AbstractDictionaryComponent<Suite>,
    AppleScriptClass {

    private var properties: List<AppleScriptPropertyDefinition> = emptyList()
    private val parentClassName: String?
    private var pluralClassName: String
    private val elements: MutableList<AppleScriptClass> = mutableListOf()
    private val respondingCommands: MutableList<AppleScriptCommand> = mutableListOf()
    private val elementNames: List<String>
    private val respondingCommandNames: List<String>
    private var initialized: Boolean = false

    constructor(
        suite: Suite,
        name: String,
        code: String,
        xmlTagClass: XmlTag,
        parentClassName: String?,
        elementNames: List<String>?,
        respondingCommandNames: List<String>?,
        pluralClassName: String?,
    ) : super(suite, name, code, xmlTagClass, null) {
        this.parentClassName = parentClassName
        this.pluralClassName = if (StringUtil.isEmpty(pluralClassName)) "${name}s" else pluralClassName!!
        this.elementNames = elementNames ?: emptyList()
        this.respondingCommandNames = respondingCommandNames ?: emptyList()
    }

    constructor(
        suite: Suite,
        name: String,
        code: String,
        properties: List<AppleScriptPropertyDefinition>,
        xmlTagClass: XmlTag,
        description: String?,
        parentClassName: String?,
    ) : super(suite, name, code, xmlTagClass, description) {
        this.properties = properties
        this.parentClassName = parentClassName
        this.pluralClassName = "${name}s"
        this.elementNames = emptyList()
        this.respondingCommandNames = emptyList()
    }

    override fun getDocFooter(): String = buildString {
        AppleScriptDocHelper.appendClassAttributes(this, this@DictionaryClass)
        val parentClass = getParentClass()
        if (parentClass != null) {
            val indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
            append("<p>").append(indent).append("INHERITED FROM ").append(parentClass.getName().uppercase()).append("</p>")
            AppleScriptDocHelper.appendClassAttributes(this, parentClass)
        }
        append("</HTML>")
    }

    @Suppress("UNCHECKED_CAST")
    override fun getContents(): List<AppleScriptClass> = emptyList()

    override fun getProperties(): List<AppleScriptPropertyDefinition> = properties

    override fun setProperties(properties: List<AppleScriptPropertyDefinition>) {
        this.properties = properties
    }

    override fun getSuite(): Suite = myParent

    override fun getParentClassName(): String? = parentClassName

    override fun getParentClass(): AppleScriptClass? = getDictionary().findClass(parentClassName)

    override fun getElementNames(): List<String> = elementNames

    override fun getElements(): List<AppleScriptClass> {
        if (!initialized) initializeElements()
        return elements
    }

    override fun getRespondingCommands(): List<AppleScriptCommand> {
        if (!initialized) initializeElements()
        return respondingCommands
    }

    private fun initializeElements() {
        for (className in elementNames) {
            getDictionary().findClass(className)?.let { elements.add(it) }
        }
        for (commandName in respondingCommandNames) {
            getDictionary().findCommand(commandName)?.let { respondingCommands.add(it) }
        }
        initialized = true
    }

    override fun getPluralClassName(): String = pluralClassName

    override fun setPluralClassName(pluralClassName: String): DictionaryClass {
        if (!StringUtil.isEmpty(pluralClassName)) {
            this.pluralClassName = pluralClassName
        }
        return this
    }
}
