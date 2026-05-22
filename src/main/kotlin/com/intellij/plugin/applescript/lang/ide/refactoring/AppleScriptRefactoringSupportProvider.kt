package com.intellij.plugin.applescript.lang.ide.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.psi.PsiElement

class AppleScriptRefactoringSupportProvider : RefactoringSupportProvider() {

    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean =
        element !is AppleScriptHandler
}
