package com.intellij.plugin.applescript

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object AppleScriptIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/applescript_file_icon.svg", AppleScriptIcons::class.java)

    @JvmField
    val OPEN_DICTIONARY: Icon = IconLoader.getIcon("/icons/dictionary_icon.png", AppleScriptIcons::class.java)
}
