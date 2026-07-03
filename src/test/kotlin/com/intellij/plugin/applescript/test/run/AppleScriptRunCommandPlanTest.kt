package com.intellij.plugin.applescript.test.run

import com.intellij.plugin.applescript.lang.ide.run.AppleScriptRunCommandPlan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppleScriptRunCommandPlanTest {
    @Test
    fun osascriptCommandLineStartsWithOsascriptPath() {
        val command = AppleScriptRunCommandPlan.osascriptCommandLine(null, null, null)
        assertEquals(listOf("/usr/bin/osascript", ""), command)
    }

    @Test
    fun osascriptCommandLineSplitsOptionsOnSpaces() {
        val command = AppleScriptRunCommandPlan.osascriptCommandLine("/tmp/s.applescript", "-s o", null)
        assertEquals(listOf("/usr/bin/osascript", "-s", "o", "/tmp/s.applescript"), command)
    }

    @Test
    fun osascriptCommandLineParsesQuotedAndBareParameters() {
        val command = AppleScriptRunCommandPlan.osascriptCommandLine("/tmp/s.applescript", null, "\"hello world\" foo")
        assertEquals(listOf("/usr/bin/osascript", "/tmp/s.applescript", "hello world", "foo"), command)
    }

    @Test
    fun osascriptCommandLinePassesEmptyPathThrough() {
        val command = AppleScriptRunCommandPlan.osascriptCommandLine("", "", "")
        assertEquals(listOf("/usr/bin/osascript", ""), command)
    }

    @Test
    fun osascriptCommandLineDropsEmptyQuotedParameter() {
        val command = AppleScriptRunCommandPlan.osascriptCommandLine("/tmp/s.applescript", null, "\"\" foo")
        assertEquals(listOf("/usr/bin/osascript", "/tmp/s.applescript", "foo"), command)
    }

    @Test
    fun defaultConfigurationNameTakesLastPathSegment() {
        assertEquals(
            "script.applescript",
            AppleScriptRunCommandPlan.defaultConfigurationName("/a/b/script.applescript"),
        )
    }

    @Test
    fun defaultConfigurationNameReturnsWholeStringWithoutSeparator() {
        assertEquals("bare.applescript", AppleScriptRunCommandPlan.defaultConfigurationName("bare.applescript"))
    }

    @Test
    fun defaultConfigurationNameIsEmptyForTrailingSeparator() {
        assertEquals("", AppleScriptRunCommandPlan.defaultConfigurationName("/a/b/"))
    }

    @Test
    fun appleEventDebugEnvironmentHasSendAndReceiveFlags() {
        assertEquals(
            mapOf("AEDebugSends" to "1", "AEDebugReceives" to "1"),
            AppleScriptRunCommandPlan.APPLE_EVENT_DEBUG_ENVIRONMENT,
        )
    }
}
