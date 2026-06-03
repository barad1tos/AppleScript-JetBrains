package com.intellij.plugin.applescript.lang.ide

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.sdef.AccessType
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

/**
 * URL formatting + parsing utilities for the documentation overlay's PSI-element links.
 *
 * Link shapes mirror the Apple SDEF hierarchy:
 *  - class:      psi_element://dictionary:Finder.xml/suite:Finder Basics/class#icon family
 *  - dictionary: psi_element://dictionary#Finder.xml
 *  - suite:      psi_element://dictionary:Finder.xml/suite#Finder Basics
 */
object AppleScriptDocHelper {
    private const val URL_PREFIX_DICTIONARY = "dictionary#"
    private const val URL_PREFIX_SUITE = "suite#"
    private const val URL_PREFIX_CLASS = "class#"
    private const val URL_PREFIX_COMMAND = "command#"
    private const val TYPE_SEPARATOR = "/"
    private const val ELEMENT_NAME_SEPARATOR = ":"
    private const val ATTRIBUTE_INDENT_SIZE = 8

    @JvmStatic
    fun appendElementLink(
        sb: StringBuilder,
        psiElement: AppleScriptPsiElement,
        label: String?,
    ): String? {
        val elementRef =
            when (psiElement) {
                is AppleScriptClass ->
                    "dictionary:${psiElement.dictionary.getName()}$TYPE_SEPARATOR" +
                        "suite$ELEMENT_NAME_SEPARATOR${psiElement.suite.getName()}$TYPE_SEPARATOR" +
                        "$URL_PREFIX_CLASS${psiElement.getName()}"
                is ApplicationDictionary -> "$URL_PREFIX_DICTIONARY${psiElement.getName()}"
                is Suite ->
                    "dictionary:${psiElement.dictionary.getName()}$TYPE_SEPARATOR" +
                        "$URL_PREFIX_SUITE${psiElement.getName()}"
                is AppleScriptCommand ->
                    "dictionary:${psiElement.dictionary.getName()}$TYPE_SEPARATOR" +
                        "suite$ELEMENT_NAME_SEPARATOR${psiElement.suite.getName()}$TYPE_SEPARATOR" +
                        "$URL_PREFIX_COMMAND${psiElement.getName()}"
                else -> ""
            }
        if (StringUtil.isEmpty(elementRef)) return null

        val result = "<a href=\"${DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL}$elementRef\">$label</a>"
        sb.append(result)
        return result
    }

    @JvmStatic
    internal fun getDocumentationElementForLink(
        psiManager: PsiManager,
        link: String,
    ): PsiElement? {
        val target = DocumentationLinkTarget.parse(link)
        val dictionary = findDictionary(psiManager, target.dictionaryName)

        return when (target.typeName) {
            "class" -> findClass(dictionary, target)
            "command" -> findCommand(dictionary, target)
            "dictionary" -> dictionary
            "suite" -> dictionary?.findSuiteByName(target.targetName)
            else -> null
        }
    }

    @JvmStatic
    fun appendClassAttributes(
        sb: StringBuilder,
        dictionaryClass: AppleScriptClass,
    ) {
        val classElements: List<AppleScriptClass> = dictionaryClass.elements
        val classProperties: List<AppleScriptPropertyDefinition> = dictionaryClass.properties
        val indent = "&nbsp;".repeat(ATTRIBUTE_INDENT_SIZE)

        if (classElements.isNotEmpty()) {
            sb
                .append("<p>")
                .append(indent)
                .append("ELEMENTS <br>")
                .append(indent)
                .append("contains ")
            appendElementLinks(sb, classElements)
            sb.append(".</p>")
        }
        if (classProperties.isNotEmpty()) {
            sb.append("<p>").append(indent).append("PROPERTIES <br>")
            for (prop in classProperties) {
                sb.append(indent)
                appendClassProperty(sb, prop)
            }
            sb.append("</p>")
        }
        val commandsToRespond: List<AppleScriptCommand> = dictionaryClass.respondingCommands
        if (commandsToRespond.isNotEmpty()) {
            sb
                .append("<p>")
                .append(indent)
                .append("RESPONDS TO: <br>")
                .append(indent)
            appendElementLinks(sb, commandsToRespond)
        }
    }

    private fun findDictionary(
        psiManager: PsiManager,
        dictionaryName: String,
    ): ApplicationDictionary? {
        val dictionaryRegistry = psiManager.project.getService(AppleScriptProjectDictionaryService::class.java)
        // The link encodes the dictionary's display name, which may differ from the
        // registry key (application name). Primary lookup is by key; the fallback scans
        // dictionaries by getName(), so a name/key divergence still resolves.
        return dictionaryRegistry?.getDictionary(dictionaryName)
            ?: dictionaryRegistry?.getDictionaries()?.firstOrNull { it.getName() == dictionaryName }
    }

    private fun findClass(
        dictionary: ApplicationDictionary?,
        target: DocumentationLinkTarget,
    ): PsiElement? {
        val suite = dictionary?.findSuiteByName(target.suiteName)
        return suite?.findClassByCode(target.targetName) ?: dictionary?.findClass(target.targetName)
    }

    private fun findCommand(
        dictionary: ApplicationDictionary?,
        target: DocumentationLinkTarget,
    ): PsiElement? {
        val suite = dictionary?.findSuiteByName(target.suiteName)
        return suite?.findCommandByCode(target.targetName) ?: dictionary?.findCommand(target.targetName)
    }

    private fun appendElementLinks(
        sb: StringBuilder,
        elements: List<DictionaryComponent>,
    ) {
        elements.forEachIndexed { index, element ->
            if (index > 0) {
                sb.append(", ")
            }
            appendElementLink(sb, element, element.getName())
        }
    }

    private fun appendClassProperty(
        sb: StringBuilder,
        prop: AppleScriptPropertyDefinition,
    ) {
        val accessType =
            when (prop.accessType) {
                AccessType.R -> ", r/o"
                AccessType.W -> ", w/o"
                else -> ""
            }
        sb
            .append("<b>")
            .append(prop.getName())
            .append("</b> ")
            .append("(")
            .append(prop.typeSpecifier)
            .append(accessType)
            .append(") : ")
            .append(StringUtil.notNullize(prop.description))
            .append("<br>")
    }

    private data class DocumentationLinkTarget(
        val dictionaryName: String,
        val typeName: String,
        val targetName: String,
        val suiteName: String,
    ) {
        companion object {
            fun parse(link: String): DocumentationLinkTarget {
                val dictionaryEndIndex =
                    link.indexOf(TYPE_SEPARATOR).let { if (it > 0) it else link.length }
                val typeStartIndex = link.lastIndexOf(TYPE_SEPARATOR)
                val hashIndex = link.indexOf("#")
                val typeNameStartIndex =
                    if (typeStartIndex >= 0) {
                        typeStartIndex + TYPE_SEPARATOR.length
                    } else {
                        0
                    }
                val suiteName =
                    if (typeStartIndex > 0) {
                        val suiteStartIndex =
                            link.lastIndexOf(ELEMENT_NAME_SEPARATOR, typeStartIndex)
                        link.substring(suiteStartIndex + ELEMENT_NAME_SEPARATOR.length, typeStartIndex)
                    } else {
                        ""
                    }
                return DocumentationLinkTarget(
                    dictionaryName = link.substring("dictionary".length + 1, dictionaryEndIndex),
                    typeName = link.substring(typeNameStartIndex, hashIndex),
                    targetName = link.substring(hashIndex + 1),
                    suiteName = suiteName,
                )
            }
        }
    }
}
