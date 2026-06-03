@file:Suppress("DEPRECATION")

package com.intellij.plugin.applescript.lang.ide

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class AppleScriptLiveTemplatesProvider : DefaultLiveTemplatesProvider {
    override fun getDefaultLiveTemplateFiles(): Array<String> = arrayOf(LIVE_TEMPLATES_FILE)

    override fun getHiddenLiveTemplateFiles(): Array<String> = emptyArray()

    private companion object {
        private const val LIVE_TEMPLATES_FILE = "liveTemplates/AppleScriptLiveTemplates"
    }
}
