package com.intellij.plugin.applescript.test

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Resolution check that `property effect` inside a `tell application "Microsoft PowerPoint"`
 * block does not surface a highlighting error.
 *
 * Left UNFILTERED / manual: its checkHighlighting is host-sensitive (resolution depends on
 * the bundled/installed PowerPoint dictionary), so it is not wired into any build.gradle.kts
 * matcher. Run on demand with -PincludeHeavyTests=true --tests "*DictionaryResolutionTest".
 */
class DictionaryResolutionTest : BasePlatformTestCase() {
    fun testPropertyEffectResolution() {
        val script =
            "tell application \"Microsoft PowerPoint\"\n" +
                "  property effect\n" +
                "end tell"

        myFixture.configureByText("test.applescript", script)
        // Should not show errors for 'property effect'
        myFixture.checkHighlighting(false, false, true, false)
    }
}
