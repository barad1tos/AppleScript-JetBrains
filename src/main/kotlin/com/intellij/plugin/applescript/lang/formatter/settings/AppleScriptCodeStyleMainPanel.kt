package com.intellij.plugin.applescript.lang.formatter.settings

import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.psi.codeStyle.CodeStyleSettings

class AppleScriptCodeStyleMainPanel internal constructor(
    currentSettings: CodeStyleSettings,
    settings: CodeStyleSettings,
) : TabbedLanguageCodeStylePanel(AppleScriptLanguage, currentSettings, settings)
