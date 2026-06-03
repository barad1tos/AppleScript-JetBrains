package com.intellij.plugin.applescript.lang.ide.completion

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.plugin.applescript.lang.sdef.CommandParameter

private const val NEUTRAL_COMPLETION_WEIGHT = 0
private const val OPTIONAL_PARAMETER_COMPLETION_WEIGHT = -10

class AppleScriptCompletionWeigher : CompletionWeigher() {
    override fun weigh(
        element: LookupElement,
        location: CompletionLocation,
    ): Int {
        val parameter = element.`object` as? CommandParameter
        return if (parameter?.isOptional == true) {
            OPTIONAL_PARAMETER_COMPLETION_WEIGHT
        } else {
            NEUTRAL_COMPLETION_WEIGHT
        }
    }
}
