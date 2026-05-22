package com.intellij.plugin.applescript.lang.ide.search

import com.intellij.plugin.applescript.psi.AppleScriptSelectorId
import com.intellij.plugin.applescript.psi.impl.AppleScriptHandlerInterleavedParameters
import com.intellij.pom.PomDeclarationSearcher
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiElement
import com.intellij.util.Consumer

class AppleScriptHandlerDeclarationSearcher : PomDeclarationSearcher() {

    override fun findDeclarationsAt(element: PsiElement, offsetInElement: Int, consumer: Consumer<in PomTarget>) {
        if (element is AppleScriptSelectorId) {
            val contextElement = element.context?.context
            if (contextElement is AppleScriptHandlerInterleavedParameters) {
                consumer.consume(contextElement)
            }
        }
    }
}
