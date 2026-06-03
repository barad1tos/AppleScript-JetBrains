package com.intellij.plugin.applescript.psi.sdef

import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiPolyVariantReference

interface DictionaryReference :
    MultiRangeReference,
    PsiPolyVariantReference {
    override fun getElement(): DictionaryCompositeElement
}
