package com.intellij.plugin.applescript.lang.dictionary.xml

import com.intellij.openapi.util.JDOMUtil
import org.jdom.Document
import java.io.OutputStream

/** Writes a merged JDOM [Document] back to a stream for the scripting-additions dictionary cache. */
internal object LegacyJdomWriter {
    fun write(
        document: Document,
        output: OutputStream,
    ) = JDOMUtil.write(document, output)
}
