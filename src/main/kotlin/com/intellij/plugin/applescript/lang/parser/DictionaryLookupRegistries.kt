package com.intellij.plugin.applescript.lang.parser

import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand

internal object DictionaryClassRegistry {
    private val index: SdefIndexService
        get() = SdefIndexService.getInstance()

    fun isStdLibClass(name: String): Boolean = index.lookupStdLibClass(name)

    fun isApplicationClass(
        applicationName: String,
        className: String,
    ): Boolean = index.lookupApplicationClass(applicationName, className)

    fun isStdLibClassPluralName(pluralName: String): Boolean = index.lookupStdLibClassPluralName(pluralName)

    fun isApplicationClassPluralName(
        applicationName: String,
        pluralClassName: String,
    ): Boolean = index.lookupApplicationClassPluralName(applicationName, pluralClassName)

    fun isStdClassWithPrefixExist(prefix: String): Boolean = index.lookupStdClassWithPrefixExist(prefix)

    fun isClassWithPrefixExist(
        applicationName: String,
        classNamePrefix: String,
    ): Boolean = index.lookupClassWithPrefixExist(applicationName, classNamePrefix)

    fun isStdClassPluralWithPrefixExist(prefix: String): Boolean = index.lookupStdClassPluralWithPrefixExist(prefix)

    fun isClassPluralWithPrefixExist(
        applicationName: String,
        pluralClassNamePrefix: String,
    ): Boolean = index.lookupClassPluralWithPrefixExist(applicationName, pluralClassNamePrefix)
}

internal object DictionaryCommandRegistry {
    private val index: SdefIndexService
        get() = SdefIndexService.getInstance()

    fun isStdCommand(name: String): Boolean = index.lookupStdCommand(name)

    fun isApplicationCommand(
        applicationName: String,
        commandName: String,
    ): Boolean = index.lookupApplicationCommand(applicationName, commandName)

    fun isCommandWithPrefixExist(
        applicationName: String,
        commandNamePrefix: String,
    ): Boolean = index.lookupCommandWithPrefixExist(applicationName, commandNamePrefix)

    fun isStdCommandWithPrefixExist(namePrefix: String): Boolean = index.lookupStdCommandWithPrefixExist(namePrefix)

    fun findStdCommands(
        project: Project,
        commandName: String,
    ): Collection<AppleScriptCommand> = index.findStdCommands(project, commandName)

    fun findApplicationCommands(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand> = index.findApplicationCommands(project, applicationName, commandName)
}

internal object DictionaryPropertyRegistry {
    private val index: SdefIndexService
        get() = SdefIndexService.getInstance()

    fun isStdProperty(name: String): Boolean = index.lookupStdProperty(name)

    fun isApplicationProperty(
        applicationName: String,
        propertyName: String,
    ): Boolean = index.lookupApplicationProperty(applicationName, propertyName)

    fun isStdPropertyWithPrefixExist(namePrefix: String): Boolean = index.lookupStdPropertyWithPrefixExist(namePrefix)

    fun isPropertyWithPrefixExist(
        applicationName: String,
        propertyNamePrefix: String,
    ): Boolean = index.lookupPropertyWithPrefixExist(applicationName, propertyNamePrefix)
}

internal object DictionaryConstantRegistry {
    private val index: SdefIndexService
        get() = SdefIndexService.getInstance()

    fun isStdConstant(name: String): Boolean = index.lookupStdConstant(name)

    fun isApplicationConstant(
        applicationName: String,
        constantName: String,
    ): Boolean = index.lookupApplicationConstant(applicationName, constantName)

    fun isStdConstantWithPrefixExist(namePrefix: String): Boolean = index.lookupStdConstantWithPrefixExist(namePrefix)

    fun isConstantWithPrefixExist(
        applicationName: String,
        constantNamePrefix: String,
    ): Boolean = index.lookupConstantWithPrefixExist(applicationName, constantNamePrefix)
}
