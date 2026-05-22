package com.intellij.plugin.applescript.lang.sdef.parser

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.ide.sdef.DictionaryInfo
import com.intellij.plugin.applescript.lang.sdef.AccessType
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandImpl
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameterImpl
import com.intellij.plugin.applescript.lang.sdef.CommandResult
import com.intellij.plugin.applescript.lang.sdef.DictionaryClass
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeration
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumerationImpl
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumerator
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeratorImpl
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

/**
 * Parses an SDEF XML file (`<dictionary>` root) into the [ApplicationDictionary] PSI model: suites
 * and their nested commands, classes, class extensions, value types, record types, and enumerations.
 *
 * Resolves `<xi:include>` directives against [AppleScriptSystemDictionaryRegistryService]'s
 * cached dictionary files to avoid the IntelliJ "file accessed outside allowed roots" assertion.
 */
object SDEF_Parser {

    private val LOG: Logger = Logger.getInstance("#${SDEF_Parser::class.java.name}")

    @JvmStatic
    fun parse(file: XmlFile, parsedDictionary: ApplicationDictionary) {
        println("Start parsing xml file --- $file ---")
        LOG.debug("Start parsing xml file --- $file ---")

        // The interface signature claims non-null but ApplicationDictionaryImpl returns the raw field
        // which is null until first set — mirrors the Java original verbatim until Phase 4f.
        @Suppress("SENSELESS_COMPARISON")
        if (parsedDictionary.getRootTag() == null) {
            file.rootTag?.let { parsedDictionary.setRootTag(it) }
        }
        val document = file.document
        if (document != null) {
            val rootTag = document.rootTag
            if (rootTag != null) {
                val attr = rootTag.getAttribute("title")
                if ("dictionary" == rootTag.name && attr != null) {
                    val dicTitle = attr.value
                    if (!StringUtil.isEmpty(dicTitle)) {
                        parsedDictionary.setName(dicTitle!!)
                    }
                }
                parseRootTag(parsedDictionary, rootTag)
            }
        }
        println("parsing completed for file.")
        LOG.debug("parsing completed for file.")
    }

    @JvmStatic
    fun parseRootTag(parsedDictionary: ApplicationDictionary, rootTag: XmlTag) {
        val xInclNs = rootTag.getAttributeValue("xmlns:xi")
        var includes = getIncludes(rootTag, xInclNs)
        processIncludes(parsedDictionary, includes)
        val rootSubTags = rootTag.subTags
        for (suiteTag in rootSubTags) {
            includes = getIncludes(suiteTag, xInclNs)
            processIncludes(parsedDictionary, includes)
        }
        for (suiteTag in rootSubTags) {
            if ("dictionary" == suiteTag.name && suiteTag is IncludedXmlTag) {
                val xmlFile = getDictionaryFileFromInclude(parsedDictionary.project, suiteTag)
                if (xmlFile != null) {
                    parsedDictionary.processInclude(xmlFile)
                }
            } else if ("suite" != suiteTag.name) {
                continue
            }

            rootTag.subTags[0].name
            val suite = parseSuiteTag(suiteTag, parsedDictionary) ?: continue

            for (commandTag in suiteTag.findSubTags("command")) {
                val command = parseCommandTag(commandTag, suite) ?: continue
                parsedDictionary.addCommand(command)
                suite.addCommand(command)
            }

            for (classTag in suiteTag.findSubTags("class")) {
                val appleScriptClass = parseClassTag(classTag, suite) ?: continue
                parsedDictionary.addClass(appleScriptClass)
                suite.addClass(appleScriptClass)
            }

            for (valueTypeTag in suiteTag.findSubTags("value-type")) {
                val simpleClass = parseClassTag(valueTypeTag, suite) ?: continue
                parsedDictionary.addClass(simpleClass)
                suite.addClass(simpleClass)
            }

            for (classExtensionTag in suiteTag.findSubTags("class-extension")) {
                val appleScriptClass = parseClassExtensionTag(classExtensionTag, parsedDictionary, suite)
                if (appleScriptClass != null) {
                    parsedDictionary.addClass(appleScriptClass)
                    suite.addClass(appleScriptClass)
                }
            }

            for (recordTag in suiteTag.findSubTags("record-type")) {
                val record = parseRecordTag(recordTag, suite) ?: continue
                parsedDictionary.addRecord(record)
                suite.addRecord(record)
            }

            for (enumerationTag in suiteTag.findSubTags("enumeration")) {
                val enumeration = parseEnumerationTag(enumerationTag, suite) ?: continue
                parsedDictionary.addEnumeration(enumeration)
                suite.addEnumeration(enumeration)
            }
            // TODO: remove adding the components directly to dictionary above (legacy of the Java port).
            parsedDictionary.addSuite(suite)
        }
    }

