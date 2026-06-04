package com.intellij.plugin.applescript.lang.sdef.parser

import com.intellij.openapi.diagnostic.Logger
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.IncludedXmlTag

/**
 * Parses an SDEF XML file (`<dictionary>` root) into the [ApplicationDictionary] PSI model: suites
 * and their nested commands, classes, class extensions, value types, record types, and enumerations.
 *
 * Resolves `<xi:include>` directives against cached dictionary files to avoid the
 * IntelliJ "file accessed outside allowed roots" assertion.
 */
object SdefParser {
    private val LOG: Logger = Logger.getInstance("#${SdefParser::class.java.name}")

    @JvmStatic
    fun parse(
        file: XmlFile,
        parsedDictionary: ApplicationDictionary,
    ) {
        LOG.debug("Start parsing xml file --- $file ---")

        if (parsedDictionary.rootTag == null) {
            file.rootTag?.let { parsedDictionary.setRootTag(it) }
        }
        file.document?.rootTag?.let { rootTag ->
            applyDictionaryTitle(parsedDictionary, rootTag)
            parseRootTag(parsedDictionary, rootTag)
        }
        LOG.debug("parsing completed for file.")
    }

    @JvmStatic
    fun parseRootTag(
        parsedDictionary: ApplicationDictionary,
        rootTag: XmlTag,
    ) {
        SdefRootParser(parsedDictionary).parse(rootTag)
    }
}

private fun applyDictionaryTitle(
    parsedDictionary: ApplicationDictionary,
    rootTag: XmlTag,
) {
    val dictionaryTitle = rootTag.getAttributeValue(ATTRIBUTE_TITLE)
    if (rootTag.name == TAG_DICTIONARY && !dictionaryTitle.isNullOrEmpty()) {
        parsedDictionary.name = dictionaryTitle
    }
}

private class SdefRootParser(
    private val parsedDictionary: ApplicationDictionary,
) {
    private val includeProcessor = SdefIncludeProcessor(parsedDictionary.project)

    fun parse(rootTag: XmlTag) {
        val xIncludeNamespace = rootTag.getAttributeValue(ATTRIBUTE_XMLNS_XI)
        includeProcessor.process(parsedDictionary, rootTag.findIncludes(xIncludeNamespace))

        val rootSubTags = rootTag.subTags
        rootSubTags.forEach { suiteTag ->
            includeProcessor.process(parsedDictionary, suiteTag.findIncludes(xIncludeNamespace))
        }
        rootSubTags.forEach(::parseRootSubTag)
    }

    private fun parseRootSubTag(suiteTag: XmlTag) {
        when (suiteTag.name) {
            TAG_DICTIONARY if suiteTag is IncludedXmlTag -> processDictionaryInclude(suiteTag)
            TAG_SUITE -> parseSuiteTag(suiteTag)
        }
    }

    private fun processDictionaryInclude(includeTag: IncludedXmlTag) {
        includeProcessor
            .getDictionaryFileFromInclude(includeTag)
            ?.let(parsedDictionary::processInclude)
    }

    private fun parseSuiteTag(suiteTag: XmlTag) {
        val suite = SdefComponentParser.parseSuiteTag(suiteTag, parsedDictionary) ?: return

        suiteTag
            .findSubTags(TAG_COMMAND)
            .mapNotNull { SdefCommandTagParser.parse(it, suite) }
            .forEach { command ->
                parsedDictionary.addCommand(command)
                suite.addCommand(command)
            }

        suiteTag
            .findSubTags(TAG_CLASS)
            .mapNotNull { SdefComponentParser.parseClassTag(it, suite) }
            .forEach { appleScriptClass ->
                parsedDictionary.addClass(appleScriptClass)
                suite.addClass(appleScriptClass)
            }

        suiteTag
            .findSubTags(TAG_VALUE_TYPE)
            .mapNotNull { SdefComponentParser.parseClassTag(it, suite) }
            .forEach { simpleClass ->
                parsedDictionary.addClass(simpleClass)
                suite.addClass(simpleClass)
            }

        suiteTag
            .findSubTags(TAG_CLASS_EXTENSION)
            .mapNotNull { SdefComponentParser.parseClassExtensionTag(it, parsedDictionary, suite) }
            .forEach { appleScriptClass ->
                parsedDictionary.addClass(appleScriptClass)
                suite.addClass(appleScriptClass)
            }

        suiteTag
            .findSubTags(TAG_RECORD_TYPE)
            .mapNotNull { SdefComponentParser.parseRecordTag(it, suite) }
            .forEach { record ->
                parsedDictionary.addRecord(record)
                suite.addRecord(record)
            }

        suiteTag
            .findSubTags(TAG_ENUMERATION)
            .mapNotNull { SdefComponentParser.parseEnumerationTag(it, suite) }
            .forEach { enumeration ->
                parsedDictionary.addEnumeration(enumeration)
                suite.addEnumeration(enumeration)
            }
        // KEEP (Phase 8 / v2.0 backlog): the dual registration (components added directly to the
        // dictionary above AND to the suite) is a legacy of the Java port. Removing it changes
        // dictionary-population ordering on the frozen parser surface — a behavioural change
        // deferred to the grammar-hardening milestone.
        parsedDictionary.addSuite(suite)
    }
}
