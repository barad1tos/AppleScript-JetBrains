package com.intellij.plugin.applescript.smoke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class AppleScriptSmokeStarterTest {
    @Test
    fun `fixture root validation rejects missing property`() {
        val failure = AppleScriptSmokeStarter.fixtureRootFailure(null)

        assertEquals("-Dapplescript.smoke.fixtureRoot not set", failure)
    }

    @Test
    fun `fixture root validation rejects blank property`() {
        val failure = AppleScriptSmokeStarter.fixtureRootFailure(" ")

        assertEquals("-Dapplescript.smoke.fixtureRoot not set", failure)
    }

    @Test
    fun `fixture root validation rejects non-directory path`() {
        val missingFixtureRoot = Files.createTempDirectory("applescript-smoke").resolve("missing").toFile()

        val failure = AppleScriptSmokeStarter.fixtureRootFailure(missingFixtureRoot.path)

        assertEquals("fixture root is not a directory: ${missingFixtureRoot.path}", failure)
    }

    @Test
    fun `launch plan accepts existing fixture directory`() {
        val fixtureRoot = Files.createTempDirectory("applescript-smoke").toFile()

        val launchPlan = AppleScriptSmokeStarter.createLaunchPlan(fixtureRoot.path)

        assertNull(launchPlan.failureMessage())
        assertNotNull(launchPlan.requireFixtureDirectory())
        assertEquals(fixtureRoot.path, launchPlan.requireFixtureDirectory().path)
    }
}
