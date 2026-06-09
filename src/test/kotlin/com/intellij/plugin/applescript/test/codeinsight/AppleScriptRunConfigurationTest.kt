package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.plugin.applescript.lang.ide.run.AppleScriptConfigurationType
import com.intellij.plugin.applescript.lang.ide.run.AppleScriptRunConfiguration
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jdom.Element

class AppleScriptRunConfigurationTest : BasePlatformTestCase() {
    fun testConfigurationTypeIsRegistered() {
        val type = ConfigurationTypeUtil.findConfigurationType(AppleScriptConfigurationType::class.java)
        assertNotNull("AppleScriptConfigurationType must be registered", type)
        assertEquals("AppleScriptRunType", type.id)
        assertEquals("Run AppleScript", type.displayName)
    }

    fun testConfigurationTypeHasFactory() {
        val type = ConfigurationTypeUtil.findConfigurationType(AppleScriptConfigurationType::class.java)
        val factories = type.configurationFactories
        assertEquals("Must have exactly one factory", 1, factories.size)
        assertEquals("AppleScript", factories[0].id)
    }

    fun testFactoryCreatesTemplateConfiguration() {
        val type = ConfigurationTypeUtil.findConfigurationType(AppleScriptConfigurationType::class.java)
        val factory = type.configurationFactories.single()
        val template = factory.createTemplateConfiguration(project)
        assertTrue(template is AppleScriptRunConfiguration)
        assertNotNull(template.name)
    }

    fun testReadExternalSetsAllFields() {
        val type = ConfigurationTypeUtil.findConfigurationType(AppleScriptConfigurationType::class.java)
        val factory = type.configurationFactories.single()
        val config = AppleScriptRunConfiguration(project, factory, "TestConfig")

        val element = Element("configuration")
        element.setAttribute("scriptUrl", "/Users/test/script.applescript")
        element.setAttribute("scriptParameters", "arg1 arg2")
        element.setAttribute("scriptOptions", "-ss")
        element.setAttribute("logEvents", "true")

        config.readExternal(element)

        assertEquals("/Users/test/script.applescript", config.scriptPath)
        assertEquals("arg1 arg2", config.scriptParameters)
        assertEquals("-ss", config.scriptOptions)
        assertTrue(config.showAppleEvents)
    }

    fun testReadExternalHandlesMissingAttributes() {
        val type = ConfigurationTypeUtil.findConfigurationType(AppleScriptConfigurationType::class.java)
        val factory = type.configurationFactories.single()
        val config = AppleScriptRunConfiguration(project, factory, "TestConfig")

        val element = Element("configuration")
        config.readExternal(element)

        assertNull(config.scriptPath)
        assertNull(config.scriptParameters)
        assertNull(config.scriptOptions)
        assertFalse(config.showAppleEvents)
    }

    fun testWriteExternalWritesAllFields() {
        val type = ConfigurationTypeUtil.findConfigurationType(AppleScriptConfigurationType::class.java)
        val factory = type.configurationFactories.single()
        val config = AppleScriptRunConfiguration(project, factory, "TestConfig")

        config.scriptPath = "/test/script.applescript"
        config.scriptParameters = "param1"
        config.scriptOptions = "-s"
        config.showAppleEvents = true

        val element = Element("configuration")
        config.writeExternal(element)

        assertEquals("/test/script.applescript", element.getAttributeValue("scriptUrl"))
        assertEquals("param1", element.getAttributeValue("scriptParameters"))
        assertEquals("-s", element.getAttributeValue("scriptOptions"))
        assertEquals("true", element.getAttributeValue("logEvents"))
    }

    fun testWriteExternalSkipsNullFields() {
        val type = ConfigurationTypeUtil.findConfigurationType(AppleScriptConfigurationType::class.java)
        val factory = type.configurationFactories.single()
        val config = AppleScriptRunConfiguration(project, factory, "TestConfig")

        val element = Element("configuration")
        config.writeExternal(element)

        assertNull(element.getAttributeValue("scriptUrl"))
        assertNull(element.getAttributeValue("scriptParameters"))
        assertNull(element.getAttributeValue("scriptOptions"))
        assertEquals("false", element.getAttributeValue("logEvents"))
    }

    fun testSerializationRoundTrip() {
        val type = ConfigurationTypeUtil.findConfigurationType(AppleScriptConfigurationType::class.java)
        val factory = type.configurationFactories.single()

        val original = AppleScriptRunConfiguration(project, factory, "RoundTrip")
        original.scriptPath = "/test/path.applescript"
        original.scriptParameters = "hello world"
        original.scriptOptions = "-ss"
        original.showAppleEvents = true

        val element = Element("configuration")
        original.writeExternal(element)

        val restored = AppleScriptRunConfiguration(project, factory, "RoundTrip")
        restored.readExternal(element)

        assertEquals(original.scriptPath, restored.scriptPath)
        assertEquals(original.scriptParameters, restored.scriptParameters)
        assertEquals(original.scriptOptions, restored.scriptOptions)
        assertEquals(original.showAppleEvents, restored.showAppleEvents)
    }
}
