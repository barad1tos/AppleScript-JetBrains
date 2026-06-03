package com.intellij.plugin.applescript.lang.parser

import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand

/**
 * Static facade over [ParsableScriptHelper] consumed by the generated parser
 * (see `AppleScriptGeneratedParserUtil`). All methods proxy to the application-level
 * [AppleScriptSystemDictionaryRegistryService].
 *
 * This object intentionally stays flat: the generated parser calls Java-visible static methods,
 * and the parser-util ABI guard freezes that facade shape.
 */
@Suppress("TooManyFunctions")
object ParsableScriptSuiteRegistryHelper {
    private val registry: AppleScriptSystemDictionaryRegistryService
        get() = AppleScriptSystemDictionaryRegistryService.getInstance()

    private val helper: ParsableScriptHelper
        get() = registry

    @JvmStatic
    fun ensureKnownApplicationInitialized(applicationName: String): Boolean =
        helper.ensureKnownApplicationDictionaryInitialized(applicationName)

    @JvmStatic
    fun isStdLibClass(name: String): Boolean = helper.isStdLibClass(name)

    @JvmStatic
    fun isApplicationClass(
        applicationName: String,
        className: String,
    ): Boolean = helper.isApplicationClass(applicationName, className)

    @JvmStatic
    fun isStdLibClassPluralName(pluralName: String): Boolean = helper.isStdLibClassPluralName(pluralName)

    @JvmStatic
    fun isApplicationClassPluralName(
        applicationName: String,
        pluralClassName: String,
    ): Boolean = helper.isApplicationClassPluralName(applicationName, pluralClassName)

    @JvmStatic
    fun isStdClassWithPrefixExist(classNamePrefix: String): Boolean = helper.isStdClassWithPrefixExist(classNamePrefix)

    @JvmStatic
    fun isClassWithPrefixExist(
        applicationName: String,
        classNamePrefix: String,
    ): Boolean = helper.isClassWithPrefixExist(applicationName, classNamePrefix)

    @JvmStatic
    fun isStdClassPluralWithPrefixExist(prefix: String): Boolean = helper.isStdClassPluralWithPrefixExist(prefix)

    @JvmStatic
    fun isClassPluralWithPrefixExist(
        applicationName: String,
        pluralClassNamePrefix: String,
    ): Boolean = helper.isClassPluralWithPrefixExist(applicationName, pluralClassNamePrefix)

    @JvmStatic
    fun isStdCommand(name: String): Boolean = helper.isStdCommand(name)

    @JvmStatic
    fun isApplicationCommand(
        applicationName: String,
        commandName: String,
    ): Boolean = helper.isApplicationCommand(applicationName, commandName)

    @JvmStatic
    fun isCommandWithPrefixExist(
        applicationName: String,
        commandNamePrefix: String,
    ): Boolean = helper.isCommandWithPrefixExist(applicationName, commandNamePrefix)

    @JvmStatic
    fun isStdCommandWithPrefixExist(namePrefix: String): Boolean = helper.isStdCommandWithPrefixExist(namePrefix)

    @JvmStatic
    fun findStdCommands(
        project: Project,
        commandName: String,
    ): Collection<AppleScriptCommand> = helper.findStdCommands(project, commandName)

    @JvmStatic
    fun findApplicationCommands(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand> =
        helper.findApplicationCommands(
            project,
            applicationName,
            commandName,
        )

    @JvmStatic
    fun isStdProperty(name: String): Boolean = helper.isStdProperty(name)

    @JvmStatic
    fun isApplicationProperty(
        applicationName: String,
        propertyName: String,
    ): Boolean = helper.isApplicationProperty(applicationName, propertyName)

    @JvmStatic
    fun isStdPropertyWithPrefixExist(namePrefix: String): Boolean = helper.isStdPropertyWithPrefixExist(namePrefix)

    @JvmStatic
    fun isPropertyWithPrefixExist(
        applicationName: String,
        propertyNamePrefix: String,
    ): Boolean = helper.isPropertyWithPrefixExist(applicationName, propertyNamePrefix)

    @JvmStatic
    fun isStdConstant(name: String): Boolean = helper.isStdConstant(name)

    @JvmStatic
    fun isApplicationConstant(
        applicationName: String,
        constantName: String,
    ): Boolean = helper.isApplicationConstant(applicationName, constantName)

    @JvmStatic
    fun isStdConstantWithPrefixExist(namePrefix: String): Boolean = helper.isStdConstantWithPrefixExist(namePrefix)

    @JvmStatic
    fun isConstantWithPrefixExist(
        applicationName: String,
        constantNamePrefix: String,
    ): Boolean = helper.isConstantWithPrefixExist(applicationName, constantNamePrefix)

    @Suppress("unused")
    @JvmStatic
    fun getScriptingAdditions(): HashSet<String> = helper.getScriptingAdditions()

    // D-01 / D-04 facade dispatchers — additive, no existing method touched (D-08 parser-util
    // contract preserved). The two new booleans live on the registry-service class (NOT the
    // ParsableScriptHelper interface), so these proxies go via getInstance() directly.

    @JvmStatic
    fun isInitialized(): Boolean = registry.isInitialized()

    @JvmStatic
    fun areAppDictionariesIndexed(): Boolean = registry.areAppDictionariesIndexed()

    /**
     * Phase 4 SERVICE-05 (Wave 5) / iteration-2 BLOCKER-fix proxy: lets the dictionary index service
     * bound-wait on the facade-owned `standardReady` Deferred WITHOUT importing the facade directly. Routing
     * through this static-utility class (NOT in the `verifyServiceDependencyGraph` services list)
     * avoids the index-service -> facade back-edge that DFS would detect as a cycle.
     *
     * NOT marked `@JvmStatic` (unlike the 26 frozen-contract methods above) because (a) the only
     * caller is Kotlin code in the dictionary index service — no Java parser-util call site exists — and (b)
     * `@JvmStatic suspend fun` emits a name-mangled JVM signature (the `-IoAF18A` suffix from
     * `Result<Unit>`'s inline-class boxing) that would force the ABI guard to enumerate those
     * mangled names. Keeping these proxies as plain object members preserves the contract count at
     * the original 26.
     */
    suspend fun awaitStandardReady(): Result<Unit> = registry.awaitStandardReadyInternal()

    /**
     * Phase 4 SERVICE-05 (Wave 5) / iteration-2 BLOCKER-fix proxy: same as [awaitStandardReady] but
     * for the `appsReady` Deferred. See [awaitStandardReady] for cycle-prevention rationale.
     */
    suspend fun awaitAppsReady(): Result<Unit> = registry.awaitAppsReadyInternal()
}
