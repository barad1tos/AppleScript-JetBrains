package com.intellij.plugin.applescript.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptHandler
import com.intellij.plugin.applescript.psi.AppleScriptHandlerSelectorPart
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptPsiElementFactory
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.pom.PomNamedTarget
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.IncorrectOperationException
import com.intellij.util.PlatformIcons
import javax.swing.Icon

open class AppleScriptHandlerInterleavedParameters(
    node: ASTNode,
) : AbstractAppleScriptComponent(node),
    AppleScriptHandler,
    NavigatablePsiElement,
    PsiNameIdentifierOwner,
    PomNamedTarget {
    @Throws(IncorrectOperationException::class)
    override fun setName(newElementName: String): PsiElement {
        val selectors = getParameters().size
        val selectorNames = newElementName.split(":")
        if (selectorNames.size == selectors) {
            for (index in 0 until selectors) {
                val myId = getParameters()[index].getSelectorNameIdentifier()
                val newId = AppleScriptPsiElementFactory.createIdentifierFromText(project, selectorNames[index])
                if (newId != null) myId.replace(newId)
            }
        } else {
            val myIdentifier = getParameters()[0].getSelectorNameIdentifier()
            val identifierNew = AppleScriptPsiElementFactory.createIdentifierFromText(project, newElementName)
            if (identifierNew != null) myIdentifier.replace(identifierNew)
        }
        return this
    }

    override fun isHandler(): Boolean = true

    override fun getNameIdentifier(): PsiElement? {
        findChildByClass(AppleScriptIdentifier::class.java) // legacy side-effect read
        val selector =
            findChildByType<AppleScriptHandlerSelectorPart>(
                AppleScriptTypes.HANDLER_INTERLEAVED_PARAMETERS_SELECTOR_PART,
            )
        return selector?.getSelectorNameIdentifier()
    }

    override fun getIdentifier(): AppleScriptIdentifier {
        val selector =
            findChildByType<AppleScriptHandlerSelectorPart>(
                AppleScriptTypes.HANDLER_INTERLEAVED_PARAMETERS_SELECTOR_PART,
            )
        return selector?.getSelectorNameIdentifier() ?: getSelectors().first()
    }

    override fun getIcon(flags: Int): Icon = PlatformIcons.FUNCTION_ICON

    override fun getName(): String = getSelector()

    override fun getTextOffset(): Int {
        val selectors =
            findChildrenByType<AppleScriptHandlerSelectorPart>(
                AppleScriptTypes.HANDLER_INTERLEAVED_PARAMETERS_SELECTOR_PART,
            )
        return if (selectors.isEmpty()) super.getTextOffset() else selectors[0].textRange.startOffset
    }

    override fun getSelector(): String =
        buildString {
            for (selector in getParameters()) {
                append(selector.getSelectorPart())
            }
        }

    override fun getSelectors(): List<AppleScriptIdentifier> = getParameters().map { it.getSelectorNameIdentifier() }

    override fun getParameters(): List<AppleScriptHandlerSelectorPart> =
        findChildrenByType(AppleScriptTypes.HANDLER_INTERLEAVED_PARAMETERS_SELECTOR_PART)
}
