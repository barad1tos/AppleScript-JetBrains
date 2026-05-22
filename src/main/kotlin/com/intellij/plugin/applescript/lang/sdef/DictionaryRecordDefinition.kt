package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

class DictionaryRecordDefinition : AbstractDictionaryComponent<Suite>, DictionaryRecord {

    private var properties: List<AppleScriptPropertyDefinition>? = null

    constructor(
        suite: Suite,
        name: String,
        code: String,
        properties: List<AppleScriptPropertyDefinition>?,
        description: String?,
        xmlTagRecord: XmlTag,
    ) : super(suite, name, code, xmlTagRecord, description) {
        this.properties = properties
    }

    constructor(
        suite: Suite,
        name: String,
        code: String,
        description: String?,
        xmlTagRecord: XmlTag,
    ) : super(suite, name, code, xmlTagRecord, description)

    override fun getProperties(): List<AppleScriptPropertyDefinition> = properties.orEmpty()

    override fun setProperties(properties: List<AppleScriptPropertyDefinition>?) {
        this.properties = properties
    }

    override fun getSuite(): Suite = dictionaryParentComponent
}
