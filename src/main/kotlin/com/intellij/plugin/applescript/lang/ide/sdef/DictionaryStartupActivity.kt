package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Starts the application-level dictionary producer when a project enters post-startup. */
class DictionaryStartupActivity internal constructor(
    private val registryProvider: () -> AppleScriptSystemDictionaryRegistryService =
        AppleScriptSystemDictionaryRegistryService::getInstance,
) : ProjectActivity {
    override suspend fun execute(project: Project) {
        registryProvider()
    }
}
