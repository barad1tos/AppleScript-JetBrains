package com.intellij.plugin.applescript.lang

import com.intellij.plugin.applescript.AppleScriptIcons
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.DictionaryEnumeratorImpl
import com.intellij.plugin.applescript.lang.sdef.SuiteImpl
import com.intellij.plugin.applescript.psi.AppleScriptComponent
import com.intellij.plugin.applescript.psi.AppleScriptScriptObject
import com.intellij.plugin.applescript.psi.AppleScriptSimpleFormalParameter
import com.intellij.psi.PsiElement
import com.intellij.util.PlatformIcons
import javax.swing.Icon

enum class AppleScriptComponentType(@JvmField val icon: Icon) {
    PROPERTY(PlatformIcons.PROPERTY_ICON),
    HANDLER(PlatformIcons.FUNCTION_ICON),
    SCRIPT(AppleScriptIcons.FILE),
    PARAMETER(PlatformIcons.PARAMETER_ICON),

    /** Generic variable bucket — anything that didn't match a more specific category. */
    VARIABLE(PlatformIcons.VARIABLE_ICON),

    APPLICATION_DICTIONARY(AppleScriptIcons.OPEN_DICTIONARY),
    DICTIONARY_SUITE(PlatformIcons.PACKAGE_ICON),
    DICTIONARY_CLASS(PlatformIcons.CLASS_ICON),
    DICTIONARY_ENUMERATOR(PlatformIcons.VARIABLE_ICON),
    DICTIONARY_COMMAND(PlatformIcons.FUNCTION_ICON),
    ;

    fun getIcon(): Icon = icon

    @Suppress("unused")
    fun getIcon(component: AppleScriptComponent): Icon = icon

    override fun toString(): String = super.toString().replace("_", " ")

    companion object {
        @JvmStatic
        fun typeOf(element: PsiElement?): AppleScriptComponentType? {
            if (element !is AppleScriptComponent) return null
            return when {
                element.isHandler() -> HANDLER
                element is AppleScriptSimpleFormalParameter || element is CommandParameter -> PARAMETER
                element.isVariable() -> VARIABLE
                element.isScriptProperty() || element.isObjectProperty() -> PROPERTY
                element is AppleScriptScriptObject -> SCRIPT
                element is AppleScriptClass -> DICTIONARY_CLASS
                element is AppleScriptCommand -> DICTIONARY_COMMAND
                element is DictionaryEnumeratorImpl -> DICTIONARY_ENUMERATOR
                element is SuiteImpl -> DICTIONARY_SUITE
                element is ApplicationDictionary -> APPLICATION_DICTIONARY
                else -> null
            }
        }
    }
}
