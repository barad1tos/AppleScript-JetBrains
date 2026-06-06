@file:JvmName("AppleScriptPsiImplUtil")
@file:JvmMultifileClass

package com.intellij.plugin.applescript.psi.impl

import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.AppleScriptApplicationReference
import com.intellij.plugin.applescript.psi.AppleScriptTellCompoundStatement
import com.intellij.plugin.applescript.psi.AppleScriptTellSimpleStatement
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.plugin.applescript.psi.AppleScriptUseStatement
import com.intellij.plugin.applescript.psi.AppleScriptUsingTermsFromStatement
import com.intellij.psi.util.PsiTreeUtil

fun getNameFromApplicationReference(appRef: AppleScriptApplicationReference?): String? {
    if (appRef == null) return null
    val text = appRef.text
    val from = text.indexOf('"') + 1
    val to = text.indexOf('"', from)
    val applicationName =
        if (from in 0..text.length && to in 0..text.length && from <= to) {
            text.substring(from, to)
        } else {
            null
        }
    return applicationName?.let {
        if (appRef.node.findChildByType(AppleScriptTypes.ID) != null) {
            ApplicationDiscoveryService.getInstance().findKnownApplicationNameByBundleIdentifier(it) ?: it
        } else {
            it
        }
    }
}

fun getApplicationName(useStatement: AppleScriptUseStatement): String? =
    when {
        useStatement.text.contains("application") -> {
            val appRef = PsiTreeUtil.findChildOfType(useStatement, AppleScriptApplicationReference::class.java)
            appRef
                ?.node
                ?.findChildByType(AppleScriptTypes.STRING_LITERAL)
                ?.text
                ?.replace("\"", "")
        }

        useStatement.node.findChildByType(AppleScriptTypes.SCRIPTING_ADDITIONS) != null ->
            ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY

        else -> null
    }

fun useStandardAdditions(useStatement: AppleScriptUseStatement): Boolean =
    useStatement.node.findChildByType(AppleScriptTypes.SCRIPTING_ADDITIONS) != null

fun withImporting(useStatement: AppleScriptUseStatement): Boolean {
    val text = useStatement.node.text
    return !text.contains("without") && !text.contains("false")
}

fun getApplicationName(usingTermsStatement: AppleScriptUsingTermsFromStatement): String? {
    val appRef = usingTermsStatement.applicationReference
    return appRef?.applicationName
        ?: if (withImportingStdLibrary(usingTermsStatement)) {
            ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
        } else {
            null
        }
}

fun withImportingStdLibrary(usingTermsStatement: AppleScriptUsingTermsFromStatement): Boolean =
    usingTermsStatement.node.findChildByType(AppleScriptTypes.SCRIPTING_ADDITIONS) != null

fun getApplicationName(applicationReference: AppleScriptApplicationReference): String? =
    getNameFromApplicationReference(applicationReference)

fun getApplicationName(tellCompound: AppleScriptTellCompoundStatement): String? {
    val applicationReference = tellCompound.applicationReference
    return applicationReference?.applicationName
}

fun getApplicationName(tellSimple: AppleScriptTellSimpleStatement): String? =
    PsiTreeUtil.findChildOfType(tellSimple, AppleScriptApplicationReference::class.java)?.applicationName
