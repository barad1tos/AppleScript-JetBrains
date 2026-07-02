package com.intellij.plugin.applescript.lang.formatter.settings

import com.intellij.lang.Language
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider

class AppleScriptCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
    override fun getLanguage(): Language = AppleScriptLanguage

    override fun getConfigurableDisplayName(): String = "AppleScript"

    override fun createConfigurable(
        settings: CodeStyleSettings,
        originalSettings: CodeStyleSettings,
    ): CodeStyleConfigurable = AppleScriptCodeStyleConfigurable(settings, originalSettings)
}
