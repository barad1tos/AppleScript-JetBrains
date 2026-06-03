package com.intellij.plugin.applescript.lang.sdef.parser

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.sdef.AccessType
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandImpl
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.ClassDefinition
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameterData
import com.intellij.plugin.applescript.lang.sdef.CommandParameterImpl
import com.intellij.plugin.applescript.lang.sdef.CommandResult
import com.intellij.plugin.applescript.lang.sdef.DictionaryClass
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeration
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumerationImpl
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumerator
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeratorImpl
import com.intellij.plugin.applescript.lang.sdef.DictionaryPropertyData
import com.intellij.plugin.applescript.lang.sdef.DictionaryPropertyImpl
import com.intellij.plugin.applescript.lang.sdef.DictionaryRecord
import com.intellij.plugin.applescript.lang.sdef.DictionaryRecordDefinition
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.lang.sdef.SuiteImpl
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.IncludedXmlTag
import java.io.File

private const val ATTRIBUTE_CODE = "code"
private const val ATTRIBUTE_ACCESS = "access"
private const val ATTRIBUTE_DESCRIPTION = "description"
private const val ATTRIBUTE_EXTENDS = "extends"
private const val ATTRIBUTE_HIDDEN = "hidden"
private const val ATTRIBUTE_HREF = "href"
private const val ATTRIBUTE_INHERITS = "inherits"
private const val ATTRIBUTE_NAME = "name"
private const val ATTRIBUTE_OPTIONAL = "optional"
private const val ATTRIBUTE_PLURAL = "plural"
private const val ATTRIBUTE_TITLE = "title"
private const val ATTRIBUTE_TYPE = "type"
private const val ATTRIBUTE_XMLNS_XI = "xmlns:xi"
private const val CLASS_EXTENSION_FALLBACK_CODE_LENGTH = 4
private const val FILE_URL_LOCALHOST_PREFIX = "file://localhost"
private const val READ_ONLY_ACCESS = "r"
private const val WRITE_ONLY_ACCESS = "w"
private const val TAG_CLASS = "class"
private const val TAG_CLASS_EXTENSION = "class-extension"
private const val TAG_COMMAND = "command"
private const val TAG_DICTIONARY = "dictionary"
private const val TAG_DIRECT_PARAMETER = "direct-parameter"
private const val TAG_DOCUMENTATION = "documentation"
private const val TAG_ELEMENT = "element"
private const val TAG_ENUMERATION = "enumeration"
private const val TAG_ENUMERATOR = "enumerator"
private const val TAG_INCLUDE = "include"
private const val TAG_PARAMETER = "parameter"
private const val TAG_PROPERTY = "property"
private const val TAG_RECORD_TYPE = "record-type"
private const val TAG_RESPONDS_TO = "responds-to"
private const val TAG_RESULT = "result"
private const val TAG_SUITE = "suite"
private const val TAG_TYPE = "type"
private const val TAG_VALUE_TYPE = "value-type"
private const val YES_VALUE = "yes"

