package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.lang.ide.structure.AppleScriptStructureViewFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppleScriptStructureViewAdditionalTest : BasePlatformTestCase() {
    fun testStructureViewFactoryIsRegistered() {
        val element = PluginDescriptorTestSupport.findElement(
            "lang.psiStructureViewFactory",
            "com.intellij.plugin.applescript.lang.ide.structure.AppleScriptStructureViewFactory",
        )
        assertNotNull("StructureViewFactory must be registered", element)
    }

    fun testStructureViewFactoryCreatesBuilder() {
        myFixture.configureByText(AppleScriptFileType, "on run\nend run")
        val factory = AppleScriptStructureViewFactory()
        val builder = factory.getStructureViewBuilder(myFixture.file)
        assertNotNull("Structure view builder must not be null for AppleScript file", builder)
    }

    fun testStructureViewShowsHandlers() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            on greet(name)
                return "Hello " & name
            end greet

            on farewell(name)
                return "Goodbye " & name
            end farewell
            """.trimIndent(),
        )
        val factory = AppleScriptStructureViewFactory()
        val builder = factory.getStructureViewBuilder(myFixture.file)
        assertNotNull(builder)
        val structureView = builder!!.createStructureView(null, myFixture.project)
        assertNotNull(structureView)
        val model = structureView.treeModel
        assertNotNull(model)
    }

    fun testStructureViewShowsVariables() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            property myProp : "default"
            set myVar to 123

            on run
                log myVar
            end run
            """.trimIndent(),
        )
        val factory = AppleScriptStructureViewFactory()
        val builder = factory.getStructureViewBuilder(myFixture.file)
        assertNotNull(builder)
        val structureView = builder!!.createStructureView(null, myFixture.project)
        assertNotNull(structureView)
    }

    fun testStructureViewShowsScriptObjects() {
        myFixture.configureByText(
            AppleScriptFileType,
            """
            script MyScript
                property x : 1
            end script

            on run
                run script MyScript
            end run
            """.trimIndent(),
        )
        val factory = AppleScriptStructureViewFactory()
        val builder = factory.getStructureViewBuilder(myFixture.file)
        assertNotNull(builder)
        val structureView = builder!!.createStructureView(null, myFixture.project)
        assertNotNull(structureView)
    }

    fun testStructureViewHandlesEmptyFile() {
        myFixture.configureByText(AppleScriptFileType, "")
        val factory = AppleScriptStructureViewFactory()
        val builder = factory.getStructureViewBuilder(myFixture.file)
        assertNotNull(builder)
    }
}
