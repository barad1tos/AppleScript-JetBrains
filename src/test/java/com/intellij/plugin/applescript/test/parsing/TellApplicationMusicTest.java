package com.intellij.plugin.applescript.test.parsing;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class TellApplicationMusicTest extends BasePlatformTestCase {

    public void testTellApplicationMusic() {
        String script = "tell application \"Music\"\n" +
                        "  property effect\n" +
                        "  get properties\n" +
                        "end tell";

        myFixture.configureByText("test.applescript", script);
        myFixture.checkHighlighting(false, false, true, false);
    }
}
