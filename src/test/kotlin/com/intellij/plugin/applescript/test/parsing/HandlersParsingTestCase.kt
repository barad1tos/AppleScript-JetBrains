package com.intellij.plugin.applescript.test.parsing

class HandlersParsingTestCase : AbstractParsingFixtureTestCase() {

    override fun getMyTargetDirectoryPath(): String = "handlers"

    fun testSimpleHandler() = doParseScriptInPackageTest("simple_handler")

    fun testHandlerDef() = doParseScriptInPackageTest("handler_def")

    fun testIfSamples() = doParseScriptInPackageTest("if_samples")

    fun testLeftAssociate() = doParseScriptInPackageTest("left_associate")

    fun testCoercionPrecedence() = doParseScriptInPackageTest("coercion_precedence")

    fun testHandlerInterleved() = doParseScriptInPackageTest("handler_interleved")

    fun testDirectParameters() = doParseScriptInPackageTest("direct_parameters")

    fun testAllInPackage() = doParseAllInPackageTest()
}
