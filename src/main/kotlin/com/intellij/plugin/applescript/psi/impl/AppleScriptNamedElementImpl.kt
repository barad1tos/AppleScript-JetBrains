package com.intellij.plugin.applescript.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptNamedElement
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.util.IncorrectOperationException
import com.intellij.psi.util.PsiTreeUtil.getRequiredChildOfType as requiredChild

abstract class AppleScriptNamedElementImpl(
    node: ASTNode,
) : AppleScriptPsiElementImpl(node),
    AppleScriptNamedElement,
    PsiNameIdentifierOwner {
    @Throws(IncorrectOperationException::class)
    override fun setName(newElementName: String): PsiElement {
        val identifier = getIdentifier()
        val identifierNew = AppleScriptPsiElementFactory.createIdentifierFromText(project, newElementName)
        if (identifierNew != null) {
            node.replaceChild(identifier.node, identifierNew.node)
        }
        return this
    }

    override fun getReference(): PsiReference? = super.getReference()

    override fun getPresentation(): ItemPresentation? {
        val parent = parent
        return if (parent is NavigationItem) parent.presentation else null
    }

    override fun getName(): String? = getNameIdentifier()?.text ?: getIdentifier().text

    override fun getNameIdentifier(): PsiElement? = getIdentifier()

    override fun getIdentifier(): AppleScriptIdentifier = requiredChild(this, AppleScriptIdentifier::class.java)
}
