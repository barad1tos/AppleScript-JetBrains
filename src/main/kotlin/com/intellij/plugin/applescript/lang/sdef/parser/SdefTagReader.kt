package com.intellij.plugin.applescript.lang.sdef.parser

import com.intellij.plugin.applescript.lang.sdef.AccessType
import com.intellij.plugin.applescript.lang.sdef.DictionaryPropertyData
import com.intellij.psi.xml.XmlTag

internal object SdefTagReader {
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

internal data class SdefTagIdentity(
    val name: String,
    val code: String,
)
