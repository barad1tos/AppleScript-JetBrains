package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiElementImpl
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeName
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

open class DictionaryCompositeNameImpl(
    node: ASTNode,
) : AppleScriptPsiElementImpl(node),
    DictionaryCompositeName {
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

    override fun getName(): String = getCompositeName()
}