    private fun getDictionaryFileFromInclude(project: Project, xmlIncludeTag: IncludedXmlTag): XmlFile? {
        val origXmlElement: XmlTag? = xmlIncludeTag.original
        val origPsiFile = origXmlElement?.containingFile
        if (origPsiFile !is XmlFile) return null
        var xmlFile: XmlFile? = origPsiFile

        val dictionaryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        var vFile: VirtualFile? = origPsiFile.virtualFile ?: return xmlFile
        val dInfo = dictionaryService.getDictionaryInfoByApplicationPath(vFile!!.path)
        if (dInfo != null) {
            val ioFile = dInfo.getDictionaryFile()
            if (ioFile.exists()) {
                vFile = LocalFileSystem.getInstance().findFileByIoFile(ioFile)
                if (vFile == null || !vFile.isValid) return null

                val psiFile: PsiFile? = PsiManager.getInstance(project).findFile(vFile)
                xmlFile = psiFile as? XmlFile
            }
        }
        return xmlFile
    }

    private fun getIncludes(rootTag: XmlTag, xInclNs: String?): Array<XmlTag>? {
        if (xInclNs == null) return null
        return rootTag.findSubTags("include", xInclNs)
    }

    private fun processIncludes(parsedDictionary: ApplicationDictionary, includes: Array<XmlTag>?) {
        if (includes == null) return
        for (include in includes) {
            var hrefIncl = include.getAttributeValue("href")
            if (StringUtil.isEmpty(hrefIncl)) continue
            hrefIncl = hrefIncl!!.replace("file://localhost", "")
            val includedFile = File(hrefIncl)

            // An assertion ("File accessed outside allowed roots") may fire when an included dictionary
            // lives outside the project — try to reuse the already-generated cached file instead.
            val dictionarySystemRegistry = AppleScriptSystemDictionaryRegistryService.getInstance()
            var ioFile: File? = null
            val dInfo = dictionarySystemRegistry.getDictionaryInfoByApplicationPath(includedFile.path)
            if (dInfo != null) {
                ioFile = dInfo.getDictionaryFile()
            } else if (includedFile.isFile) {
                val rawName = includedFile.name
                val index = rawName.lastIndexOf('.')
                val fName = if (index < 0) rawName else rawName.substring(0, index)
                ioFile = dictionarySystemRegistry.getDictionaryFile(fName)
            }
            if (ioFile == null || !ioFile.exists()) ioFile = includedFile
            if (!ioFile.exists()) continue

            val vFile = LocalFileSystem.getInstance().findFileByIoFile(ioFile)
            if (vFile == null || !vFile.isValid) continue

            val psiFile = PsiManager.getInstance(parsedDictionary.project).findFile(vFile)
            val xmlFile = psiFile as? XmlFile ?: continue
            parsedDictionary.processInclude(xmlFile)
        }
    }

    private fun parseSuiteTag(suiteTag: XmlTag, dictionary: ApplicationDictionary): Suite? {
        // TODO: add all subtags to the suite here (legacy of the Java port).
        val name = suiteTag.getAttributeValue("name")
        val code = suiteTag.getAttributeValue("code")
        val description = suiteTag.getAttributeValue("description")
        val hiddenVal = suiteTag.getAttributeValue("hidden")
        if (name == null || code == null) return null
        return SuiteImpl(dictionary, code, name, "yes" == hiddenVal, description, suiteTag)
    }

