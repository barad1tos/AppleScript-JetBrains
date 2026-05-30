package com.intellij.plugin.applescript.test.parsing

class DictionaryConstantParsingTestCase : AbstractParsingFixtureTestCase() {

    override fun getMyTargetDirectoryPath(): String = "dictionary_constant"

    fun testTellFinder() = doParseScriptInPackageTest("tell_finder")

    fun testTellApplication() = doParseScriptInPackageTest("tell_application")
}
