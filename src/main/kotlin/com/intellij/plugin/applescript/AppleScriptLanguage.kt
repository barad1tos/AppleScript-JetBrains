package com.intellij.plugin.applescript

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType

object AppleScriptLanguage : Language("AppleScript") {
    override fun getAssociatedFileType(): LanguageFileType = AppleScriptFileType

    override fun isCaseSensitive(): Boolean = false

    @Suppress("unused")
    private fun readResolve(): Any = AppleScriptLanguage
}
