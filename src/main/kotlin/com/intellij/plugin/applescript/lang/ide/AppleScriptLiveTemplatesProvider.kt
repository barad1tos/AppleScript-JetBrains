@file:Suppress("DEPRECATION")

package com.intellij.plugin.applescript.lang.ide

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class AppleScriptLiveTemplatesProvider : DefaultLiveTemplatesProvider {

    override fun getDefaultLiveTemplateFiles(): Array<String> = TEMPLATE_FILES

    override fun getHiddenLiveTemplateFiles(): Array<String> = emptyArray()

    private companion object {
        private val TEMPLATE_FILES = arrayOf("liveTemplates/AppleScriptLiveTemplates")
    }
}
