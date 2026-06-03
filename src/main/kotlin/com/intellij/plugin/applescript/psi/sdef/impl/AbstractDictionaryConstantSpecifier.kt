package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeElement
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeName
import com.intellij.plugin.applescript.psi.sdef.DictionaryReference

open class AbstractDictionaryConstantSpecifier(
    node: ASTNode,
) : DictionaryCompositeNameImpl(node),
    DictionaryCompositeElement,
    DictionaryCompositeName {
    override fun getReference(): DictionaryReference = DictionaryConstantSpecifierReference()

    override fun getCompositeNameElement(): DictionaryCompositeName = this

    private inner class DictionaryConstantSpecifierReference :
        AbstractDictionaryReferenceElement(),
        DictionaryReference {
        override fun getMyElement(): DictionaryCompositeElement = this@AbstractDictionaryConstantSpecifier
    }
}
