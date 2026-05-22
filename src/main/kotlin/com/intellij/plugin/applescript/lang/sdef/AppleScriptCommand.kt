package com.intellij.plugin.applescript.lang.sdef

interface AppleScriptCommand : DictionaryComponent {

    fun getParameterByName(name: String): CommandParameter?

    fun getParameterNames(): List<String>

    fun getParameters(): List<CommandParameter>

    fun getDirectParameter(): CommandDirectParameter?

    fun getResult(): CommandResult?

    fun setResult(result: CommandResult?): CommandResult?

    fun getMandatoryParameters(): List<CommandParameter>

    fun setParameters(parameters: List<@JvmSuppressWildcards CommandParameter>)

    fun setDirectParameter(directParameter: CommandDirectParameter?)

    override fun getSuite(): Suite
}
