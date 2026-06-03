package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptDirectParameterVal
import com.intellij.plugin.applescript.psi.impl.AppleScriptExpressionImpl
import com.intellij.plugin.applescript.psi.sdef.AppleScriptCommandHandlerCall
import com.intellij.plugin.applescript.psi.sdef.AppleScriptCommandHandlerParameter
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeElement
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeName
import com.intellij.plugin.applescript.psi.sdef.DictionaryReference
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

open class AbstractAppleScriptCommandHandlerCall(
    node: ASTNode,
) : AppleScriptExpressionImpl(node),
    AppleScriptCommandHandlerCall {
    override fun getReference(): DictionaryReference = CommandHandlerReference()

    override fun getCompositeNameElement(): DictionaryCompositeName =
        PsiTreeUtil.getRequiredChildOfType(this, DictionaryCompositeName::class.java)

    override fun getCommandName(): String = getCompositeNameElement().getCompositeName()

    override fun getDirectParameter(): PsiElement? =
        PsiTreeUtil.getChildOfType(this, AppleScriptDirectParameterVal::class.java)

    override fun getCommandParameters(): List<AppleScriptCommandHandlerParameter>? =
        PsiTreeUtil.getChildrenOfTypeAsList(this, AppleScriptCommandHandlerParameter::class.java)

    private inner class CommandHandlerReference :
        AbstractDictionaryReferenceElement(),
        DictionaryReference {
        override fun isReferenceTo(element: PsiElement): Boolean {
            val target = resolve()
            if (element is AppleScriptCommandHandlerCall && target != null) {
                return target === element
            }
            return super.isReferenceTo(element)
        }

        override fun getMyElement(): DictionaryCompositeElement = this@AbstractAppleScriptCommandHandlerCall
    }
}
