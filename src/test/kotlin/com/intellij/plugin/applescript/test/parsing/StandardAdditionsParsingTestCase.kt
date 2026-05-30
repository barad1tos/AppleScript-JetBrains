package com.intellij.plugin.applescript.test.parsing

class StandardAdditionsParsingTestCase : AbstractParsingFixtureTestCase() {

    override fun getMyTargetDirectoryPath(): String = "standard_additions"

    fun testScriptingAdditions() = doParseScriptInPackageTest("scripting_additions")

    fun testStandardAdditionsPackage() = doParseAllInPackageTest()
}
