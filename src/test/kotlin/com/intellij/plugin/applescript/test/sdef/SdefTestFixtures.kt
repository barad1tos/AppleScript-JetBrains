package com.intellij.plugin.applescript.test.sdef

import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.psi.sdef.impl.ApplicationDictionaryImpl
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile

internal const val EMPTY_TEST_DICTIONARY_XML = "<dictionary title=\"TestApp\"></dictionary>"

internal fun buildTestDictionary(
    project: Project,
    xmlText: String = EMPTY_TEST_DICTIONARY_XML,
    applicationName: String = "TestApp",
    fileName: String = "synthetic.sdef",
): ApplicationDictionaryImpl =
    ApplicationDictionaryImpl(
        project = project,
        dictionaryXmlFile = buildTestXmlFile(project, xmlText, fileName),
        applicationName = applicationName,
        applicationBundleFile = null,
    )

internal fun buildTestXmlFile(
    project: Project,
    xmlText: String,
    fileName: String = "synthetic.sdef",
): XmlFile =
    PsiFileFactory
        .getInstance(project)
        .createFileFromText(fileName, XMLLanguage.INSTANCE, xmlText) as XmlFile
