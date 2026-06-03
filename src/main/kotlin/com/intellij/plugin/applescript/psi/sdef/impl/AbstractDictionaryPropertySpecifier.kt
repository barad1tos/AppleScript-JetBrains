package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.lang.ASTNode
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeElement
import com.intellij.plugin.applescript.psi.sdef.DictionaryCompositeName
import com.intellij.plugin.applescript.psi.sdef.DictionaryReference

open class AbstractDictionaryPropertySpecifier(
    node: ASTNode,
) : DictionaryCompositeNameImpl(node),
    DictionaryCompositeElement,
    DictionaryCompositeName {
    override fun getCompositeNameElement(): DictionaryCompositeName = this

    override fun getReference(): DictionaryReference = DictionaryPropertySpecifierReference()

    private inner class DictionaryPropertySpecifierReference :
        AbstractDictionaryReferenceElement(),
        DictionaryReference {
        override fun getMyElement(): DictionaryCompositeElement = this@AbstractDictionaryPropertySpecifier
    }
}
