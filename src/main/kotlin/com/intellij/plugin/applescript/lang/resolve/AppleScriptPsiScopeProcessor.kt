package com.intellij.plugin.applescript.lang.resolve

import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor

abstract class AppleScriptPsiScopeProcessor : PsiScopeProcessor {
    override fun execute(
        element: PsiElement,
        state: ResolveState,
    ): Boolean = element !is AppleScriptPsiElement || doExecute(element, state)

    protected abstract fun doExecute(
        element: AppleScriptPsiElement,
        state: ResolveState,
    ): Boolean
}
