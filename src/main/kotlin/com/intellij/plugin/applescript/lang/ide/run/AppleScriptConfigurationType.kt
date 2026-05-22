@file:Suppress("DEPRECATION")

package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.AppleScriptIcons

class AppleScriptConfigurationType : ConfigurationTypeBase(
    "AppleScriptRunType",
    "Run AppleScript",
    "Run Configuration for AppleScript",
    AppleScriptIcons.FILE,
) {

    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getId(): String = "AppleScript"

            override fun isConfigurationSingletonByDefault(): Boolean = true

            override fun canConfigurationBeSingleton(): Boolean = false

            override fun createTemplateConfiguration(project: Project): RunConfiguration =
                AppleScriptRunConfiguration(project, this, " Template config")
        })
    }
}
