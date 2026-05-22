package com.intellij.plugin.applescript.lang.parser

import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand

/**
 * Contract used by the generated parser to query already-initialised application dictionaries.
 *
 * Implementations must be cheap for the parser hot path — they should never trigger initialisation of
 * dictionaries for application names that were not discovered in the standard paths at IDE startup,
 * otherwise the parser would block on partially-typed application identifiers.
 */
interface ParsableScriptHelper {

    /**
     * Ensures that terms from [knownApplicationName] were initialised by
     * [com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService]
     * before the parser queries them. For performance reasons does NOT attempt to initialise an
     * application whose name was not discovered in standard paths at startup — e.g., the parser
     * should not try to initialise an application name that the user has not finished typing.
     *
     * @return true if the dictionary for the application was initialised
     */
    fun ensureKnownApplicationDictionaryInitialized(knownApplicationName: String): Boolean

    // Application classes
    fun isStdLibClass(name: String): Boolean

    fun isApplicationClass(applicationName: String, className: String): Boolean

    fun isStdLibClassPluralName(pluralName: String): Boolean

    fun isApplicationClassPluralName(applicationName: String, pluralClassName: String): Boolean

    fun isStdClassWithPrefixExist(classNamePrefix: String): Boolean

    fun isClassWithPrefixExist(applicationName: String, classNamePrefix: String): Boolean

    fun isStdClassPluralWithPrefixExist(namePrefix: String): Boolean

    fun isClassPluralWithPrefixExist(applicationName: String, pluralClassNamePrefix: String): Boolean

    // Application commands
    fun isStdCommand(name: String): Boolean

    fun isApplicationCommand(applicationName: String, commandName: String): Boolean

    fun isCommandWithPrefixExist(applicationName: String, commandNamePrefix: String): Boolean

    fun isStdCommandWithPrefixExist(namePrefix: String): Boolean

    fun findStdCommands(project: Project, commandName: String): Collection<AppleScriptCommand>

    fun findApplicationCommands(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand>

    // Application properties
    fun isStdProperty(name: String): Boolean

    fun isStdPropertyWithPrefixExist(namePrefix: String): Boolean

    fun isApplicationProperty(applicationName: String, propertyName: String): Boolean

    fun isPropertyWithPrefixExist(applicationName: String, propertyNamePrefix: String): Boolean

    // Application constants (enumerators)
    fun isStdConstant(name: String): Boolean

    fun isApplicationConstant(applicationName: String, constantName: String): Boolean

    fun isStdConstantWithPrefixExist(namePrefix: String): Boolean

    fun isConstantWithPrefixExist(applicationName: String, namePrefix: String): Boolean

    fun getScriptingAdditions(): HashSet<String>
}