/**
 * Parses an SDEF XML file (`<dictionary>` root) into the [ApplicationDictionary] PSI model: suites
 * and their nested commands, classes, class extensions, value types, record types, and enumerations.
 *
 * Resolves `<xi:include>` directives against [AppleScriptSystemDictionaryRegistryService]'s
 * cached dictionary files to avoid the IntelliJ "file accessed outside allowed roots" assertion.
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

private class SdefIncludeProcessor(
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

private fun XmlTag.findIncludes(namespace: String?): Array<XmlTag>? = namespace?.let { findSubTags(TAG_INCLUDE, it) }

private object SdefComponentParser {
    fun parseSuiteTag(
        suiteTag: XmlTag,
        dictionary: ApplicationDictionary,
    ): Suite? {
        // KEEP (Phase 8 / v2.0 backlog): consolidating subtag attachment into the suite here
        // (instead of the caller wiring them) is a legacy-of-the-Java-port reshape of the
        // frozen parser surface. Deferred to the grammar-hardening milestone.
        val identity = SdefTagReader.readRequiredIdentity(suiteTag) ?: return null
        val description = suiteTag.getAttributeValue(ATTRIBUTE_DESCRIPTION)
        val isHidden = YES_VALUE == suiteTag.getAttributeValue(ATTRIBUTE_HIDDEN)
        return SuiteImpl(dictionary, identity.code, identity.name, isHidden, description, suiteTag)
    }

    fun parseClassExtensionTag(
        classExtensionTag: XmlTag,
        dictionary: ApplicationDictionary,
        suite: Suite,
    ): AppleScriptClass? {
        val parentClassName = classExtensionTag.getAttributeValue(ATTRIBUTE_EXTENDS)
        val parentClassCode = resolveClassExtensionParentCode(parentClassName, dictionary)
        if (parentClassName == null || parentClassCode == null) return null

        val classExtension: AppleScriptClass =
            DictionaryClass(
                suite,
                buildClassDefinition(
                    name = parentClassName,
                    code = parentClassCode,
                    classTag = classExtensionTag,
                    parentClassName = null,
                ),
                classExtensionTag,
            )
        classExtension.properties =
            SdefPropertyParser.parseProperties(
                classExtension,
                classExtensionTag.findSubTags(TAG_PROPERTY),
            )
        return classExtension
    }

    fun parseEnumerationTag(
        enumerationTag: XmlTag,
        suite: Suite,
    ): DictionaryEnumeration? {
        val identity = SdefTagReader.readRequiredIdentity(enumerationTag) ?: return null

        val enumeration: DictionaryEnumeration =
            DictionaryEnumerationImpl(
                suite,
                identity.name,
                identity.code,
                enumerationTag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
                enumerationTag,
            )
        val enumConstants =
            enumerationTag
                .findSubTags(TAG_ENUMERATOR)
                .mapNotNullTo(ArrayList()) { enumTag ->
                    parseEnumeratorTag(enumeration, enumTag)
                }
        enumeration.setEnumerators(enumConstants)
        return enumeration
    }

    fun parseRecordTag(
        recordTag: XmlTag,
        suite: Suite,
    ): DictionaryRecord? {
        val identity = SdefTagReader.readRequiredIdentity(recordTag) ?: return null

        val record: DictionaryRecord =
            DictionaryRecordDefinition(
                suite,
                identity.name,
                identity.code,
                recordTag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
                recordTag,
            )
        record.setProperties(SdefPropertyParser.parseProperties(record, recordTag.findSubTags(TAG_PROPERTY)))
        return record
    }

    fun parseClassTag(
        classTag: XmlTag,
        suite: Suite,
    ): AppleScriptClass? {
        val identity = SdefTagReader.readRequiredIdentity(classTag) ?: return null
        val aClass: AppleScriptClass =
            DictionaryClass(
                suite,
                buildClassDefinition(
                    name = identity.name,
                    code = identity.code,
                    classTag = classTag,
                    parentClassName = classTag.getAttributeValue(ATTRIBUTE_INHERITS),
                ),
                classTag,
            )
        aClass.properties = SdefPropertyParser.parseProperties(aClass, classTag.findSubTags(TAG_PROPERTY))
        return aClass
    }

    private fun buildClassDefinition(
        name: String,
        code: String,
        classTag: XmlTag,
        parentClassName: String?,
    ): ClassDefinition =
        ClassDefinition(
            name = name,
            code = code,
            description = classTag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
            parentClassName = parentClassName,
            pluralClassName = readPluralClassName(classTag, name),
            elementNames = initClassElements(classTag),
            respondingCommandNames = initClassRespondingMessages(classTag),
            properties = emptyList(),
        )

    private fun readPluralClassName(
        classTag: XmlTag,
        className: String,
    ): String =
        classTag
            .getAttributeValue(ATTRIBUTE_PLURAL)
            ?.takeUnless(String::isEmpty)
            ?: "${className}s"

    private fun parseEnumeratorTag(
        enumeration: DictionaryEnumeration,
        enumTag: XmlTag,
    ): DictionaryEnumerator? {
        val identity = SdefTagReader.readRequiredIdentity(enumTag) ?: return null
        return DictionaryEnumeratorImpl(
            enumeration,
            identity.name,
            identity.code,
            enumTag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
            enumTag,
        )
    }

    private fun resolveClassExtensionParentCode(
        parentClassName: String?,
        dictionary: ApplicationDictionary,
    ): String? {
        val parentClassCode = dictionary.findClass(parentClassName)?.code
        return parentClassCode ?: parentClassName?.let(::fallbackClassExtensionCode)
    }

    private fun fallbackClassExtensionCode(parentClassName: String): String {
        val length = parentClassName.length
        val startIndex =
            if (length >= CLASS_EXTENSION_FALLBACK_CODE_LENGTH) {
                CLASS_EXTENSION_FALLBACK_CODE_LENGTH
            } else {
                length - 1
            }
        return parentClassName.substring(startIndex)
    }

    private fun initClassRespondingMessages(classTag: XmlTag): List<String> {
        val commandNames = ArrayList<String>()
        for (elemTag in classTag.findSubTags(TAG_RESPONDS_TO)) {
            elemTag.getAttributeValue(TAG_COMMAND)?.let { commandNames.add(it) }
        }
        return commandNames
    }

    private fun initClassElements(classTag: XmlTag): List<String> {
        val elementNames = ArrayList<String>()
        for (elemTag in classTag.findSubTags(TAG_ELEMENT)) {
            elemTag.getAttributeValue(ATTRIBUTE_TYPE)?.let { elementNames.add(it) }
        }
        return elementNames
    }
}

private object SdefPropertyParser {
    fun parseProperties(
        classOrRecord: DictionaryComponent,
        propertyTags: Array<XmlTag>,
    ): List<AppleScriptPropertyDefinition> =
        if (classOrRecord is AppleScriptClass || classOrRecord is DictionaryRecord) {
            propertyTags.mapNotNullTo(ArrayList(propertyTags.size)) { propTag ->
                parseProperty(classOrRecord, propTag)
            }
        } else {
            emptyList()
        }

    private fun parseProperty(
        classOrRecord: DictionaryComponent,
        propTag: XmlTag,
    ): AppleScriptPropertyDefinition? {
        val propertyData = SdefTagReader.readPropertyAttributes(propTag) ?: return null
        return DictionaryPropertyImpl(classOrRecord, propertyData, propTag)
    }
}

private object SdefCommandTagParser {
    fun parse(
        commandTag: XmlTag,
        suite: Suite,
    ): AppleScriptCommand? {
        val identity = SdefTagReader.readRequiredIdentity(commandTag) ?: return null
        val command: AppleScriptCommand = AppleScriptCommandImpl(suite, identity.name, identity.code, commandTag)
        command.description = commandTag.getAttributeValue(ATTRIBUTE_DESCRIPTION)
        command.setDictionaryDoc(commandTag.getSubTagText(TAG_DOCUMENTATION))
        commandTag.findFirstSubTag(TAG_RESULT)?.let(::parseResult)?.let(command::setResult)
        command.directParameter = parseDirectParameter(command, commandTag.findFirstSubTag(TAG_DIRECT_PARAMETER))
        command.parameters = parseParameters(command, commandTag.findSubTags(TAG_PARAMETER))
        return command
    }

    private fun parseResult(resultTag: XmlTag): CommandResult? {
        val resultType = resultTag.getAttributeValue(ATTRIBUTE_TYPE)
        return resultType?.let { CommandResult(it, resultTag.getAttributeValue(ATTRIBUTE_DESCRIPTION)) }
    }

    private fun parseDirectParameter(
        command: AppleScriptCommand,
        directParam: XmlTag?,
    ): CommandDirectParameter? =
        directParam?.let { tag ->
            SdefTagReader.readDirectParameterType(tag)?.let { type ->
                CommandDirectParameter(
                    command,
                    type,
                    tag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
                    YES_VALUE == tag.getAttributeValue(ATTRIBUTE_OPTIONAL),
                )
            }
        }

    private fun parseParameters(
        command: AppleScriptCommand,
        parameterTags: Array<XmlTag>,
    ): List<CommandParameter> =
        parameterTags.mapNotNullTo(ArrayList(parameterTags.size)) { paramTag ->
            parseParameter(command, paramTag)
        }

    private fun parseParameter(
        command: AppleScriptCommand,
        paramTag: XmlTag,
    ): CommandParameter? {
        val identity = SdefTagReader.readRequiredIdentity(paramTag)
        val parameterType = SdefTagReader.readParameterType(paramTag)
        return if (identity != null && parameterType != null) {
            CommandParameterImpl(
                command,
                CommandParameterData(
                    name = identity.name,
                    code = identity.code,
                    type = parameterType,
                    optional = YES_VALUE == paramTag.getAttributeValue(ATTRIBUTE_OPTIONAL),
                    description = paramTag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
                ),
                paramTag,
            )
        } else {
            null
        }
    }
}

private object SdefTagReader {
    fun readRequiredIdentity(tag: XmlTag): SdefTagIdentity? {
        val name = tag.getAttributeValue(ATTRIBUTE_NAME)
        val code = tag.getAttributeValue(ATTRIBUTE_CODE)
        return if (name != null && code != null) SdefTagIdentity(name, code) else null
    }

    fun readPropertyAttributes(propTag: XmlTag): DictionaryPropertyData? {
        val identity = readRequiredIdentity(propTag)
        val propertyType = readParameterType(propTag)
        return if (identity != null && propertyType != null) {
            DictionaryPropertyData(
                name = identity.name,
                code = identity.code,
                typeSpecifier = propertyType,
                description = propTag.getAttributeValue(ATTRIBUTE_DESCRIPTION),
                accessType = readPropertyAccessType(propTag),
            )
        } else {
            null
        }
    }

    private fun readPropertyAccessType(propTag: XmlTag): AccessType =
        when (propTag.getAttributeValue(ATTRIBUTE_ACCESS)) {
            READ_ONLY_ACCESS -> AccessType.R
            WRITE_ONLY_ACCESS -> AccessType.W
            else -> AccessType.RW
        }

    fun readDirectParameterType(directParam: XmlTag): String? =
        directParam.getAttributeValue(ATTRIBUTE_TYPE)
            ?: directParam.findFirstSubTag(TAG_TYPE)?.let(::readNestedTypeValue)

    fun readParameterType(paramTag: XmlTag): String? =
        paramTag.getAttributeValue(ATTRIBUTE_TYPE)
            ?: paramTag.findFirstSubTag(TAG_TYPE)?.getAttributeValue(ATTRIBUTE_TYPE)

    private fun readNestedTypeValue(typeTag: XmlTag): String? =
        typeTag.getAttributeValue(ATTRIBUTE_TYPE)
            ?: typeTag.findFirstSubTag(TAG_TYPE)?.getAttributeValue(ATTRIBUTE_TYPE)
}

private data class SdefTagIdentity(
    val name: String,
    val code: String,
)
