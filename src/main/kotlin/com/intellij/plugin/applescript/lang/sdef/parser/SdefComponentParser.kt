package com.intellij.plugin.applescript.lang.sdef.parser

import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.ClassDefinition
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
import com.intellij.psi.xml.XmlTag

internal object SdefComponentParser {
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
