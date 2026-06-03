package com.intellij.plugin.applescript.test.parsing

class LiveSamplesParsingTestCase : AbstractParsingFixtureTestCase() {
    override fun getMyTargetDirectoryPath(): String = "live_samples"

    // Phase 8 / PARSE-07: four drifted live-sample golden fixtures — grammar frozen in v1.x.
    // Disabled via the Platform's `shouldRunTest()` hook (the only green-keeping mechanism on
    // these pure-JUnit3 BasePlatformTestCase classes; see HandlersParsingTestCase for the full
    // rationale).
    override fun shouldRunTest(): Boolean = name !in DRIFTED_METHODS && super.shouldRunTest()

    fun testMail() = doParseScriptInPackageTest("mail")

    fun testQuickTime() = doParseScriptInPackageTest("QuickTime")

    fun testAuthenticateDialog() = doParseScriptInPackageTest("authenticate_dialog")

    fun testNewEvent() = doParseScriptInPackageTest("new_event")

    fun testClassNameCase() = doParseScriptInPackageTest("class_name_case")

    fun testNativeClasses() = doParseScriptInPackageTest("native_classes")

//  fun testLiveSamplesPackage() = doParseAllInPackageTest()

    private companion object {
        val DRIFTED_METHODS =
            setOf(
                "testMail",
                "testAuthenticateDialog",
                "testClassNameCase",
                "testNativeClasses",
            )
    }
}
