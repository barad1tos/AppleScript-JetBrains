package com.intellij.plugin.applescript.lang.sdef

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.xml.XmlTag

open class AppleScriptCommandImpl :
    AbstractDictionaryComponent<Suite>,
    AppleScriptCommand {

    private val parameters: MutableList<CommandParameter> = mutableListOf()
    private val mandatoryParameters: MutableList<CommandParameter> = mutableListOf()
    private var directParameter: CommandDirectParameter? = null
    private val parameterNames: MutableList<String> = mutableListOf()
    private val parametersMap: MutableMap<String, CommandParameter> = mutableMapOf()
    private var result: CommandResult? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var cocoaClass: String? = null

    constructor(
        suite: Suite,
        name: String,
        code: String,
        parameters: List<CommandParameter>?,
        directParameter: CommandDirectParameter?,
        result: CommandResult?,
        description: String?,
        xmlTagCommand: XmlTag,
    ) : super(suite, name, code, xmlTagCommand, description) {
        this.directParameter = directParameter
        this.result = result
        if (parameters != null) setParameters(parameters)
    }

    constructor(
        suite: Suite,
        name: String,
        code: String,
        xmlTagCommand: XmlTag,
    ) : super(suite, name, code, xmlTagCommand)

    override fun getParameterByName(name: String): CommandParameter? = parametersMap[name]

    override fun getParameterNames(): List<String> = parameterNames

    override fun getParameters(): List<CommandParameter> = parameters

    override fun getDirectParameter(): CommandDirectParameter? = directParameter

    override fun getResult(): CommandResult? = result

    override fun setResult(result: CommandResult?): CommandResult? {
        this.result = result
        return result
    }

    override fun getMandatoryParameters(): List<CommandParameter> = mandatoryParameters

    override fun setParameters(parameters: List<CommandParameter>) {
        for (parameter in parameters) {
            parameterNames.add(parameter.getName())
            this.parameters.add(parameter)
            parametersMap[parameter.getName()] = parameter
            // Workaround for "in" / "of" being detected as object references rather than parameters.
            if (!parameter.isOptional() && parameter.getName() != "in" && parameter.getName() != "of") {
                mandatoryParameters.add(parameter)
            }
        }
    }

    override fun setDirectParameter(directParameter: CommandDirectParameter?) {
        this.directParameter = directParameter
    }

    override fun getCocoaClassName(): String? = cocoaClass

    override fun getDocFooter(): String {
        val sb = StringBuilder()
        val indent = "&nbsp;&nbsp;&nbsp;&nbsp;"
        val p = getDirectParameter()
        val params = getParameters()
        if (p != null || params.isNotEmpty()) {
            sb.append("<p><b>Parameters:</b></p>")
        }
        if (p != null) {
            sb.append(indent).append(indent).append(p.getTypeSpecifier()).append(" : ")
                .append(StringUtil.notNullize(p.getDescription())).append("<br>")
        }
        for (par in params) {
            val (op, cl) = if (par.isOptional()) "[" to "]" else "" to ""
            val pType = StringUtil.notNullize(par.getTypeSpecifier())
            sb.append(indent).append(indent).append(op).append("<b>").append(par.getName()).append("</b> ")
                .append(pType).append(cl).append(" : ").append(par.getDescription()).append("<br>")
        }
        val res = getResult()
        if (res != null) {
            sb.append("<p>").append("<b>Returns:</b></p>").append(indent).append(indent)
                .append(res.getType()).append(" : ").append(StringUtil.notNullize(res.getDescription()))
        }
        return sb.toString()
    }

    override fun getSuite(): Suite = myParent
}