    private fun parseClassExtensionTag(
        classExtensionTag: XmlTag,
        dictionary: ApplicationDictionary,
        suite: Suite,
    ): AppleScriptClass? {
        val parentClassName = classExtensionTag.getAttributeValue("extends")
        val parentClass = dictionary.findClass(parentClassName)
        var parentClassCode = parentClass?.getCode()
        val pluralName = classExtensionTag.getAttributeValue("plural")
        // TODO: parent class code could be NULL — would need to parse the included dictionary in that case.
        if (parentClassCode == null && parentClassName != null) {
            val l = parentClassName.length
            parentClassCode = parentClassName.substring(if (l >= 4) 4 else l - 1)
        }
        if (parentClassName == null || parentClassCode == null) return null

        val elementNames = initClassElements(classExtensionTag)
        val respondingCommands = initClassRespondingMessages(classExtensionTag)

        val classExtension: AppleScriptClass = DictionaryClass(
            suite, parentClassName, parentClassCode, classExtensionTag,
            null, elementNames, respondingCommands, pluralName,
        )
        classExtension.setDescription(classExtensionTag.getAttributeValue("description"))

        val propertyTags = classExtensionTag.findSubTags("property")
        classExtension.setProperties(getPropertiesFromTags(classExtension, propertyTags))
        return classExtension
    }

    private fun getPropertiesFromTags(
        classOrRecord: DictionaryComponent,
        propertyTags: Array<XmlTag>,
    ): List<AppleScriptPropertyDefinition> {
        if (classOrRecord !is AppleScriptClass && classOrRecord !is DictionaryRecord) {
            return emptyList()
        }
        val properties = ArrayList<AppleScriptPropertyDefinition>(propertyTags.size)
        for (propTag in propertyTags) {
            val pName = propTag.getAttributeValue("name")
            val pCode = propTag.getAttributeValue("code")
            val pDescription = propTag.getAttributeValue("description")
            var pType = propTag.getAttributeValue("type")
            if (StringUtil.isEmpty(pType)) {
                val tType = propTag.findFirstSubTag("type")
                pType = tType?.getAttributeValue("type")
            }
            val pAccessType = propTag.getAttributeValue("access")
            val accessType = if ("r" == pAccessType) AccessType.R else AccessType.RW
            if (pName != null && pCode != null && pType != null) {
                properties.add(
                    DictionaryPropertyImpl(classOrRecord, pName, pCode, pType, pDescription, propTag, accessType),
                )
            }
        }
        return properties
    }

    private fun parseEnumerationTag(enumerationTag: XmlTag, suite: Suite): DictionaryEnumeration? {
        val name = enumerationTag.getAttributeValue("name") ?: return null
        val code = enumerationTag.getAttributeValue("code") ?: return null

        val description = enumerationTag.getAttributeValue("description")
        val enumConstants = ArrayList<DictionaryEnumerator>()
        val enumeration: DictionaryEnumeration =
            DictionaryEnumerationImpl(suite, name, code, description, enumerationTag)
        for (enumTag in enumerationTag.findSubTags("enumerator")) {
            val eName = enumTag.getAttributeValue("name")
            val eCode = enumTag.getAttributeValue("code")
            val eDescription = enumTag.getAttributeValue("description")
            if (eName != null && eCode != null) {
                enumConstants.add(DictionaryEnumeratorImpl(enumeration, eName, eCode, eDescription, enumTag))
            }
        }
        enumeration.setEnumerators(enumConstants)
        return enumeration
    }

    private fun parseRecordTag(recordTag: XmlTag, suite: Suite): DictionaryRecord? {
        val name = recordTag.getAttributeValue("name") ?: return null
        val code = recordTag.getAttributeValue("code") ?: return null

        val description = recordTag.getAttributeValue("description")
        val propertyTags = recordTag.findSubTags("property")
        val record: DictionaryRecord = DictionaryRecordDefinition(suite, name, code, description, recordTag)
        record.setProperties(getPropertiesFromTags(record, propertyTags))
        return record
    }

