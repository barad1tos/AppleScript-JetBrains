package com.intellij.plugin.applescript.psi.impl

import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.plugin.applescript.lang.AppleScriptComponentType
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.IncorrectOperationException
import javax.swing.Icon

/**
 * Base class for AppleScript components — declarations, definitions, variables, handlers.
 * Subclasses provide `getIdentifier()`.
 */
abstract class AbstractAppleScriptComponent(
    node: ASTNode,
) : AppleScriptPsiElementImpl(node),
    AppleScriptComponent {
    override fun toString(): String = node.elementType.toString()

    @Throws(IncorrectOperationException::class)
    override fun setName(newElementName: String): PsiElement {
        val identifier = getIdentifier()
        val identifierNew = AppleScriptPsiElementFactory.createIdentifierFromText(project, newElementName)
        if (identifierNew != null) {
            node.replaceChild(identifier.node, identifierNew.node)
        }
        return this
    }

    override fun getReferences(): Array<PsiReference> = super.getReferences()

    override fun getName(): String? {
        val nameIdentifier = getNameIdentifier()
        return nameIdentifier?.text ?: node.text
    }

    override fun getTextOffset(): Int = getNameIdentifier()?.textOffset ?: super.getTextOffset()

    override fun getNameIdentifier(): PsiElement? = getIdentifier()

    abstract override fun getIdentifier(): AppleScriptIdentifier

    override fun getIcon(flags: Int): Icon = AppleScriptComponentType.typeOf(this)?.icon ?: AllIcons.General.Ellipsis

    override fun getReference(): PsiReference? =
        if (this is AppleScriptTargetVariable) {
            AppleScriptTargetReferenceImpl(this)
        } else {
            null
        }

    override fun getPresentation(): ItemPresentation = AppleScriptComponentPresentation(this)
}
