package com.intellij.plugin.applescript.lang.dictionary.xml

import com.intellij.openapi.util.JDOMUtil
import org.jdom.Document
import java.io.File

/** Reads a dictionary XML file into a JDOM [Document], the shape the SDEF ingest pipeline expects. */
internal object LegacyJdomParser {
    fun build(file: File): Document = Document(JDOMUtil.load(file.toPath()))
}
