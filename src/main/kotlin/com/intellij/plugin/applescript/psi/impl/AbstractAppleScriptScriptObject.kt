package com.intellij.plugin.applescript.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.plugin.applescript.lang.AppleScriptComponentType
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptScriptBody
import com.intellij.plugin.applescript.psi.AppleScriptScriptObject
import javax.swing.Icon

abstract class AbstractAppleScriptScriptObject(
    node: ASTNode,
) : AbstractAppleScriptComponent(node),
    AppleScriptScriptObject {
    override fun isObjectProperty(): Boolean = false

    override fun isHandler(): Boolean = false

    override fun isScriptProperty(): Boolean = false

    override fun getPresentation(): ItemPresentation =
        object : AppleScriptElementPresentation(this) {
            override fun getPresentableText(): String? = name
        }

    override fun getIcon(flags: Int): Icon = AppleScriptComponentType.DICTIONARY_CLASS.icon

    override fun getParentScriptObject(): AppleScriptScriptObject? = null

    override fun getProperties(): List<AppleScriptComponent>? = null

    override fun getHandlers(): List<AppleScriptComponent>? = null

    override fun getScriptBody(): AppleScriptScriptBody? = findChildByClass(AppleScriptScriptBody::class.java)
}
