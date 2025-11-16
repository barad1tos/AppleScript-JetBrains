package com.intellij.plugin.applescript.test;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class DictionaryResolutionTest extends BasePlatformTestCase {

    public void testPropertyEffectResolution() {
        String script = "tell application \"Microsoft PowerPoint\"\n" +
                        "  property effect\n" +
                        "end tell";

        myFixture.configureByText("test.applescript", script);
        // Should not show errors for 'property effect'
        myFixture.checkHighlighting(false, false, true, false);
    }
}
