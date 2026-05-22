package com.intellij.plugin.applescript.lang.resolve

import com.intellij.openapi.util.Key
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.util.containers.SortedList

class AppleScriptResolveProcessor(private val myName: String) : AppleScriptPsiScopeProcessor() {

    private var myResult: AppleScriptComponent? = null

    private val myTargets: SortedList<AppleScriptTargetVariable> =
        SortedList { o1, o2 -> -(o1.textOffset - o2.textOffset) }

    fun getResult(): PsiElement? = myResult

    override fun <T> getHint(hintKey: Key<T>): T? = null

    override fun handleEvent(event: PsiScopeProcessor.Event, associated: Any?) = Unit

    override fun doExecute(element: AppleScriptPsiElement, state: ResolveState): Boolean {
        if (element is AppleScriptComponent && myName == element.getName()) {
            if (element is AppleScriptTargetVariable) {
                myTargets.add(element)
                // Set the closest of all variables added so far and keep walking.
                myResult = myTargets[0]
                return true
            }
            myResult = element
            return false
        }
        return true
    }
}
