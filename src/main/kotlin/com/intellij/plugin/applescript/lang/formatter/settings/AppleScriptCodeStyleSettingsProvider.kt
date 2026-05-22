@file:Suppress("DEPRECATION")

package com.intellij.plugin.applescript.lang.formatter.settings

import com.intellij.openapi.options.Configurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider

class AppleScriptCodeStyleSettingsProvider : CodeStyleSettingsProvider() {

    override fun getConfigurableDisplayName(): String = "AppleScript"

    override fun createSettingsPage(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): Configurable =
        AppleScriptCodeStyleConfigurable(settings, originalSettings)
}
