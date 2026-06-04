package com.intellij.plugin.applescript.lang.sdef.parser

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.IncludedXmlTag
import java.io.File

private const val ATTRIBUTE_HREF = "href"
private const val FILE_URL_LOCALHOST_PREFIX = "file://localhost"
private const val TAG_INCLUDE = "include"

internal class SdefIncludeProcessor(
    private val project: Project,
) {
    fun process(
        parsedDictionary: ApplicationDictionary,
        includes: Array<XmlTag>?,
    ) {
        includes
            .orEmpty()
            .mapNotNull(::resolveIncludedXmlFile)
            .forEach(parsedDictionary::processInclude)
    }

    fun getDictionaryFileFromInclude(xmlIncludeTag: IncludedXmlTag): XmlFile? {
        var xmlFile = xmlIncludeTag.original?.containingFile as? XmlFile
        val originalPath = xmlFile?.virtualFile?.path
        val cachedDictionaryFile =
            originalPath
                ?.let(dictionaryRegistry::getDictionaryInfoByApplicationPath)
                ?.getDictionaryFile()

        if (cachedDictionaryFile?.exists() == true) {
            xmlFile = cachedDictionaryFile.toValidVirtualFile()?.let(::toXmlFile)
        }
        return xmlFile
    }

    private fun resolveIncludedXmlFile(include: XmlTag): XmlFile? {
        val includedFile =
            include
                .getAttributeValue(ATTRIBUTE_HREF)
                ?.takeUnless(String::isEmpty)
                ?.replace(FILE_URL_LOCALHOST_PREFIX, "")
                ?.let(::File)

        val ioFile = includedFile?.let(::resolveIncludedIoFile)
        return ioFile
            ?.takeIf(File::exists)
            ?.toValidVirtualFile()
            ?.let(::toXmlFile)
    }

    private fun resolveIncludedIoFile(includedFile: File): File {
        val registryFile =
            dictionaryRegistry
                .getDictionaryInfoByApplicationPath(includedFile.path)
                ?.getDictionaryFile()
                ?: includedFile.takeIf(File::isFile)?.let(::getDictionaryFileByBaseName)
        return registryFile?.takeIf(File::exists) ?: includedFile
    }

    private fun getDictionaryFileByBaseName(includedFile: File): File? {
        val rawName = includedFile.name
        val extensionIndex = rawName.lastIndexOf('.')
        val fileName = if (extensionIndex < 0) rawName else rawName.substring(0, extensionIndex)
        return dictionaryRegistry.getDictionaryFile(fileName)
    }

    private fun File.toValidVirtualFile(): VirtualFile? =
        LocalFileSystem
            .getInstance()
            .findFileByIoFile(this)
            ?.takeIf(VirtualFile::isValid)

    private fun toXmlFile(virtualFile: VirtualFile): XmlFile? {
        val psiFile: PsiFile? = PsiManager.getInstance(project).findFile(virtualFile)
        return psiFile as? XmlFile
    }

    private val dictionaryRegistry: AppleScriptSystemDictionaryRegistryService
        get() = AppleScriptSystemDictionaryRegistryService.getInstance()
}

internal fun XmlTag.findIncludes(namespace: String?): Array<XmlTag>? = namespace?.let { findSubTags(TAG_INCLUDE, it) }
