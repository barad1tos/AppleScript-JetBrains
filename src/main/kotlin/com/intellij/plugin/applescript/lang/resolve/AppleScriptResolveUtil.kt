package com.intellij.plugin.applescript.lang.resolve

import com.intellij.plugin.applescript.psi.AppleScriptAssignmentStatement
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptScriptObject
import com.intellij.plugin.applescript.psi.AppleScriptTellCompoundStatement
import com.intellij.plugin.applescript.psi.AppleScriptTellSimpleStatement
import com.intellij.plugin.applescript.psi.AppleScriptVarDeclarationList
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.SortedList

object AppleScriptResolveUtil {
    @JvmStatic
    fun toCandidateInfoArray(elements: List<PsiElement?>?): Array<ResolveResult> {
        if (elements == null) return ResolveResult.EMPTY_ARRAY
        val filtered = elements.filterNotNull()
        return Array(filtered.size) { i -> PsiElementResolveResult(filtered[i]) }
    }

    // KEEP (Phase 8 / v2.0 backlog): relocating this to AppleScriptScriptObject and
    // simplifying the three-way variable extraction is a real refactor that touches the
    // PSI-adjacent resolve surface, not a one-liner. Deferred — out of the v1.x cleanup
    // scope (behaviour-preserving only).
    @JvmStatic
    fun getNamedSubComponentsFor(script: AppleScriptScriptObject): List<AppleScriptComponent> {
        val result = ArrayList<AppleScriptComponent>()
        val scriptBody = script.getScriptBody()
        val namedComponents = PsiTreeUtil.getChildrenOfType(scriptBody, AppleScriptComponent::class.java)
        val varsCreations = PsiTreeUtil.getChildrenOfType(scriptBody, AppleScriptAssignmentStatement::class.java)
        val varsDeclarations = PsiTreeUtil.getChildrenOfType(scriptBody, AppleScriptVarDeclarationList::class.java)

        namedComponents?.let { result.addAll(it) }
        varsCreations?.forEach { variable ->
            result.addAll(variable.targets)
        }
        varsDeclarations?.forEach { declarationList ->
            PsiTreeUtil.getChildrenOfType(declarationList, AppleScriptComponent::class.java)?.let {
                result.addAll(it)
            }
        }
        return result
    }

    @JvmStatic
    fun getTellStatementResolveScope(myElement: PsiElement): SortedList<PsiElement> {
        val resultList = SortedList<PsiElement> { e1, e2 -> e2.textOffset - e1.textOffset }
        var tellStatement: PsiElement? = myElement
        while (tellStatement != null) {
            tellStatement = tellStatement.parent
            if (tellStatement is AppleScriptTellSimpleStatement || tellStatement is AppleScriptTellCompoundStatement) {
                resultList.add(tellStatement)
            }
        }
        return resultList
    }
}
