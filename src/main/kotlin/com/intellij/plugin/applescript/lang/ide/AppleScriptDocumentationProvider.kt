package com.intellij.plugin.applescript.lang.ide

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptDirectParameterDeclaration
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationPart
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.plugin.applescript.psi.AppleScriptSimpleFormalParameter
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

class AppleScriptDocumentationProvider : AbstractDocumentationProvider() {
    override fun getQuickNavigateInfo(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? =
        findDictionaryTarget(element)?.let { dictionaryTarget ->
            "${dictionaryTarget.type} \"${dictionaryTarget.getName()}\" [ ${dictionaryTarget.dictionary.getName()} ]"
        } ?: findLocalDataSymbolTarget(element)?.let { localTarget ->
            "${localTarget.localSymbolKind()} \"${localTarget.getName()}\""
        }

    override fun generateDoc(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? =
        findDictionaryTarget(element)?.documentation
            ?: findLocalDataSymbolTarget(element)?.let(::localSymbolDocumentation)

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

    private fun findLocalDataSymbolTarget(element: PsiElement): AppleScriptComponent? {
        var candidate: PsiElement? = element
        while (candidate != null) {
            if (candidate is AppleScriptComponent && candidate.isLocalDataSymbol()) return candidate
            val resolved = candidate.reference?.resolve()
            if (resolved is AppleScriptComponent && resolved.isLocalDataSymbol()) return resolved
            candidate = candidate.parent
        }
        return null
    }

    private fun localSymbolDocumentation(localTarget: AppleScriptComponent): String =
        buildString {
            append("<b>")
            append(localTarget.localSymbolKind().replaceFirstChar { it.uppercaseChar() })
            append("</b> ")
            append(StringUtil.escapeXmlEntities(localTarget.getName().orEmpty()))
        }
}

private fun AppleScriptComponent.isLocalDataSymbol(): Boolean =
    isVariable() ||
        this is AppleScriptSimpleFormalParameter ||
        this is AppleScriptDirectParameterDeclaration ||
        this is AppleScriptLabeledParameterDeclarationPart

private fun AppleScriptComponent.localSymbolKind(): String = if (isParameter()) "parameter" else "variable"

private fun AppleScriptComponent.isParameter(): Boolean =
    this is AppleScriptSimpleFormalParameter ||
        this is AppleScriptDirectParameterDeclaration ||
        this is AppleScriptLabeledParameterDeclarationPart