    private fun parseClassTag(classTag: XmlTag, suite: Suite): AppleScriptClass? {
        val name = classTag.getAttributeValue("name") ?: return null
        val code = classTag.getAttributeValue("code") ?: return null
        val pluralName = classTag.getAttributeValue("plural")
        val parentClassName = classTag.getAttributeValue("inherits")
        val elementNames = initClassElements(classTag)
        val respondingCommands = initClassRespondingMessages(classTag)

        val aClass: AppleScriptClass = DictionaryClass(
            suite, name, code, classTag, parentClassName, elementNames, respondingCommands, pluralName,
        )
        aClass.setDescription(classTag.getAttributeValue("description"))

        val properties = ArrayList<AppleScriptPropertyDefinition>()
        for (propTag in classTag.findSubTags("property")) {
            val pName = propTag.getAttributeValue("name")
            val pCode = propTag.getAttributeValue("code")
            val pDescription = propTag.getAttributeValue("description")
            var pType = propTag.getAttributeValue("type")
            if (StringUtil.isEmpty(pType)) {
                val tType = propTag.findFirstSubTag("type")
                pType = tType?.getAttributeValue("type")
            }
            val pAccessType = propTag.getAttributeValue("access")
            val accessType = if ("r" == pAccessType) AccessType.R else AccessType.RW
            if (pName != null && pCode != null && pType != null) {
                properties.add(
                    DictionaryPropertyImpl(aClass, pName, pCode, pType, pDescription, propTag, accessType),
                )
            }
        }
        aClass.setProperties(properties)
        return aClass
    }

    private fun initClassRespondingMessages(classTag: XmlTag): List<String> {
        val commandNames = ArrayList<String>()
        for (elemTag in classTag.findSubTags("responds-to")) {
            elemTag.getAttributeValue("command")?.let { commandNames.add(it) }
        }
        return commandNames
    }

    private fun initClassElements(classTag: XmlTag): List<String> {
        val elementNames = ArrayList<String>()
        for (elemTag in classTag.findSubTags("element")) {
            elemTag.getAttributeValue("type")?.let { elementNames.add(it) }
        }
        return elementNames
    }

    private fun parseCommandTag(commandTag: XmlTag, suite: Suite): AppleScriptCommand? {
        val name = commandTag.getAttributeValue("name") ?: return null
        val code = commandTag.getAttributeValue("code") ?: return null
        val description = commandTag.getAttributeValue("description")
        val documentation = commandTag.getSubTagText("documentation")

        val command: AppleScriptCommand = AppleScriptCommandImpl(suite, name, code, commandTag)
        command.setDescription(description)
        command.setDictionaryDoc(documentation)

        commandTag.findFirstSubTag("result")?.let { resultTag ->
            val rType = resultTag.getAttributeValue("type")
            val rDesc = resultTag.getAttributeValue("description")
            if (rType != null) {
                command.setResult(CommandResult(rType, rDesc))
            }
        }

        var directParameter: CommandDirectParameter? = null
        val directParam = commandTag.findFirstSubTag("direct-parameter")
        if (directParam != null) {
            val typeAttr = directParam.getAttribute("type")
            val paramDescription = directParam.getAttribute("description")
            val isOptionalAttr = directParam.getAttribute("optional")
            val typeVal: String? = if (typeAttr != null) {
                typeAttr.value
            } else {
                // Could be expressed as a nested <type> sub-tag instead of an attribute.
                val typeTag = directParam.findFirstSubTag("type")
                if (typeTag != null) {
                    var nestedType = typeTag.getAttribute("type")
                    if (nestedType == null) {
                        val typeSubTag = typeTag.findFirstSubTag("type")
                        nestedType = typeSubTag?.getAttribute("type")
                    }
                    nestedType?.value
                } else null
            }
            val isOptional = isOptionalAttr != null && "yes" == isOptionalAttr.value
            if (typeVal != null) {
                directParameter = CommandDirectParameter(command, typeVal, paramDescription?.value, isOptional)
            }
        }

        val commandParameters = ArrayList<CommandParameter>()
        for (paramTag in commandTag.findSubTags("parameter")) {
            val pName = paramTag.getAttributeValue("name")
            val pCode = paramTag.getAttributeValue("code")
            val pDescription = paramTag.getAttributeValue("description")
            var pType = paramTag.getAttributeValue("type")
            if (pType == null) {
                val typeSubTag = paramTag.findFirstSubTag("type")
                if (typeSubTag != null) {
                    pType = typeSubTag.getAttributeValue("type")
                }
            }
            val pOptional = paramTag.getAttributeValue("optional")
            if (pName != null && pCode != null && pType != null) {
                val bOptional = "yes" == pOptional
                commandParameters.add(
                    CommandParameterImpl(command, pName, pCode, bOptional, pType, pDescription, paramTag),
                )
            }
        }
        command.setDirectParameter(directParameter)
        command.setParameters(commandParameters)
        return command
    }
}
