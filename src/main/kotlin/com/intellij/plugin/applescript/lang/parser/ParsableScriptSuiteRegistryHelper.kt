package com.intellij.plugin.applescript.lang.parser

import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand

/**
 * Static facade over [ParsableScriptHelper] consumed by the generated parser
 * (see `AppleScriptGeneratedParserUtil`). All methods proxy to the application-level
 * [AppleScriptSystemDictionaryRegistryService].
 */
object ParsableScriptSuiteRegistryHelper {

    private val scriptHelper: ParsableScriptHelper
        get() = AppleScriptSystemDictionaryRegistryService.getInstance()

    @JvmStatic
    fun ensureKnownApplicationInitialized(applicationName: String): Boolean =
        scriptHelper.ensureKnownApplicationDictionaryInitialized(applicationName)

    @JvmStatic
    fun isStdLibClass(name: String): Boolean = scriptHelper.isStdLibClass(name)

    @JvmStatic
    fun isApplicationClass(applicationName: String, className: String): Boolean =
        scriptHelper.isApplicationClass(applicationName, className)

    @JvmStatic
    fun isStdLibClassPluralName(pluralName: String): Boolean = scriptHelper.isStdLibClassPluralName(pluralName)

    @JvmStatic
    fun isApplicationClassPluralName(applicationName: String, pluralClassName: String): Boolean =
        scriptHelper.isApplicationClassPluralName(applicationName, pluralClassName)

    @JvmStatic
    fun isStdClassWithPrefixExist(classNamePrefix: String): Boolean =
        scriptHelper.isStdClassWithPrefixExist(classNamePrefix)

    @JvmStatic
    fun isClassWithPrefixExist(applicationName: String, classNamePrefix: String): Boolean =
        scriptHelper.isClassWithPrefixExist(applicationName, classNamePrefix)

    @JvmStatic
    fun isStdClassPluralWithPrefixExist(namePrefix: String): Boolean =
        scriptHelper.isStdClassPluralWithPrefixExist(namePrefix)

    @JvmStatic
    fun isClassPluralWithPrefixExist(applicationName: String, pluralClassNamePrefix: String): Boolean =
        scriptHelper.isClassPluralWithPrefixExist(applicationName, pluralClassNamePrefix)

    @JvmStatic
    fun isStdCommand(name: String): Boolean = scriptHelper.isStdCommand(name)

    @JvmStatic
    fun isApplicationCommand(applicationName: String, commandName: String): Boolean =
        scriptHelper.isApplicationCommand(applicationName, commandName)

    @JvmStatic
    fun isCommandWithPrefixExist(applicationName: String, commandNamePrefix: String): Boolean =
        scriptHelper.isCommandWithPrefixExist(applicationName, commandNamePrefix)

    @JvmStatic
    fun isStdCommandWithPrefixExist(namePrefix: String): Boolean =
        scriptHelper.isStdCommandWithPrefixExist(namePrefix)

    @JvmStatic
    fun findStdCommands(project: Project, commandName: String): Collection<AppleScriptCommand> =
        scriptHelper.findStdCommands(project, commandName)

    @JvmStatic
    fun findApplicationCommands(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand> = scriptHelper.findApplicationCommands(project, applicationName, commandName)

    @JvmStatic
    fun isStdProperty(name: String): Boolean = scriptHelper.isStdProperty(name)

    @JvmStatic
    fun isApplicationProperty(applicationName: String, propertyName: String): Boolean =
        scriptHelper.isApplicationProperty(applicationName, propertyName)

    @JvmStatic
    fun isStdPropertyWithPrefixExist(namePrefix: String): Boolean =
        scriptHelper.isStdPropertyWithPrefixExist(namePrefix)

    @JvmStatic
    fun isPropertyWithPrefixExist(applicationName: String, propertyNamePrefix: String): Boolean =
        scriptHelper.isPropertyWithPrefixExist(applicationName, propertyNamePrefix)

    @JvmStatic
    fun isStdConstant(name: String): Boolean = scriptHelper.isStdConstant(name)

    @JvmStatic
    fun isApplicationConstant(applicationName: String, constantName: String): Boolean =
        scriptHelper.isApplicationConstant(applicationName, constantName)

    @JvmStatic
    fun isStdConstantWithPrefixExist(namePrefix: String): Boolean =
        scriptHelper.isStdConstantWithPrefixExist(namePrefix)

    @JvmStatic
    fun isConstantWithPrefixExist(applicationName: String, constantNamePrefix: String): Boolean =
        scriptHelper.isConstantWithPrefixExist(applicationName, constantNamePrefix)

    @JvmStatic
    fun getScriptingAdditions(): HashSet<String> = scriptHelper.getScriptingAdditions()

    // D-01 / D-04 facade dispatchers — additive, no existing method touched (D-08 parser-util
    // contract preserved). The two new booleans live on the registry-service class (NOT the
    // ParsableScriptHelper interface), so these proxies go via getInstance() directly.

    @JvmStatic
    fun isInitialized(): Boolean =
        AppleScriptSystemDictionaryRegistryService.getInstance().isInitialized()

    @JvmStatic
    fun areAppDictionariesIndexed(): Boolean =
        AppleScriptSystemDictionaryRegistryService.getInstance().areAppDictionariesIndexed()

    /**
     * Phase 4 SERVICE-05 (Wave 5) / iteration-2 BLOCKER-fix proxy: lets [SdefIndexService] bound-wait
     * on the facade-owned `standardReady` Deferred WITHOUT importing the facade directly. Routing
     * through this static-utility class (NOT in the `verifyServiceDependencyGraph` services list)
     * avoids the SdefIndexService -> facade back-edge that DFS would detect as a cycle.
     *
     * NOT marked `@JvmStatic` (unlike the 26 frozen-contract methods above) because (a) the only
     * caller is Kotlin code in `SdefIndexService` — no Java parser-util call site exists — and (b)
     * `@JvmStatic suspend fun` emits a name-mangled JVM signature (the `-IoAF18A` suffix from
     * `Result<Unit>`'s inline-class boxing) that would force [ParserUtilContractTest] to enumerate
     * those mangled names. Keeping these proxies as plain object members preserves the contract
     * count at the original 26.
     */
    suspend fun awaitStandardReady(): Result<Unit> =
        AppleScriptSystemDictionaryRegistryService.getInstance().awaitStandardReadyInternal()

    /**
     * Phase 4 SERVICE-05 (Wave 5) / iteration-2 BLOCKER-fix proxy: same as [awaitStandardReady] but
     * for the `appsReady` Deferred. See [awaitStandardReady] for cycle-prevention rationale.
     */
    suspend fun awaitAppsReady(): Result<Unit> =
        AppleScriptSystemDictionaryRegistryService.getInstance().awaitAppsReadyInternal()
}
