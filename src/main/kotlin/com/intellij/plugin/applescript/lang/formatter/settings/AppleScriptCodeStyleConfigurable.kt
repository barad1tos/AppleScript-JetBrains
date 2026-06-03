package com.intellij.plugin.applescript.lang.formatter.settings

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.psi.codeStyle.CodeStyleSettings

class AppleScriptCodeStyleConfigurable(
    settings: CodeStyleSettings,
    cloneSettings: CodeStyleSettings,
) : CodeStyleAbstractConfigurable(settings, cloneSettings, "Apple Script") {
    override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel = createMainPanel(settings)

    private fun createMainPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
        AppleScriptCodeStyleMainPanel(
            currentSettings,
            settings,
        )

    override fun getHelpTopic(): String? = null
}
