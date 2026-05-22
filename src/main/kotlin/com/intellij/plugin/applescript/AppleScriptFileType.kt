package com.intellij.plugin.applescript

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object AppleScriptFileType : LanguageFileType(AppleScriptLanguage) {

    override fun getName(): String = "AppleScript"

    override fun getDescription(): String = "AppleScript file"

    override fun getDefaultExtension(): String = "scpt"

    override fun getIcon(): Icon = AppleScriptIcons.FILE
}
