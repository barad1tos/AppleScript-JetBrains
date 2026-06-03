package com.intellij.plugin.applescript.psi.impl

import com.intellij.navigation.ColoredItemPresentation
import com.intellij.plugin.applescript.AppleScriptNames
import com.intellij.plugin.applescript.psi.AppleScriptPsiElement
import javax.swing.Icon

open class AppleScriptElementPresentation(
    private val myElement: AppleScriptPsiElement,
) : ColoredItemPresentation {
    override fun getTextAttributesKey() = null

    override fun getPresentableText(): String? = myElement.name ?: AppleScriptNames.UNNAMED_ELEMENT

    override fun getLocationString(): String? = "(${myElement.containingFile})"

    override fun getIcon(unused: Boolean): Icon? = myElement.getIcon(0)
}
