package com.intellij.plugin.applescript.lang.ide

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

class AppleScriptDocumentationProvider : AbstractDocumentationProvider() {
    override fun getQuickNavigateInfo(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? {
        if (element is DictionaryComponent) {
            return "${element.type} \"${element.getName()}\" [ ${element.dictionary.getName()} ]"
        }
        return null
    }

    override fun generateDoc(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? {
        val targetElement: PsiElement? =
            when {
                element is DictionaryComponent -> element
                else -> element.reference?.resolve()
            }
        if (targetElement is DictionaryComponent) {
            return targetElement.documentation
        }
        return null
    }

    override fun getDocumentationElementForLink(
        psiManager: PsiManager,
        link: String,
        context: PsiElement?,
    ): PsiElement? {
        if (context !is AppleScriptPsiElement) return null
        return AppleScriptDocHelper.getDocumentationElementForLink(psiManager, link)
    }
}
