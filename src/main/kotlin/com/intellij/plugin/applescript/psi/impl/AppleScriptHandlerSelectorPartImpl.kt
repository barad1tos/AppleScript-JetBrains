package com.intellij.plugin.applescript.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptHandlerSelectorPart
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptSelectorId
import com.intellij.plugin.applescript.psi.AppleScriptTargetListLiteral
import com.intellij.plugin.applescript.psi.AppleScriptTargetRecordLiteral
import com.intellij.plugin.applescript.psi.AppleScriptTargetVariable
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

abstract class AppleScriptHandlerSelectorPartImpl(node: ASTNode) :
    AppleScriptNamedElementImpl(node),
    AppleScriptHandlerSelectorPart {

    override fun findParameters(): List<AppleScriptComponent> {
        val components = mutableListOf<AppleScriptComponent>()
        when (val parameter = getParameter()) {
            is AppleScriptTargetVariable -> components.add(parameter)
            is AppleScriptTargetListLiteral -> components.addAll(AppleScriptPsiImplUtil.getTargets(parameter))
            is AppleScriptTargetRecordLiteral -> components.addAll(AppleScriptPsiImplUtil.getTargets(parameter))
        }
        return components
    }

    override fun getReference(): PsiReference? = null

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement = super.setName(name)

    override fun getName(): String? = getParameterName()

    override fun getNameIdentifier(): PsiElement? = findParameterNode()?.psi

    override fun getIdentifier(): AppleScriptIdentifier {
        val parameter = getParameter()
        val parameterId = PsiTreeUtil.findChildOfAnyType(parameter, AppleScriptIdentifier::class.java)
        return parameterId ?: PsiTreeUtil.getRequiredChildOfType(parameter ?: lastChild, AppleScriptIdentifier::class.java)
    }

    override fun getParameterName(): String? = findParameterNode()?.text

    override fun findParameterNode(): ASTNode? = getParameter()?.node

    override fun getParameter(): PsiElement? {
        val first = firstChild
        val last = findLastChildByType<PsiElement>(AppleScriptTypes.TARGET_VARIABLE)
        val lastAny = lastChild
        return when {
            first != lastAny && lastAny != null -> lastAny
            first != last && last != null -> last
            else -> null
        }
    }

    override fun getSelectorPart(): String = buildString {
        var child: ASTNode? = node.firstChildNode
        if (child != null) {
            append(child.text)
            child = child.treeNext
        }
        while (child != null && AppleScriptTokenTypesSets.COMMENT_OR_WHITE_SPACE.contains(child.elementType)) {
            child = child.treeNext
        }
        if (child != null && child.elementType == AppleScriptTypes.COLON) {
            append(child.text)
        }
    }

    override fun getSelectorNameIdentifier(): AppleScriptIdentifier =
        findNotNullChildByClass(AppleScriptSelectorId::class.java).identifier
}
