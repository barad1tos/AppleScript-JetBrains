package com.intellij.plugin.applescript.test.parsing

class DictionaryConstantParsingTestCase : AbstractParsingFixtureTestCase() {
    override fun getMyTargetDirectoryPath(): String = "dictionary_constant"

    // Phase 8 / PARSE-07: drifted golden fixture — grammar frozen in v1.x. Disabled via the
    // Platform's `shouldRunTest()` hook (the only green-keeping mechanism on these pure-JUnit3
    // BasePlatformTestCase classes; see HandlersParsingTestCase for the full rationale).
    override fun shouldRunTest(): Boolean = name !in DRIFTED_METHODS && super.shouldRunTest()

    fun testTellFinder() = doParseScriptInPackageTest("tell_finder")

    fun testTellApplication() = doParseScriptInPackageTest("tell_application")

    private companion object {
        val DRIFTED_METHODS = setOf("testTellFinder", "testTellApplication")
    }
}
