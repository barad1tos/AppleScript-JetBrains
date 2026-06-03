package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.plugin.applescript.lang.ide.completion.AppleScriptCompletionWeigher
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandImpl
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameterData
import com.intellij.plugin.applescript.lang.sdef.CommandParameterImpl
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.psi.xml.XmlTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class AppleScriptCompletionWeigherTest {
    @Test
    fun `optional command parameters receive lower weight than required parameters`() {
        val requiredParameterWeight = weigh(commandParameter("required", isOptional = false))
        val optionalParameterWeight = weigh(commandParameter("optional", isOptional = true))

        assertTrue(
            "Optional command parameters must receive lower weight than required command parameters",
            optionalParameterWeight < requiredParameterWeight,
        )
    }

    @Test
    fun `non command parameters stay neutral`() {
        val requiredParameterWeight = weigh(commandParameter("required", isOptional = false))
        val plainLookupWeight = weigh("plain lookup")

        assertEquals(requiredParameterWeight, plainLookupWeight)
    }

    @Test
    fun `completion weigher is registered in plugin descriptor`() {
        val weigherElement =
            requireNotNull(
                PluginDescriptorTestSupport.findElement(
                    tagName = "weigher",
                    implementationClass = AppleScriptCompletionWeigher::class.java.name,
                ),
            ) {
                "Completion weigher registration must be present"
            }

        assertEquals("completion", weigherElement.getAttribute("key"))
        assertEquals(
            "AppleScriptCommandParameterCompletionWeigher",
            weigherElement.getAttribute("id"),
        )
        assertEquals("after prefix", weigherElement.getAttribute("order"))
        assertEquals(
            AppleScriptCompletionWeigher::class.java.name,
            weigherElement.getAttribute("implementationClass"),
        )
    }

    private fun weigh(lookupObject: Any): Int {
        val lookupElement = LookupElementBuilder.create(lookupObject, "lookup")
        return AppleScriptCompletionWeigher().weigh(lookupElement, CompletionLocation(null))
    }

    private fun commandParameter(
        name: String,
        isOptional: Boolean,
    ): CommandParameter {
        val xmlTag = stubXmlTag()
        val command = AppleScriptCommandImpl(stubSuite(), "display dialog", "dlog", xmlTag)
        return CommandParameterImpl(
            command,
            CommandParameterData(name = name, code = "----", type = "text", optional = isOptional),
            xmlTag,
        )
    }

    private fun stubSuite(): Suite =
        Proxy.newProxyInstance(
            Suite::class.java.classLoader,
            arrayOf(Suite::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "isValid" -> true
                "toString" -> "SuiteStub@${System.identityHashCode(proxy)}"
                else -> null
            }
        } as Suite

    private fun stubXmlTag(): XmlTag =
        Proxy.newProxyInstance(
            XmlTag::class.java.classLoader,
            arrayOf(XmlTag::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "isValid" -> true
                "toString" -> "XmlTagStub@${System.identityHashCode(proxy)}"
                else -> null
            }
        } as XmlTag
}
