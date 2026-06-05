package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.dictionary.icons.DictionaryIconLoader
import com.intellij.plugin.applescript.lang.dictionary.index.DictionaryIndexes
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.lang.sdef.parser.SdefParser
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import java.io.File
import javax.swing.Icon

/**
 * PSI representation of one SDEF dictionary. Owns the suite registry plus per-name lookup maps for
 * commands / classes / records / properties / enumerations, eagerly initialised at construction by
 * parsing the bundled XmlFile via [SdefParser]. Bundle metadata (application name, icon) is read
 * from the .app's Info.plist + ICNS resource when available.
 *
 * Lookup, index mutation, suite lookup, and PSI identity behavior live in focused support layers so
 * this concrete class only owns XML-backed dictionary lifecycle.
 */
internal class ApplicationDictionaryImpl(
    project: Project,
    dictionaryXmlFile: XmlFile,
    override var applicationName: String,
    override val applicationBundleFile: File?,
) : ApplicationDictionaryNameSupport() {
    override val dictionaryProject: Project = project
    override val dictionaryVirtualFile: VirtualFile = dictionaryXmlFile.virtualFile
    override var dictionaryIcon: Icon? = null
    private val includedFiles: MutableList<PsiFile> = ArrayList()
    override var dictionaryName: String = ""
    override var dictionaryDocumentationText: String? = null

    override var dictionaryRootTag: XmlTag? = null
    override val suites: MutableList<Suite> = ArrayList()

    // Plan 02-05 / D-07: the 9-map index cluster moved into [DictionaryIndexes]
    // (CHM-backed). Closes the processInclude race latent since v1.0.0; Phase 1
    // explicitly deferred this fix here. v1.3 service split lifts `indexes`
    // wholesale into `SdefIndexService` without touching any consumer site.
    override val indexes: DictionaryIndexes = DictionaryIndexes()

    init {
        readDictionaryFromXmlFile(dictionaryXmlFile)
        dictionaryIcon = applicationBundleFile?.let { DictionaryIconLoader.loadFromBundle(it, applicationName) }
        if (StringUtil.isEmpty(dictionaryName)) {
            dictionaryName = applicationName
        }
        LOG.info(
            "Dictionary [$dictionaryName] for application [$applicationName] initialized " +
                "In project[${dictionaryProject.name}]  Commands: ${indexes.dictionaryCommandMap.size}. " +
                "Classes: ${indexes.dictionaryClassMap.size}",
        )
    }

    override fun processInclude(includedFile: XmlFile): PsiFile {
        if (!includedFile.isValid) {
            return includedFile
        }
        includedFile.document?.rootTag?.let { SdefParser.parseRootTag(this, it) }
        includedFiles.add(includedFile)
        LOG.debug("Processed included file: $includedFile")
        return includedFile
    }

    private fun readDictionaryFromXmlFile(xmlFile: XmlFile) {
        if (xmlFile.isValid) {
            xmlFile.rootTag?.let { setRootTag(it) }
            SdefParser.parse(xmlFile, this)
            LOG.debug("Dictionary loaded. Virtual file: $xmlFile")
        }
    }

    companion object {
        @JvmField
        val LOG: Logger = Logger.getInstance("#${ApplicationDictionaryImpl::class.java.name}")
    }
}
