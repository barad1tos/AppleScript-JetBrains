package com.intellij.plugin.applescript.lang.ide.completion

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.plugin.applescript.lang.sdef.CommandParameter

class AppleScriptCompletionWeigher : CompletionWeigher() {

    override fun weigh(element: LookupElement, location: CompletionLocation): Int {
        val lookupObject = element.`object`
        val parameter = lookupObject as? CommandParameter
        return if (parameter != null && parameter.isOptional()) -10 else 0
    }
}
