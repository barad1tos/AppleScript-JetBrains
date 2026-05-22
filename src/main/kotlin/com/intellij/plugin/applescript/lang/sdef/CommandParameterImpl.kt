package com.intellij.plugin.applescript.lang.sdef

import com.intellij.psi.xml.XmlTag

class CommandParameterImpl :
    AbstractDictionaryComponent<AppleScriptCommand>,
    CommandParameter {

    private val typeSpecifier: String
    private val optional: Boolean

    constructor(
        myCommand: AppleScriptCommand,
        name: String,
        code: String,
        optional: Boolean,
        typeSpecifier: String,
        description: String?,
        xmlTagParameter: XmlTag,
    ) : super(myCommand, name, code, xmlTagParameter, description) {
        this.typeSpecifier = typeSpecifier
        this.optional = optional
    }

    constructor(
        myCommand: AppleScriptCommand,
        name: String,
        code: String,
        typeSpecifier: String,
        xmlTagParameter: XmlTag,
    ) : super(myCommand, name, code, xmlTagParameter) {
        this.typeSpecifier = typeSpecifier
        this.optional = false
    }

    override fun getTypeSpecifier(): String = typeSpecifier

    override fun getMyCommand(): AppleScriptCommand = myParent

    override fun isOptional(): Boolean = optional

    override fun getSuite(): Suite = getMyCommand().getSuite()
}
