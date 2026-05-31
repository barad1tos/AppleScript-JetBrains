package com.intellij.plugin.applescript.lang.resolve

import com.intellij.openapi.util.Key
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor

class AppleScriptComponentScopeProcessor(private val myResult: MutableSet<AppleScriptComponent>) :
    AppleScriptPsiScopeProcessor() {

    private val myCollectedTargets: MutableMap<String, AppleScriptTargetVariable> = HashMap()

    override fun doExecute(element: AppleScriptPsiElement, state: ResolveState): Boolean {
        if (element is AppleScriptTargetVariable) {
            val name = element.getName() ?: return true
            if (!myCollectedTargets.containsKey(name)) {
                myCollectedTargets[name] = element
                myResult.add(element)
                // Reached only in the else of !containsKey(name), so the map holds a value for `name`.
            } else if (element.containingFile !==
                requireNotNull(myCollectedTargets[name]) { "myCollectedTargets[$name] present: this branch implies containsKey(name)" }.containingFile
            ) {
                myResult.add(element) // should not happen if the file is the same
                // If a variable with the same name is already collected it must live in the same local scope
                // (file, handler, etc.).
                throw AssertionError("Elements are defined in different files:")
            }
        } else if (element is AppleScriptComponent && element !is DictionaryComponent) {
            myResult.add(element)
        }
        return true
    }

    override fun <T> getHint(hintKey: Key<T>): T? = null

    override fun handleEvent(event: PsiScopeProcessor.Event, associated: Any?) = Unit
}
