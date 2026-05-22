package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiElementImpl
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeElement
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeName
import com.intellij.plugin.applescript.psi.sdef.DictionaryReference
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

open class AbstractDictionaryConstantSpecifier(node: ASTNode) :
    AppleScriptPsiElementImpl(node),
    DictionaryCompositeElement,
    DictionaryCompositeName {

    override fun getReference(): DictionaryReference = DictionaryConstantSpecifierReference()

    override fun getCompositeNameElement(): DictionaryCompositeName = this

    override fun getIdentifiers(): List<PsiElement> {
        val result = ArrayList<PsiElement>()
        var psiChild: PsiElement? = firstChild
        if (psiChild == null) {
            result.add(this)
            return result
        }
        while (psiChild != null) {
            if (psiChild.node.elementType !== TokenType.WHITE_SPACE) {
                result.add(psiChild)
            }
            psiChild = psiChild.nextSibling
        }
        return result
    }

    override fun getCompositeName(): String {
        val sb = StringBuilder()
        for (id in getIdentifiers()) {
            sb.append(id.text).append(" ")
        }
        return sb.toString().trim()
    }

    private inner class DictionaryConstantSpecifierReference :
        AbstractDictionaryReferenceElement(),
        DictionaryReference {

        override fun getMyElement(): DictionaryCompositeElement = this@AbstractDictionaryConstantSpecifier
    }
}
