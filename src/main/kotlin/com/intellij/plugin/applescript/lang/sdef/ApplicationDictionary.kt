package com.intellij.plugin.applescript.lang.sdef

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import java.io.File

/**
 * GROUP A (0 gen-implementer) SDEF interface, largest of the family — Phase 5 (v1.4) property
 * conversion (PSI-03). Extends `DictionarySuite` → `DictionaryComponent`.
 *
 * ONLY the pure no-arg getters convert to `val` properties (Java names preserved by property naming,
 * locked by `PsiGetterJvmSignatureTest`). Conversion caveats (NON-NEGOTIABLE):
 *  - [setRootTag] RETURNS `ApplicationDictionary` (not Unit) → cannot be a property setter; it stays
 *    `fun` while [rootTag] is the read-only `val` getter.
 *  - Every `find*` / `process*` / `add*` / arg-taking `get*` member stays `fun` (not a no-arg getter).
 *  - String constants stay `const val`; collection constants stay companion values.
 */
interface ApplicationDictionary :
    DictionarySuite,
    ApplicationDictionaryLookup {
    fun processInclude(includedFile: XmlFile): PsiFile

    fun addSuite(suite: Suite): Boolean

    /** JVM-visible as `getDictionaryFile()`. */
    val dictionaryFile: VirtualFile

    /** JVM-visible as `getApplicationBundle()`. */
    val applicationBundle: File?

    /** JVM-visible as `getDictionaryEnumerationMap()`. */
    val dictionaryEnumerationMap: Map<String, DictionaryEnumeration>

    /** JVM-visible as `getDictionaryEnumeratorMap()`. */
    val dictionaryEnumeratorMap: Map<String, DictionaryEnumerator>

    /** JVM-visible as `getDictionaryRecordMap()`. */
    val dictionaryRecordMap: Map<String, DictionaryRecord>

    /** JVM-visible as `getDictionaryCommandMap()`. */
    val dictionaryCommandMap: Map<String, AppleScriptCommand>

    /** JVM-visible as `getDictionaryClassMap()`. */
    val dictionaryClassMap: Map<String, AppleScriptClass>

    /** JVM-visible as `getApplicationName()`. */
    val applicationName: String

    /** JVM-visible as `getDictionaryPropertyMap()`. */
    val dictionaryPropertyMap: Map<String, AppleScriptPropertyDefinition>

    fun setRootTag(myRootTag: XmlTag): ApplicationDictionary

    /** JVM-visible as `getRootTag()`; paired mutator [setRootTag] stays `fun` (returns a value). */
    val rootTag: XmlTag?

    /** JVM-visible as `getAllCommands()`. */
    val allCommands: Collection<AppleScriptCommand>

    companion object {
        private const val XML_EXTENSION = "xml"
        private const val APPLICATION_BUNDLE_EXTENSION = "app"

        private const val SCRIPTING_ADDITION_EXTENSION = "osax"

        private const val SCRIPT_BUNDLE_EXTENSION = "scptd"

        private const val SCRIPTING_DEFINITION_EXTENSION = "sdef"

        private const val DICTIONARY_RESOURCE_FOLDER = "/$SCRIPTING_DEFINITION_EXTENSION"

        val SUPPORTED_DICTIONARY_EXTENSIONS: List<String> =
            listOf(
                XML_EXTENSION,
                APPLICATION_BUNDLE_EXTENSION,
                SCRIPTING_ADDITION_EXTENSION,
                SCRIPT_BUNDLE_EXTENSION,
                SCRIPTING_DEFINITION_EXTENSION,
            )

        val SUPPORTED_APPLICATION_EXTENSIONS: List<String> =
            listOf(
                APPLICATION_BUNDLE_EXTENSION,
                SCRIPTING_ADDITION_EXTENSION,
                SCRIPT_BUNDLE_EXTENSION,
            )

        const val COCOA_STANDARD_LIBRARY: String = "Standard Terminology"

        const val SCRIPTING_ADDITIONS_LIBRARY: String = "Scripting Additions"

        const val COCOA_STANDARD_LIBRARY_PATH: String =
            "/System/Library/ScriptingDefinitions/CocoaStandard.$SCRIPTING_DEFINITION_EXTENSION"

        val SCRIPTING_ADDITIONS_FOLDERS: Array<String> =
            arrayOf(
                "/System/Library/ScriptingAdditions/",
                "/Library/ScriptingAdditions/",
            )

        const val COCOA_STANDARD_FILE: String =
            "$DICTIONARY_RESOURCE_FOLDER/CocoaStandard.$SCRIPTING_DEFINITION_EXTENSION"

        const val STANDARD_ADDITIONS_FILE: String =
            "$DICTIONARY_RESOURCE_FOLDER/StandardAdditions.$SCRIPTING_DEFINITION_EXTENSION"

        val APP_BUNDLE_DIRECTORIES: Array<String> =
            arrayOf(
                "/Applications",
                // macOS Catalina (10.15) moved bundled apps (Music, TV, Podcasts,
                // App Store, Stocks, Books, Home, …) here. Without this entry the
                // dictionary registry hits notFoundApplicationList on every script
                // targeting them, even when the apps are installed and running.
                "/System/Applications",
                "/System/Applications/Utilities",
                "/System/Library/CoreServices",
                "/Library/ScriptingAdditions",
                // User-installed apps under ~/Applications. System.getProperty
                // resolves at class init time, which matches everything else
                // here being a static path.
                System.getProperty("user.home") + "/Applications",
            )
    }
}

interface ApplicationDictionaryLookup {
    fun findClass(name: String?): AppleScriptClass?

    fun getParameterNamesForCommand(name: String): List<String>?

    fun findEnumerator(name: String): DictionaryEnumerator?

    fun findEnumeration(name: String): DictionaryEnumeration?

    fun findProperty(name: String): AppleScriptPropertyDefinition?

    fun findCommand(name: String?): AppleScriptCommand?

    fun findSuiteByCode(suiteCode: String): Suite?

    fun findSuiteByName(suiteCode: String): Suite?

    fun findDirectParameterForCommand(commandName: String): CommandDirectParameter?

    fun findAllCommandsWithName(name: String): List<AppleScriptCommand>
}
