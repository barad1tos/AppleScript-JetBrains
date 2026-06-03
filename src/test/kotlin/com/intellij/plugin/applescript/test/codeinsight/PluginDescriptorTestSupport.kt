package com.intellij.plugin.applescript.test.codeinsight

import org.w3c.dom.Element
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

internal object PluginDescriptorTestSupport {
    private val descriptorPath = Path.of("src/main/resources/META-INF/plugin.xml")

    fun findElement(
        tagName: String,
        implementationClass: String,
    ): Element? {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(descriptorPath.toFile())
        val elements = document.getElementsByTagName(tagName)

        return (0 until elements.length)
            .asSequence()
            .map { elements.item(it) as Element }
            .firstOrNull {
                it.getAttribute("implementationClass") == implementationClass ||
                    it.getAttribute("implementation") == implementationClass
            }
    }
}
