package com.intellij.plugin.applescript.lang.sdef

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import java.io.File

interface ApplicationDictionary : DictionarySuite {

    fun processInclude(includedFile: XmlFile): PsiFile?

    fun addSuite(suite: Suite): Boolean

    fun getDictionaryFile(): VirtualFile

    fun getApplicationBundle(): File?

    fun getDictionaryEnumerationMap(): Map<String, DictionaryEnumeration>

    fun getDictionaryEnumeratorMap(): Map<String, DictionaryEnumerator>

    fun getDictionaryRecordMap(): Map<String, DictionaryRecord>

    fun getDictionaryCommandMap(): Map<String, AppleScriptCommand>

    fun getDictionaryClassMap(): Map<String, AppleScriptClass>

    fun findClass(name: String?): AppleScriptClass?

    fun getParameterNamesForCommand(name: String): List<String>?

    fun getApplicationName(): String

    fun findEnumerator(name: String): DictionaryEnumerator?

    fun findEnumeration(name: String): DictionaryEnumeration?

    fun findProperty(name: String): AppleScriptPropertyDefinition?

    fun getDictionaryPropertyMap(): Map<String, AppleScriptPropertyDefinition>

    fun setRootTag(myRootTag: XmlTag): ApplicationDictionary

    fun getRootTag(): XmlTag

    fun getAllCommands(): Collection<AppleScriptCommand>

    fun findCommand(name: String?): AppleScriptCommand?

    fun findSuiteByCode(suiteCode: String): Suite?

    fun findSuiteByName(suiteCode: String): Suite?

    fun findDirectParameterForCommand(commandName: String): CommandDirectParameter?

    fun findAllCommandsWithName(name: String): List<AppleScriptCommand>

    companion object {
        @JvmField
        val SUPPORTED_DICTIONARY_EXTENSIONS: List<String> = listOf("xml", "app", "osax", "scptd", "sdef")

        @JvmField
        val SUPPORTED_APPLICATION_EXTENSIONS: List<String> = listOf("app", "osax", "scptd")

        @JvmField
        val COCOA_STANDARD_LIBRARY: String = "Standard Terminology"

        @JvmField
        val SCRIPTING_ADDITIONS_LIBRARY: String = "Scripting Additions"

        @JvmField
        val COCOA_STANDARD_LIBRARY_PATH: String =
            "/System/Library/ScriptingDefinitions/CocoaStandard.sdef"

        @JvmField
        val SCRIPTING_ADDITIONS_FOLDERS: Array<String> = arrayOf(
            "/System/Library/ScriptingAdditions/",
            "/Library/ScriptingAdditions/",
        )

        @JvmField
        val SDEF_FOLDER: String = "/sdef"

        @JvmField
        val COCOA_STANDARD_FILE: String = "$SDEF_FOLDER/CocoaStandard.sdef"

        @JvmField
        val STANDARD_ADDITIONS_FILE: String = "$SDEF_FOLDER/StandardAdditions.sdef"

        @JvmField
        val STANDARD_DEFINITION_FILES: Array<String> = arrayOf(COCOA_STANDARD_FILE, STANDARD_ADDITIONS_FILE)

        @JvmField
        val APP_BUNDLE_DIRECTORIES: Array<String> = arrayOf(
            "/Applications",
            "/System/Library/CoreServices",
            "/Library/ScriptingAdditions",
        )
    }
}
