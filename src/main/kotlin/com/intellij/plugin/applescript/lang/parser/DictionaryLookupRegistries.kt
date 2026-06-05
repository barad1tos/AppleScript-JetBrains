package com.intellij.plugin.applescript.lang.parser

import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand

internal object DictionaryClassRegistry {
    private val index: SdefIndexService
        get() = SdefIndexService.getInstance()

    fun isStdLibClass(name: String): Boolean = index.classLookup.lookupStdLibClass(name)

    fun isApplicationClass(
        applicationName: String,
        className: String,
    ): Boolean = index.classLookup.lookupApplicationClass(applicationName, className)

    fun isStdLibClassPluralName(pluralName: String): Boolean = index.classLookup.lookupStdLibClassPluralName(pluralName)

    fun isApplicationClassPluralName(
        applicationName: String,
        pluralClassName: String,
    ): Boolean = index.classLookup.lookupApplicationClassPluralName(applicationName, pluralClassName)

    fun isStdClassWithPrefixExist(prefix: String): Boolean = index.classLookup.lookupStdClassWithPrefixExist(prefix)

    fun isClassWithPrefixExist(
        applicationName: String,
        classNamePrefix: String,
    ): Boolean = index.classLookup.lookupClassWithPrefixExist(applicationName, classNamePrefix)

    fun isStdClassPluralWithPrefixExist(prefix: String): Boolean =
        index.classLookup.lookupStdClassPluralWithPrefixExist(
            prefix,
        )

    fun isClassPluralWithPrefixExist(
        applicationName: String,
        pluralClassNamePrefix: String,
    ): Boolean = index.classLookup.lookupClassPluralWithPrefixExist(applicationName, pluralClassNamePrefix)
}

internal object DictionaryCommandRegistry {
    private val index: SdefIndexService
        get() = SdefIndexService.getInstance()

    fun isStdCommand(name: String): Boolean = index.commandLookup.lookupStdCommand(name)

    fun isApplicationCommand(
        applicationName: String,
        commandName: String,
    ): Boolean = index.commandLookup.lookupApplicationCommand(applicationName, commandName)

    fun isCommandWithPrefixExist(
        applicationName: String,
        commandNamePrefix: String,
    ): Boolean = index.commandLookup.lookupCommandWithPrefixExist(applicationName, commandNamePrefix)

    fun isStdCommandWithPrefixExist(namePrefix: String): Boolean =
        index.commandLookup.lookupStdCommandWithPrefixExist(
            namePrefix,
        )

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

    fun isStdProperty(name: String): Boolean = index.propertyLookup.lookupStdProperty(name)

    fun isApplicationProperty(
        applicationName: String,
        propertyName: String,
    ): Boolean = index.propertyLookup.lookupApplicationProperty(applicationName, propertyName)

    fun isStdPropertyWithPrefixExist(namePrefix: String): Boolean =
        index.propertyLookup.lookupStdPropertyWithPrefixExist(
            namePrefix,
        )

    fun isPropertyWithPrefixExist(
        applicationName: String,
        propertyNamePrefix: String,
    ): Boolean = index.propertyLookup.lookupPropertyWithPrefixExist(applicationName, propertyNamePrefix)
}

internal object DictionaryConstantRegistry {
    private val index: SdefIndexService
        get() = SdefIndexService.getInstance()

    fun isStdConstant(name: String): Boolean = index.constantLookup.lookupStdConstant(name)

    fun isApplicationConstant(
        applicationName: String,
        constantName: String,
    ): Boolean = index.constantLookup.lookupApplicationConstant(applicationName, constantName)

    fun isStdConstantWithPrefixExist(namePrefix: String): Boolean =
        index.constantLookup.lookupStdConstantWithPrefixExist(
            namePrefix,
        )

    fun isConstantWithPrefixExist(
        applicationName: String,
        constantNamePrefix: String,
    ): Boolean = index.constantLookup.lookupConstantWithPrefixExist(applicationName, constantNamePrefix)
}
