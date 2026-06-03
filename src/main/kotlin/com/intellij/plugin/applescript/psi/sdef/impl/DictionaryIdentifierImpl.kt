package com.intellij.plugin.applescript.psi.sdef.impl

import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.psi.sdef.DictionaryIdentifier
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlElement

/**
 * Simple identifier representing XmlToken-like values inside SDEF components such as
 * `<command name="my command name" ...>...</command>`: this PSI exposes the full multi-word text
 * (`"my command name"`), while [getVarIdentifier] returns a single-word handle (`"my"`) for cases
 * where a tokenised identifier is required.
 */
class DictionaryIdentifierImpl(
    parent: DictionaryComponent,
    private val varIdentifierText: String,
    xmlAttributeValue: XmlElement,
) : DictionaryComponentBase<DictionaryComponent, XmlElement>(xmlAttributeValue, parent),
    DictionaryIdentifier {
    private val varIdentifierTextList: List<String> = varIdentifierText.split(" ").toMutableList()
    private val myVarIdentifier: DictionaryIdentifier =
        DictionaryIdentifierImpl(parent, varIdentifierTextList[0], myXmlElement)

    override fun getVarIdentifier(): PsiElement = myVarIdentifier

    override fun getTextLength(): Int = varIdentifierText.length

    override fun findElementAt(offset: Int): PsiElement = this

    override fun getText(): String = varIdentifierText

    override fun getName(): String = getText()

    override fun getParent(): PsiElement = myParent
}
