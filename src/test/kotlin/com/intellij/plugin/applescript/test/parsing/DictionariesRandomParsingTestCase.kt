package com.intellij.plugin.applescript.test.parsing

class DictionariesRandomParsingTestCase : AbstractParsingFixtureTestCase() {

    override fun getMyTargetDirectoryPath(): String = "dictionaries_random"

    fun testWrongDictionary() = doParseScriptInPackageTest("wrong_dictionary")

    fun testClsVsProperty() = doParseScriptInPackageTest("cls_vs_property")

    fun testClsVsDic() = doParseScriptInPackageTest("cls_vs_dic")

    fun testCoreServices() = doParseScriptInPackageTest("CoreServices")

    fun testDictionaryCommands() = doParseScriptInPackageTest("dictionary_commands")
}
