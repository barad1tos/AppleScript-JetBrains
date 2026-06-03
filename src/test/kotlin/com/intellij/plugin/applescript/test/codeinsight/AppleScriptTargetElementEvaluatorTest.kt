package com.intellij.plugin.applescript.test.codeinsight

import com.intellij.plugin.applescript.lang.resolve.AppleScriptTargetElementEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test

class AppleScriptTargetElementEvaluatorTest {
    @Test
    fun `target element evaluator is registered in plugin descriptor`() {
        val evaluatorElement =
            requireNotNull(
                PluginDescriptorTestSupport.findElement(
                    tagName = "targetElementEvaluator",
                    implementationClass = AppleScriptTargetElementEvaluator::class.java.name,
                ),
            ) {
                "Target element evaluator registration must be present"
            }

        assertEquals("AppleScript", evaluatorElement.getAttribute("language"))
        assertEquals(
            AppleScriptTargetElementEvaluator::class.java.name,
            evaluatorElement.getAttribute("implementationClass"),
        )
    }
}
