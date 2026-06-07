package com.intellij.plugin.applescript.lang.ide.findUsages

import com.intellij.plugin.applescript.psi.AppleScriptFormalParameterList
import com.intellij.plugin.applescript.psi.AppleScriptHandlerInterleavedParametersSelectorPart
import com.intellij.plugin.applescript.psi.AppleScriptLabeledParameterDeclarationPart
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.PsiElement
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider

class AppleScriptUsageTypeProvider : UsageTypeProvider {
    override fun getUsageType(element: PsiElement): UsageType? {
        if (element !is AppleScriptPsiElement) return null
        // KEEP (Phase 8 / v2.0 backlog: BL-E7): collapsing this three-way check into a single type
        // test requires tightening the PSI shape (a grammar-tier change), which is frozen
        // across the v1.x cycle. Deferred to the v2.0 grammar-hardening milestone.
        if (element is AppleScriptLabeledParameterDeclarationPart ||
            element.context is AppleScriptFormalParameterList ||
            element.context is AppleScriptHandlerInterleavedParametersSelectorPart
        ) {
            return UsageType.CLASS_METHOD_PARAMETER_DECLARATION
        }
        return null
    }
}
