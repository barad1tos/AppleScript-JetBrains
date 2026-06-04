package com.intellij.plugin.applescript.lang.dictionary.index

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.dictionary.xml.LegacyJdomParser
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import org.jdom.Document
import org.jdom.Element
import org.jdom.JDOMException
import org.jdom.Namespace
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private const val XINCLUDE_NAMESPACE_URI: String = "http" + "://www.w3.org/2003/XInclude"
private const val MAX_XINCLUDE_DEPTH: Int = 64

private data class ParsedSuiteElements(
    val classes: List<Element>,
    val valueTypes: List<Element>,
    val classExtensions: List<Element>,
    val commands: List<Element>,
    val recordTypes: List<Element>,
    val enumerations: List<Element>,
)

private fun parsedSuiteElements(suiteElement: Element): ParsedSuiteElements =
    ParsedSuiteElements(
        classes = suiteElement.getChildren("class").toList(),
        valueTypes = suiteElement.getChildren("value-type").toList(),
        classExtensions = suiteElement.getChildren("class-extension").toList(),
        commands = suiteElement.getChildren("command").toList(),
        recordTypes = suiteElement.getChildren("record-type").toList(),
        enumerations = suiteElement.getChildren("enumeration").toList(),
    )

internal class SdefIndexIngestor(
    private val indexStore: SdefIndexStore,
    private val log: Logger,
) {
    /**
     * Fills the internal structures with terms from an application dictionary file.
     *
     * Uses the legacy JDOM parser bridge, which is hardened against XXE expansion.
     *
     * @return true if the file was parsed successfully
     */
    fun parseDictionaryFile(
        xmlFile: File,
        applicationName: String,
    ): Boolean =
        parseDictionaryFile(
            xmlFile = xmlFile,
            applicationName = applicationName,
            visitedFiles = mutableSetOf(),
            includeDepth = 0,
        )

    private fun parseDictionaryFile(
        xmlFile: File,
        applicationName: String,
        visitedFiles: MutableSet<String>,
        includeDepth: Int,
    ): Boolean {
        var parsed = true
        if (visitedFiles.add(xmlFile.stablePath())) {
            try {
                val document: Document = LegacyJdomParser.build(xmlFile)
                val rootNode: Element = document.rootElement
                val suiteElements: List<Element> = rootNode.children.toList()

                if (ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY == applicationName) {
                    for (suiteElem in suiteElements) {
                        parseSuiteElementForApplication(suiteElem, applicationName, visitedFiles, includeDepth)
                        parseSuiteElementForScriptingAdditions(suiteElem, applicationName)
                    }
                } else {
                    for (suiteElem in suiteElements) {
                        parseSuiteElementForApplication(suiteElem, applicationName, visitedFiles, includeDepth)
                    }
                }
            } catch (e: JDOMException) {
                log.warn("Exception occurred while parsing dictionary file", e)
                parsed = false
            } catch (e: IOException) {
                log.warn("Exception occurred while parsing dictionary file", e)
                parsed = false
            }
        }
        return parsed
    }

    private fun parseSuiteElementForScriptingAdditions(
        suiteElem: Element,
        applicationName: String,
    ) {
        val elements = parsedSuiteElements(suiteElem)

        for (valType in elements.valueTypes) {
            parseClassElement(applicationName, valType)
        }

        for (classTag in elements.classes) {
            parseClassElement(applicationName, classTag)
            parseElementsForApplication(
                classTag.getChildren("property"),
                applicationName,
                indexStore.stdPropertyNameToDictionarySetMap,
            )
        }

        for (classTag in elements.classExtensions) {
            parseClassElement(applicationName, classTag)
            parseElementsForApplication(
                classTag.getChildren("property"),
                applicationName,
                indexStore.stdPropertyNameToDictionarySetMap,
            )
        }

        parseElementsForApplication(
            elements.commands,
            applicationName,
            indexStore.stdCommandNameToApplicationNameSetMap,
        )
        parseElementsForApplication(
            elements.recordTypes,
            applicationName,
            indexStore.stdRecordNameToApplicationNameSetMap,
        )

        for (recordTag in elements.recordTypes) {
            parseElementsForApplication(
                recordTag.getChildren("property"),
                applicationName,
                indexStore.stdPropertyNameToDictionarySetMap,
            )
        }

        parseElementsForApplication(
            elements.enumerations,
            applicationName,
            indexStore.stdEnumerationNameToApplicationNameSetMap,
        )

        for (enumerationTag in elements.enumerations) {
            parseElementsForApplication(
                enumerationTag.getChildren("enumerator"),
                applicationName,
                indexStore.stdEnumeratorConstantNameToApplicationNameListMap,
            )
        }
    }

    private fun parseSuiteElementForApplication(
        suiteElem: Element,
        applicationName: String,
        visitedFiles: MutableSet<String>,
        includeDepth: Int,
    ) {
        val elements = parsedSuiteElements(suiteElem)

        parseIncludesForApplication(suiteElem, applicationName, visitedFiles, includeDepth)

        for (valType in elements.valueTypes) {
            parseClassElement(applicationName, valType)
        }

        for (classTag in elements.classes) {
            parseClassElement(applicationName, classTag)
            parseHashElementsForApplication(
                classTag.getChildren("property"),
                applicationName,
                indexStore.applicationNameToPropertySetMap,
            )
        }

        for (classTag in elements.classExtensions) {
            parseClassElement(applicationName, classTag)
            parseHashElementsForApplication(
                classTag.getChildren("property"),
                applicationName,
                indexStore.applicationNameToPropertySetMap,
            )
        }

        parseHashElementsForApplication(
            elements.commands,
            applicationName,
            indexStore.applicationNameToCommandNameSetMap,
        )
        parseHashElementsForApplication(
            elements.recordTypes,
            applicationName,
            indexStore.applicationNameToRecordNameSetMap,
        )

        for (recordTag in elements.recordTypes) {
            parseHashElementsForApplication(
                recordTag.getChildren("property"),
                applicationName,
                indexStore.applicationNameToPropertySetMap,
            )
        }

        parseHashElementsForApplication(
            elements.enumerations,
            applicationName,
            indexStore.applicationNameToEnumerationNameSetMap,
        )

        for (enumerationTag in elements.enumerations) {
            parseHashElementsForApplication(
                enumerationTag.getChildren("enumerator"),
                applicationName,
                indexStore.applicationNameToEnumeratorConstantNameSetMap,
            )
        }
    }

    private fun parseIncludesForApplication(
        suiteElem: Element,
        applicationName: String,
        visitedFiles: MutableSet<String>,
        includeDepth: Int,
    ) {
        if (includeDepth >= MAX_XINCLUDE_DEPTH) {
            log.warn("Skipping SDEF XInclude chain deeper than $MAX_XINCLUDE_DEPTH for [$applicationName].")
            return
        }
        val xIncludeNs = Namespace.getNamespace(XINCLUDE_NAMESPACE_URI)
        val xiIncludes: List<Element> = suiteElem.getChildren("include", xIncludeNs).toList()
        for (include in xiIncludes) {
            val hrefIncl = include.getAttributeValue("href")?.replace("localhost", "") ?: continue
            val inclFile = File(hrefIncl)
            if (inclFile.exists()) {
                parseDictionaryFile(inclFile, applicationName, visitedFiles, includeDepth + 1)
            }
        }
    }

    private fun parseClassElement(
        applicationName: String,
        classElement: Element,
    ) {
        val className = classElement.getAttributeValue("name")
        val code = classElement.getAttributeValue("code")
        var pluralClassName = classElement.getAttributeValue("plural")
        if (className == null || code == null) return
        pluralClassName = if (!StringUtil.isEmpty(pluralClassName)) pluralClassName else "${className}s"

        updateObjectNameSetForApplication(
            className,
            applicationName,
            indexStore.applicationNameToClassNameSetMap,
        )
        updateObjectNameSetForApplication(
            pluralClassName,
            applicationName,
            indexStore.applicationNameToClassNamePluralSetMap,
        )
        if (ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY == applicationName) {
            updateApplicationNameSetFor(
                className,
                applicationName,
                indexStore.stdClassNameToApplicationNameSetMap,
            )
            updateApplicationNameSetFor(
                pluralClassName,
                applicationName,
                indexStore.stdClassNamePluralToApplicationNameSetMap,
            )
        }
    }
}

