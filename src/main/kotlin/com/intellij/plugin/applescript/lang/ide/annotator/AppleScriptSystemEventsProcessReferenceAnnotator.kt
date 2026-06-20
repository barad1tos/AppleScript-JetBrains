package com.intellij.plugin.applescript.lang.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.psi.AppleScriptApplicationObjectReference
import com.intellij.plugin.applescript.psi.AppleScriptNameReference
import com.intellij.plugin.applescript.psi.AppleScriptStringLiteralExpression
import com.intellij.plugin.applescript.psi.AppleScriptTellCompoundStatement
import com.intellij.plugin.applescript.psi.AppleScriptTellSimpleStatement
import com.intellij.plugin.applescript.psi.impl.getApplicationName
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

private const val SYSTEM_EVENTS_APPLICATION_NAME = "System Events"

// Inspection scope, not a parser allowlist: System Events exposes process objects
// by these class terms, and this annotator only validates their literal names.
private const val PROCESS_CLASS_NAME = "process"

internal object AppleScriptSystemEventsProcessReferenceAnnotator {
    internal fun resolveWarningMessage(
        referenceText: String,
        isInsideSystemEventsTell: Boolean,
        areAppDictionariesIndexed: Boolean,
        isKnownApplication: (String) -> Boolean,
    ): String? {
        if (!isInsideSystemEventsTell || !areAppDictionariesIndexed) return null

        val processName = processNameFromReferenceText(referenceText) ?: return null
        if (processName.isBlank()) return null
        if (isKnownApplication(processName)) return null

        return "Process \"$processName\" is not known on this macOS installation"
    }

    fun annotate(
        holder: AnnotationHolder,
        reference: AppleScriptApplicationObjectReference,
    ) {
        annotateInternal(holder, reference)
    }

    fun annotate(
        holder: AnnotationHolder,
        reference: AppleScriptNameReference,
    ) {
        annotateInternal(holder, reference)
    }

    private fun annotateInternal(
        holder: AnnotationHolder,
        reference: PsiElement,
    ) {
        val warningMessage =
            resolveWarningMessage(
                referenceText = reference.text,
                isInsideSystemEventsTell = isInsideSystemEventsTell(reference),
                areAppDictionariesIndexed =
                    AppleScriptSystemDictionaryRegistryService.getInstance().areAppDictionariesIndexed(),
                isKnownApplication = ApplicationDiscoveryService.getInstance()::isKnownApplication,
            ) ?: return

        val processNameElement = processNameLiteral(reference) ?: return

        holder
            .newAnnotation(
                HighlightSeverity.WEAK_WARNING,
                warningMessage,
            ).range(processNameElement)
            .create()
    }

    private fun isInsideSystemEventsTell(reference: PsiElement): Boolean {
        var ancestor = reference.parent
        while (ancestor != null) {
            when (ancestor) {
                is AppleScriptTellSimpleStatement -> {
                    val applicationName = getApplicationName(ancestor)
                    if (applicationName != null) return isSystemEventsTell(applicationName)
                }
                is AppleScriptTellCompoundStatement -> {
                    val applicationName = getApplicationName(ancestor)
                    if (applicationName != null) return isSystemEventsTell(applicationName)
                }
            }
            ancestor = ancestor.parent
        }
        return false
    }

    private fun isSystemEventsTell(applicationName: String?): Boolean =
        applicationName?.equals(SYSTEM_EVENTS_APPLICATION_NAME, ignoreCase = true) == true

    private fun processNameLiteral(reference: PsiElement): PsiElement? {
        val stringLiteralExpression =
            PsiTreeUtil.findChildOfType(reference, AppleScriptStringLiteralExpression::class.java) ?: return null
        val objectReferenceHead = objectReferenceHead(reference, stringLiteralExpression) ?: return null
        if (objectReferenceHead != PROCESS_CLASS_NAME) return null
        return stringLiteralExpression
    }

    private fun processNameFromReferenceText(referenceText: String): String? {
        if (!hasSupportedProcessHead(referenceText)) return null

        val literalText =
            referenceText
                .substringAfter('"', missingDelimiterValue = "")
                .substringBeforeLast('"', missingDelimiterValue = "")
        return literalText.takeIf { it.isNotEmpty() }
    }

    private fun hasSupportedProcessHead(referenceText: String): Boolean {
        val referenceHead =
            referenceText
                .substringBefore('"')
                .trim()
                .lowercase()
                .split(WHITESPACE)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        return referenceHead == PROCESS_CLASS_NAME
    }

    private fun objectReferenceHead(
        reference: PsiElement,
        literal: PsiElement,
    ): String? {
        val prefix = reference.text.substringBefore(literal.text)
        if (prefix.isBlank()) return null
        return prefix
            .trim()
            .lowercase()
            .split(WHITESPACE)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private val WHITESPACE = Regex("\\s+")
}
