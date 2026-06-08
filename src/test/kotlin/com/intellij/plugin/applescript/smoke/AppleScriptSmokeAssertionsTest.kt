package com.intellij.plugin.applescript.smoke

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class AppleScriptSmokeAssertionsTest : BasePlatformTestCase() {
    fun testSmokeAssertionsPassForRunIdeFixtures() {
        val failures = mutableListOf<String>()

        AppleScriptSmokeAssertions().run(project, File("src/test/resources/testData/runIde"), failures)

        assertEmpty(failures)
    }
}
