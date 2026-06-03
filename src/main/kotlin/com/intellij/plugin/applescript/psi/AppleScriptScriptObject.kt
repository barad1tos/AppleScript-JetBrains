package com.intellij.plugin.applescript.psi

interface AppleScriptScriptObject : AppleScriptComponent {
    fun getParentScriptObject(): AppleScriptScriptObject?

    fun getProperties(): List<AppleScriptComponent>?

    fun getHandlers(): List<AppleScriptComponent>?

    fun getScriptBody(): AppleScriptScriptBody?
}
