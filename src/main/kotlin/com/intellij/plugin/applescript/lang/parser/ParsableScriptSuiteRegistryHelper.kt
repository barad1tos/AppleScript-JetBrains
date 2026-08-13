package com.intellij.plugin.applescript.lang.parser

import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService

/** Parser boundary for lazily initializing a known application dictionary. */
object ParsableScriptSuiteRegistryHelper {
    private val registry: AppleScriptSystemDictionaryRegistryService
        get() = AppleScriptSystemDictionaryRegistryService.getInstance()

    fun ensureKnownApplicationInitialized(applicationName: String): Boolean =
        registry.ensureKnownApplicationDictionaryInitialized(applicationName)
}
