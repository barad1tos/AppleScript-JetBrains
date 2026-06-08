package com.intellij.plugin.applescript.lang.ide

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

class AppleScriptDocumentationProvider : AbstractDocumentationProvider() {
    override fun getQuickNavigateInfo(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? =
        findDictionaryTarget(element)?.let { dictionaryTarget ->
            "${dictionaryTarget.type} \"${dictionaryTarget.getName()}\" [ ${dictionaryTarget.dictionary.getName()} ]"
        } ?: findLocalVariableTarget(element)?.let { variableTarget ->
            "variable \"${variableTarget.getName()}\""
        }

    override fun generateDoc(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? =
        findDictionaryTarget(element)?.documentation
            ?: findLocalVariableTarget(element)?.let(::localVariableDocumentation)

    override fun getDocumentationElementForLink(
        psiManager: PsiManager,
        link: String,
        context: PsiElement?,
    ): PsiElement? {
        if (context !is AppleScriptPsiElement) return null
        return AppleScriptDocHelper.getDocumentationElementForLink(psiManager, link)
    }

    private fun findDictionaryTarget(element: PsiElement): DictionaryComponent? {
        var candidate: PsiElement? = element
        while (candidate != null) {
            if (candidate is DictionaryComponent) return candidate
            val resolved = candidate.reference?.resolve()
            if (resolved is DictionaryComponent) return resolved
            candidate = candidate.parent
        }
        return null
    }

    private fun findLocalVariableTarget(element: PsiElement): AppleScriptComponent? {
        var candidate: PsiElement? = element
        while (candidate != null) {
            if (candidate is AppleScriptComponent && candidate.isVariable()) return candidate
            val resolved = candidate.reference?.resolve()
            if (resolved is AppleScriptComponent && resolved.isVariable()) return resolved
            candidate = candidate.parent
        }
        return null
    }

    private fun localVariableDocumentation(variableTarget: AppleScriptComponent): String =
        buildString {
            append("<b>Variable</b> ")
            append(variableTarget.getName())
        }
}
