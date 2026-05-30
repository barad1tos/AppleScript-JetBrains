package com.intellij.plugin.applescript.test.parsing

class LiveSamplesParsingTestCase : AbstractParsingFixtureTestCase() {

    override fun getMyTargetDirectoryPath(): String = "live_samples"

    fun testMail() = doParseScriptInPackageTest("mail")

    fun testQuickTime() = doParseScriptInPackageTest("QuickTime")

    fun testAuthenticateDialog() = doParseScriptInPackageTest("authenticate_dialog")

    fun testNewEvent() = doParseScriptInPackageTest("new_event")

    fun testClassNameCase() = doParseScriptInPackageTest("class_name_case")

    fun testNativeClasses() = doParseScriptInPackageTest("native_classes")

//  fun testLiveSamplesPackage() = doParseAllInPackageTest()
}