private fun File.stablePath(): String =
    try {
        canonicalPath
    } catch (_: IOException) {
        absolutePath
    }

private fun parseElementsForApplication(
    xmlElements: List<Element>,
    applicationName: String,
    objectTagNameToApplicationNameListMap: MutableMap<String, MutableSet<String>>,
) {
    for (applicationObjectTag in xmlElements) {
        parseSimpleElementForObject(
            applicationObjectTag,
            applicationName,
            objectTagNameToApplicationNameListMap,
        )
    }
}

private fun parseHashElementsForApplication(
    xmlElements: List<Element>,
    applicationName: String,
    objectTagNameToApplicationNameListMap: MutableMap<String, MutableSet<String>>,
) {
    for (applicationObjectTag in xmlElements) {
        hashSimpleElementForObject(
            applicationObjectTag,
            applicationName,
            objectTagNameToApplicationNameListMap,
        )
    }
}

private fun parseSimpleElementForObject(
    suiteObjectElement: Element,
    applicationName: String,
    objectNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>>,
) {
    val objectName = suiteObjectElement.getAttributeValue("name")
    val code = suiteObjectElement.getAttributeValue("code")
    if (objectName == null || code == null) return
    updateApplicationNameSetFor(objectName, applicationName, objectNameToApplicationNameSetMap)
}

private fun hashSimpleElementForObject(
    suiteObjectElement: Element,
    applicationName: String,
    objectNameToApplicationNameListMap: MutableMap<String, MutableSet<String>>,
) {
    val objectName = suiteObjectElement.getAttributeValue("name")
    val code = suiteObjectElement.getAttributeValue("code")
    if (objectName == null || code == null) return
    updateObjectNameSetForApplication(objectName, applicationName, objectNameToApplicationNameListMap)
}

private fun updateApplicationNameSetFor(
    applicationObjectName: String,
    applicationName: String,
    applicationNameSetMap: MutableMap<String, MutableSet<String>>,
) {
    if (StringUtil.isEmpty(applicationObjectName)) return
    applicationNameSetMap.compute(applicationObjectName) { _, existing ->
        (existing ?: ConcurrentHashMap.newKeySet()).also { it.add(applicationName) }
    }
}

private fun updateObjectNameSetForApplication(
    applicationObjectName: String,
    applicationName: String,
    applicationNameSetMap: MutableMap<String, MutableSet<String>>,
) {
    if (StringUtil.isEmpty(applicationName)) return
    applicationNameSetMap.compute(applicationName) { _, existing ->
        (existing ?: ConcurrentHashMap.newKeySet()).also { it.add(applicationObjectName) }
    }
}
