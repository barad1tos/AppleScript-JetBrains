package com.intellij.plugin.applescript.lang.ide.findUsages

import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.usageView.UsageViewLongNameLocation

class AppleScriptElementDescriptionProvider : ElementDescriptionProvider {

    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        if (location is UsageViewLongNameLocation && element is PsiNamedElement && element is AppleScriptPsiElement) {
            return element.name
        }
        return null
    }
}
