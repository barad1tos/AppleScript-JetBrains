package com.intellij.plugin.applescript.test.parsing

class ControlStmtParsingTestCase : AbstractParsingFixtureTestCase() {
    override fun getMyTargetDirectoryPath(): String = "handlers"

    fun testTryStatement() = doParseScriptInPackageTest("try_statement")

//  fun testAllInPackage() = doParseAllInPackageTest()
}
