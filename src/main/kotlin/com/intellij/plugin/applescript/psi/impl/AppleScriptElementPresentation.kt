package com.intellij.plugin.applescript.psi.impl

import com.intellij.navigation.ColoredItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.plugin.applescript.AppleScriptNames
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import javax.swing.Icon

open class AppleScriptElementPresentation(
    private val myElement: AppleScriptPsiElement,
) : ColoredItemPresentation {

    private var myTextAttributes: TextAttributesKey? = null

    fun getElement(): AppleScriptPsiElement = myElement

    override fun getTextAttributesKey(): TextAttributesKey? = myTextAttributes

    fun setTextAttributesKey(textAttributes: TextAttributesKey) {
        myTextAttributes = textAttributes
    }

    override fun getPresentableText(): String? =
        myElement.name ?: AppleScriptNames.UNNAMED_ELEMENT

    override fun getLocationString(): String? = "(${myElement.containingFile})"

    override fun getIcon(unused: Boolean): Icon? = myElement.getIcon(0)
}
