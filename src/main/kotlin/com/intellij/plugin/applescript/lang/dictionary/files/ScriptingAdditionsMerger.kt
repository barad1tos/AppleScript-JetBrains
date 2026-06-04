package com.intellij.plugin.applescript.lang.dictionary.files

import com.intellij.openapi.diagnostic.Logger
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.xml.LegacyJdomParser
import com.intellij.plugin.applescript.lang.dictionary.xml.LegacyJdomWriter
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import org.jdom.Document
import org.jdom.Element
import org.jdom.JDOMException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private val LOG: Logger = Logger.getInstance("#${ScriptingAdditionsMerger::class.java.name}")

private const val TAG_SUITE = "suite"

internal object ScriptingAdditionsMerger {
    fun mergeAndInitialize(
        scriptingAdditions: Set<String>,
        initializeDictionary: (File, String) -> DictionaryInfo?,
    ): DictionaryInfo? {
        return try {
            val libName = ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
            val dictionaryFiles = scriptingAdditionsDictionaryFiles(scriptingAdditions)
            if (dictionaryFiles.isEmpty()) return null

            val mergedFile = File.createTempFile(libName, ".sdef")
            val firstDocument: Document = LegacyJdomParser.build(dictionaryFiles.first())
            val firstRoot: Element = firstDocument.rootElement
            dictionaryFiles.drop(1).forEach { second ->
                appendSuites(firstRoot, LegacyJdomParser.build(second).rootElement)
            }
            FileOutputStream(mergedFile).use { out ->
                LegacyJdomWriter.write(firstDocument, out)
                out.flush()
            }
            initializeDictionary(mergedFile, libName)
        } catch (e: JDOMException) {
            LOG.warn("Can not parse scripting additions file", e)
            null
        } catch (e: IOException) {
            LOG.warn("Can not merge scripting additions", e)
            null
        }
    }

    private fun scriptingAdditionsDictionaryFiles(scriptingAdditions: Set<String>): List<File> {
        val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
        return scriptingAdditions.mapNotNull { scriptingAddition ->
            facade
                .getDictionaryInfoByNameInternal(scriptingAddition)
                ?.getDictionaryFile()
                ?.let { dictionaryFile -> File(dictionaryFile.path) }
        }
    }

    private fun appendSuites(
        firstRoot: Element,
        secondRoot: Element,
    ) {
        secondRoot.getChildren(TAG_SUITE).forEach { originalSuite ->
            val suite = originalSuite.clone()
            suite.detach()
            firstRoot.addContent(suite)
        }
    }
}
