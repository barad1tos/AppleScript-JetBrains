package com.intellij.plugin.applescript.lang.sdef.parser

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeration
import com.intellij.plugin.applescript.lang.sdef.DictionaryRecord
import com.intellij.plugin.applescript.lang.sdef.Suite
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
        LOG.debug { "Start parsing xml file --- $file ---" }

        if (parsedDictionary.rootTag == null) {
            file.rootTag?.let { parsedDictionary.setRootTag(it) }
        }
        val constructionResult =
            file.document?.rootTag?.let { rootTag ->
                applyDictionaryTitle(parsedDictionary, rootTag)
                parseRootTag(parsedDictionary, rootTag)
            } ?: SdefDictionaryConstructionResult()
        LOG.debug { "parsing completed for file. Direct-root construction result: $constructionResult" }
    }

    internal fun parseRootTag(
        parsedDictionary: ApplicationDictionary,
        rootTag: XmlTag,
    ): SdefDictionaryConstructionResult = SdefRootParser(parsedDictionary).parse(rootTag)
}

/**
 * Summary of component registration attempts while parsing a dictionary root tag.
 *
 * Parsing still mutates the target [ApplicationDictionary]; this value names direct suite-tag
 * construction so callers and tests can assert the parse seam without inspecting every downstream
 * map. The counts describe parser construction attempts and do not account for collection-level
 * duplicate rejection. XInclude processing can re-enter parsing through
 * [ApplicationDictionary.processInclude], but included dictionaries are not counted in this
 * direct-root summary.
 *
 * @property classRegistrationAttempts class-like tags passed through `addClass`, including
 * `<class>`, `<value-type>`, and `<class-extension>`.
 */
internal data class SdefDictionaryConstructionResult(
    val suiteRegistrationAttempts: Int = 0,
    val commandRegistrationAttempts: Int = 0,
    val classRegistrationAttempts: Int = 0,
    val recordRegistrationAttempts: Int = 0,
    val enumerationRegistrationAttempts: Int = 0,
) {
    operator fun plus(other: SdefDictionaryConstructionResult): SdefDictionaryConstructionResult =
        SdefDictionaryConstructionResult(
            suiteRegistrationAttempts = suiteRegistrationAttempts + other.suiteRegistrationAttempts,
            commandRegistrationAttempts = commandRegistrationAttempts + other.commandRegistrationAttempts,
            classRegistrationAttempts = classRegistrationAttempts + other.classRegistrationAttempts,
            recordRegistrationAttempts = recordRegistrationAttempts + other.recordRegistrationAttempts,
            enumerationRegistrationAttempts =
                enumerationRegistrationAttempts + other.enumerationRegistrationAttempts,
        )
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

    fun parse(rootTag: XmlTag): SdefDictionaryConstructionResult {
        val xIncludeNamespace = rootTag.getAttributeValue(ATTRIBUTE_XMLNS_XI)
        includeProcessor.process(parsedDictionary, rootTag.findIncludes(xIncludeNamespace))

        val rootSubTags = rootTag.subTags
        rootSubTags.forEach { suiteTag ->
            includeProcessor.process(parsedDictionary, suiteTag.findIncludes(xIncludeNamespace))
        }
        return rootSubTags.fold(SdefDictionaryConstructionResult()) { result, next ->
            result + parseRootSubTag(next)
        }
    }

    private fun parseRootSubTag(rootSubTag: XmlTag): SdefDictionaryConstructionResult =
        when (rootSubTag.name) {
            TAG_DICTIONARY -> parseDictionaryTag(rootSubTag)

            TAG_SUITE -> parseSuiteTag(rootSubTag)

            else -> SdefDictionaryConstructionResult()
        }

    private fun parseDictionaryTag(dictionaryTag: XmlTag): SdefDictionaryConstructionResult {
        if (dictionaryTag is IncludedXmlTag) {
            processDictionaryInclude(dictionaryTag)
        }
        return SdefDictionaryConstructionResult()
    }

    private fun processDictionaryInclude(includeTag: IncludedXmlTag) {
        includeProcessor
            .getDictionaryFileFromInclude(includeTag)
            ?.let(parsedDictionary::processInclude)
    }

    private fun parseSuiteTag(suiteTag: XmlTag): SdefDictionaryConstructionResult {
        val suite =
            SdefComponentParser.parseSuiteTag(suiteTag, parsedDictionary)
                ?: return SdefDictionaryConstructionResult()
        val construction = SdefSuiteConstruction(parsedDictionary, suite)

        suiteTag
            .findSubTags(TAG_COMMAND)
            .mapNotNull { SdefCommandTagParser.parse(it, suite) }
            .forEach(construction::registerCommand)

        suiteTag
            .findSubTags(TAG_CLASS)
            .mapNotNull { SdefComponentParser.parseClassTag(it, suite) }
            .forEach(construction::registerClass)

        suiteTag
            .findSubTags(TAG_VALUE_TYPE)
            .mapNotNull { SdefComponentParser.parseClassTag(it, suite) }
            .forEach(construction::registerClass)

        suiteTag
            .findSubTags(TAG_CLASS_EXTENSION)
            .mapNotNull { SdefComponentParser.parseClassExtensionTag(it, parsedDictionary, suite) }
            .forEach(construction::registerClass)

        suiteTag
            .findSubTags(TAG_RECORD_TYPE)
            .mapNotNull { SdefComponentParser.parseRecordTag(it, suite) }
            .forEach(construction::registerRecord)

        suiteTag
            .findSubTags(TAG_ENUMERATION)
            .mapNotNull { SdefComponentParser.parseEnumerationTag(it, suite) }
            .forEach(construction::registerEnumeration)
        // KEEP (Phase 8 / v2.0 backlog: BL-F1): the dual registration (components added directly to the
        // dictionary above AND to the suite) is a legacy of the Java port. Removing it changes
        // dictionary-population ordering on the frozen parser surface — a behavioural change
        // deferred to the grammar-hardening milestone.
        return construction.complete()
    }
}

private class SdefSuiteConstruction(
    private val parsedDictionary: ApplicationDictionary,
    private val suite: Suite,
) {
    private var commandRegistrationAttempts = 0
    private var classRegistrationAttempts = 0
    private var recordRegistrationAttempts = 0
    private var enumerationRegistrationAttempts = 0

    fun registerCommand(command: AppleScriptCommand) {
        parsedDictionary.addCommand(command)
        suite.addCommand(command)
        commandRegistrationAttempts++
    }

    fun registerClass(appleScriptClass: AppleScriptClass) {
        parsedDictionary.addClass(appleScriptClass)
        suite.addClass(appleScriptClass)
        classRegistrationAttempts++
    }

    fun registerRecord(record: DictionaryRecord) {
        parsedDictionary.addRecord(record)
        suite.addRecord(record)
        recordRegistrationAttempts++
    }

    fun registerEnumeration(enumeration: DictionaryEnumeration) {
        parsedDictionary.addEnumeration(enumeration)
        suite.addEnumeration(enumeration)
        enumerationRegistrationAttempts++
    }

    fun complete(): SdefDictionaryConstructionResult {
        parsedDictionary.addSuite(suite)
        return SdefDictionaryConstructionResult(
            suiteRegistrationAttempts = 1,
            commandRegistrationAttempts = commandRegistrationAttempts,
            classRegistrationAttempts = classRegistrationAttempts,
            recordRegistrationAttempts = recordRegistrationAttempts,
            enumerationRegistrationAttempts = enumerationRegistrationAttempts,
        )
    }
}
