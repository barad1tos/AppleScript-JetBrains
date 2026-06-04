package com.intellij.plugin.applescript.lang.dictionary.persistence

import com.intellij.util.xml.Required
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import java.io.File

/**
 * Information about the application and its generated dictionary file.
 */
class DictionaryInfo(
    private val applicationName: String,
    private val dictionaryFile: File,
    private val applicationFile: File?,
) {
    val state: State =
        State(
            applicationName,
            dictionaryFile.path,
            applicationFile?.path,
        )

    var initialized: Boolean = false
        private set

    fun getApplicationName(): String = applicationName

    fun getDictionaryFile(): File = dictionaryFile

    fun getApplicationFile(): File? = applicationFile

    fun setInitialized(initialized: Boolean): Boolean {
        this.initialized = initialized
        return initialized
    }

    @Tag("dictionary-info")
    class State {
        @JvmField
        @Attribute("applicationName")
        @Required
        var applicationName: String? = null

        @JvmField
        @OptionTag
        var dictionaryUrl: String? = null

        @JvmField
        @OptionTag
        var applicationUrl: String? = null

        constructor(
            applicationName: String,
            dictionaryUrl: String,
            applicationUrl: String?,
        ) {
            this.applicationName = applicationName
            this.dictionaryUrl = dictionaryUrl
            this.applicationUrl = applicationUrl
        }

        constructor()
    }
}
