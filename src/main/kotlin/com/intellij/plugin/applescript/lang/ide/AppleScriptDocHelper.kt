package com.intellij.plugin.applescript.lang.ide

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.sdef.AccessType
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
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

    @JvmStatic
    fun appendElementLink(sb: StringBuilder, psiElement: AppleScriptPsiElement, label: String?): String? {
        val elementRef = when (psiElement) {
            is AppleScriptClass -> "dictionary:${psiElement.getDictionary().getName()}$TYPE_SEPARATOR" +
                "suite$ELEMENT_NAME_SEPARATOR${psiElement.getSuite().getName()}$TYPE_SEPARATOR" +
                "$URL_PREFIX_CLASS${psiElement.getName()}"
            is ApplicationDictionary -> "$URL_PREFIX_DICTIONARY${psiElement.getName()}"
            is Suite -> "dictionary:${psiElement.getDictionary().getName()}$TYPE_SEPARATOR" +
                "$URL_PREFIX_SUITE${psiElement.getName()}"
            is AppleScriptCommand -> "dictionary:${psiElement.getDictionary().getName()}$TYPE_SEPARATOR" +
                "suite$ELEMENT_NAME_SEPARATOR${psiElement.getSuite().getName()}$TYPE_SEPARATOR" +
                "$URL_PREFIX_COMMAND${psiElement.getName()}"
            else -> ""
        }
        if (StringUtil.isEmpty(elementRef)) return null

        val result = "<a href=\"${DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL}$elementRef\">$label</a>"
        sb.append(result)
        return result
    }

    @JvmStatic
    internal fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement? {
        val dicNameIdxEnd = link.indexOf(TYPE_SEPARATOR).let { if (it > 0) it else link.length }
        val dictionaryName = link.substring("dictionary".length + 1, dicNameIdxEnd)
        val dictionaryRegistry = context.project.getService(AppleScriptProjectDictionaryService::class.java)
        var dictionary: ApplicationDictionary? = null
        if (dictionaryRegistry != null) {
            // TODO: dictionaryName != applicationName — confirm this is safe.
            dictionary = dictionaryRegistry.getDictionary(dictionaryName)
            if (dictionary == null) {
                for (dict in dictionaryRegistry.getDictionaries()) {
                    if (dict.getName() == dictionaryName) {
                        dictionary = dict
                        break
                    }
                }
            }
        }
        val typeIndexStart = link.lastIndexOf(TYPE_SEPARATOR)
        val hashIndex = link.indexOf("#")
        val typeName = link.substring(typeIndexStart + TYPE_SEPARATOR.length, hashIndex)
        val targetName = link.substring(hashIndex + 1)
        val suiteIdxStart = link.lastIndexOf(ELEMENT_NAME_SEPARATOR)

        return when (typeName) {
            "class" -> {
                val suiteName = link.substring(suiteIdxStart + ELEMENT_NAME_SEPARATOR.length, typeIndexStart)
                val suite = dictionary?.findSuiteByName(suiteName)
                suite?.findClassByCode(targetName) ?: dictionary?.findClass(targetName)
            }
            "command" -> {
                val suiteName = link.substring(suiteIdxStart + ELEMENT_NAME_SEPARATOR.length, typeIndexStart)
                val suite = dictionary?.findSuiteByName(suiteName)
                suite?.findCommandByCode(targetName) ?: dictionary?.findCommand(targetName)
            }
            "dictionary" -> dictionary
            "suite" -> dictionary?.findSuiteByName(targetName)
            else -> null
        }
    }

    @JvmStatic
    fun appendClassAttributes(sb: StringBuilder, dictionaryClass: AppleScriptClass) {
        val classElements: List<AppleScriptClass> = dictionaryClass.getElements()
        val classProperties: List<AppleScriptPropertyDefinition> = dictionaryClass.getProperties()
        val indent = "&nbsp;".repeat(8)

        if (classElements.isNotEmpty()) {
            sb.append("<p>").append(indent).append("ELEMENTS <br>").append(indent).append("contains ")
            val it = classElements.iterator()
            var aClass = it.next()
            appendElementLink(sb, aClass, aClass.getName())
            while (it.hasNext()) {
                aClass = it.next()
                sb.append(", ")
                appendElementLink(sb, aClass, aClass.getName())
            }
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
        val commandsToRespond: List<AppleScriptCommand> = dictionaryClass.getRespondingCommands()
        if (commandsToRespond.isNotEmpty()) {
            sb.append("<p>").append(indent).append("RESPONDS TO: <br>").append(indent)
            val it = commandsToRespond.iterator()
            var cmd = it.next()
            appendElementLink(sb, cmd, cmd.getName())
            while (it.hasNext()) {
                cmd = it.next()
                sb.append(", ")
                appendElementLink(sb, cmd, cmd.getName())
            }
        }
    }

    private fun appendClassProperty(sb: StringBuilder, prop: AppleScriptPropertyDefinition) {
        val accessType = if (prop.getAccessType() == AccessType.R) ", r/o" else ""
        sb.append("<b>").append(prop.getName()).append("</b> ")
            .append("(").append(prop.getTypeSpecifier()).append(accessType)
            .append(") : ").append(StringUtil.notNullize(prop.getDescription())).append("<br>")
    }
}
