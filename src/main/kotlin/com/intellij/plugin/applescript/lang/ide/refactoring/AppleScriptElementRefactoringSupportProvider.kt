package com.intellij.plugin.applescript.lang.ide.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptNamedElement
import com.intellij.psi.PsiElement

class AppleScriptElementRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isSafeDeleteAvailable(element: PsiElement): Boolean = element is AppleScriptNamedElement

    override fun isInplaceRenameAvailable(
        element: PsiElement,
        context: PsiElement?,
    ): Boolean = element !is AppleScriptHandler
}
