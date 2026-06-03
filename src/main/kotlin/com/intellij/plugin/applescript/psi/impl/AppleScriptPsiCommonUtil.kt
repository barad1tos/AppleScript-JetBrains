@file:JvmName("AppleScriptPsiImplUtil")
@file:JvmMultifileClass

package com.intellij.plugin.applescript.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.AppleScriptArgumentSelector
import com.intellij.plugin.applescript.psi.AppleScriptIdentifier
import com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

fun getSelectorIdentifier(argumentSelector: AppleScriptArgumentSelector): AppleScriptIdentifier? =
    PsiTreeUtil.findChildOfType(argumentSelector, AppleScriptIdentifier::class.java)

fun getSelectorName(argumentSelector: AppleScriptArgumentSelector): String {
    val result = StringBuilder()
    var child: ASTNode? = argumentSelector.node.firstChildNode
    while (child != null) {
        val tokenType = child.elementType
        if (tokenType == AppleScriptTypes.IDENTIFIER || tokenType == AppleScriptTypes.COLON) {
            result.append(child.text)
        }
        child = child.treeNext
    }
    return result.toString()
}

fun isWhiteSpaceOrNls(node: ASTNode?): Boolean = node?.elementType in AppleScriptTokenTypesSets.WHITE_SPACES_SET

fun isBefore(
    e1: PsiElement,
    e2: PsiElement,
    strict: Boolean,
): Boolean = if (strict) e1.textOffset < e2.textOffset else e1.textOffset <= e2.textOffset
