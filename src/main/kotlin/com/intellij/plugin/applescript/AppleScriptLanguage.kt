package com.intellij.plugin.applescript

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import java.io.Serial

object AppleScriptLanguage : Language("AppleScript") {
    override fun getAssociatedFileType(): LanguageFileType = AppleScriptFileType

    override fun isCaseSensitive(): Boolean = false

    @Serial
    fun readResolve(): Any = AppleScriptLanguage
}
