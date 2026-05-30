package com.intellij.plugin.applescript.test.parsing

class TellParsingTestCase : AbstractParsingFixtureTestCase() {

    override fun getMyTargetDirectoryPath(): String = "tell"

    fun testLiveSamplesPackage() = doParseAllInPackageTest()
}
