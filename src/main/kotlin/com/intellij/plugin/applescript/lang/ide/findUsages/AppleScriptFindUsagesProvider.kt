package com.intellij.plugin.applescript.lang.ide.findUsages

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.AppleScriptLexerAdapter
import com.intellij.plugin.applescript.lang.AppleScriptComponentType
import com.intellij.plugin.applescript.psi.AppleScriptReferenceElement
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet

class AppleScriptFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        AppleScriptLexerAdapter(),
        TokenSet.create(AppleScriptTypes.IDENTIFIER),
        TokenSet.create(AppleScriptTypes.COMMENT),
        TokenSet.create(AppleScriptTypes.STRING_LITERAL),
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
        psiElement is PsiNamedElement || psiElement is AppleScriptReferenceElement

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        val componentType = AppleScriptComponentType.typeOf(element)
        return componentType?.toString()?.lowercase() ?: "reference"
    }

    override fun getDescriptiveName(element: PsiElement): String {
        if (element is PsiNamedElement) {
            return StringUtil.notNullize(element.name)
        }
        return ""
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        val name = (element as? PsiNamedElement)?.name
        return name ?: element.text
    }
}
