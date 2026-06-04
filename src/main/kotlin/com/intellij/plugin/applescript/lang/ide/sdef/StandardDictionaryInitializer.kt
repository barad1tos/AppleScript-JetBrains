package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.files.SdefFileProvider
import com.intellij.plugin.applescript.lang.dictionary.files.stream2file
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.io.File
import java.io.IOException
import java.io.InputStream

internal class StandardDictionaryInitializer(
    private val dictionaryRegistry: AppleScriptSystemDictionaryRegistryService,
    private val fileProvider: SdefFileProvider = SdefFileProvider.getInstance(),
) {
    fun initialize() {
        try {
            if (SystemInfo.isMac) {
                initializeMacStandardSuite()
            } else {
                initializeBundledStandardSuite()
            }
        } catch (e: IOException) {
            LOG.error("Failed to initialize dictionary for standard terms", e)
        }
    }

    private fun initializeMacStandardSuite() {
        initializeScriptingAdditions()
        initializeCocoaStandardTerminology()
    }

    private fun initializeScriptingAdditions() {
        val dictionaryInfo =
            dictionaryRegistry.getDictionaryInfoByNameInternal(
                ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY,
            )
        if (dictionaryInfo == null) {
            fileProvider.initializeScriptingAdditions()
            fileProvider.mergeScriptingAdditions()
        } else {
            dictionaryRegistry.initializeDictionaryFromInfoInternal(dictionaryInfo)
        }
    }

    private fun initializeCocoaStandardTerminology() {
        val dictionaryInfo = findSavedCocoaStandardInfo()
        if (dictionaryInfo != null) {
            dictionaryRegistry.initializeDictionaryFromInfoInternal(dictionaryInfo)
            return
        }

        val standardLibraryFile = cocoaStandardDictionaryFile()
        if (standardLibraryFile.exists() && standardLibraryFile.isFile) {
            fileProvider.createAndInitializeInfo(
                standardLibraryFile,
                ApplicationDictionary.COCOA_STANDARD_LIBRARY,
            )
        } else {
            LOG.warn("Can not find standard suite dictionary in the classpath")
        }
    }

    private fun findSavedCocoaStandardInfo(): DictionaryInfo? {
        val dictionaryInfo =
            dictionaryRegistry.getDictionaryInfoByNameInternal(
                ApplicationDictionary.COCOA_STANDARD_LIBRARY,
            )
        return dictionaryInfo?.takeIf {
            it.initialized || dictionaryRegistry.initializeDictionaryFromInfoInternal(it)
        }
    }

    private fun cocoaStandardDictionaryFile(): File {
        val standardLibraryFile = File(ApplicationDictionary.COCOA_STANDARD_LIBRARY_PATH)
        if (standardLibraryFile.exists() && standardLibraryFile.isFile) return standardLibraryFile

        val standardDictionaryStream: InputStream? =
            javaClass.getResourceAsStream(ApplicationDictionary.COCOA_STANDARD_FILE)
        return stream2file(
            standardDictionaryStream,
            ApplicationDictionary.COCOA_STANDARD_LIBRARY.replace(" ", "_"),
            ".sdef",
        )
    }

    private fun initializeBundledStandardSuite() {
        fileProvider.initStdTerms(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
        fileProvider.initStdTerms(ApplicationDictionary.COCOA_STANDARD_LIBRARY)
    }

    private companion object {
        val LOG: Logger = Logger.getInstance("#${StandardDictionaryInitializer::class.java.name}")
    }
}
