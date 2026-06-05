package com.intellij.plugin.applescript.lang.parser

import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService

/**
 * Lifecycle/readiness boundary for parser and index code that must avoid a direct service cycle.
 * Dictionary term lookups live in focused `Dictionary*Registry` parser facades.
 */
object ParsableScriptSuiteRegistryHelper {
    private val registry: AppleScriptSystemDictionaryRegistryService
        get() = AppleScriptSystemDictionaryRegistryService.getInstance()

    fun ensureKnownApplicationInitialized(applicationName: String): Boolean =
        registry.ensureKnownApplicationDictionaryInitialized(applicationName)

    fun isInitialized(): Boolean = registry.isInitialized()

    fun areAppDictionariesIndexed(): Boolean = registry.areAppDictionariesIndexed()

    /**
     * Lets the dictionary index service bound-wait on the registry-owned `standardReady` gate
     * without importing the registry service directly and creating a service dependency cycle.
     */
    suspend fun awaitStandardReady(): Result<Unit> = registry.awaitStandardReadyInternal()

    /**
     * Same as [awaitStandardReady], but for the `appsReady` gate.
     */
    suspend fun awaitAppsReady(): Result<Unit> = registry.awaitAppsReadyInternal()
}
