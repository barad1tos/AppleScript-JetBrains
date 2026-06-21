package com.intellij.plugin.applescript.test.parsing

import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.PlatformTestUtil

class DictionariesRandomParsingTestCase : AbstractParsingFixtureTestCase() {
    override fun getMyTargetDirectoryPath(): String = "dictionaries_random"

    override fun beforeFixturesConfigured() {
        project
            .getService(AppleScriptProjectDictionaryService::class.java)
            .clearCachedDictionariesForTests()

        val registryService = AppleScriptSystemDictionaryRegistryService.getInstance()
        PlatformTestUtil.waitWithEventsDispatching(
            "Application dictionaries were not indexed",
            { registryService.areAppDictionariesIndexed() },
            10,
        )
    }

    // TODO(parser): re-enable after refreshing/fixing the dictionary-random PSI snapshot drift. (backlog: BL-A1)
    override fun shouldRunTest(): Boolean = name !in DRIFTED_METHODS && super.shouldRunTest()

    fun testWrongDictionary() = doParseScriptInPackageTest("wrong_dictionary")

    fun testClsVsProperty() = doParseScriptInPackageTest("cls_vs_property")

    fun testClsVsDic() = doParseScriptInPackageTest("cls_vs_dic")

    fun testCoreServices() = doParseScriptInPackageTest("CoreServices")

    fun testDictionaryCommands() = doParseScriptInPackageTest("dictionary_commands")

    private companion object {
        val DRIFTED_METHODS = setOf("testClsVsDic")
    }
}
