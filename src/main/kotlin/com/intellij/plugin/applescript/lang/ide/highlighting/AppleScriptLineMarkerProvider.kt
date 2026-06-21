package com.intellij.plugin.applescript.lang.ide.highlighting

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.AppleScriptApplicationReference
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

class AppleScriptLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        createApplicationDictionaryMarker(element)?.let(result::add)
    }
}

private fun createApplicationDictionaryMarker(element: PsiElement): RelatedItemLineMarkerInfo<*>? =
    (element as? AppleScriptApplicationReference)?.createApplicationDictionaryMarker()

private fun AppleScriptApplicationReference.createApplicationDictionaryMarker(): RelatedItemLineMarkerInfo<*>? {
    val leafNode: PsiElement = PsiTreeUtil.firstChild(this)
    val dictionaryService = project.getService(AppleScriptProjectDictionaryService::class.java)
    val dictionary =
        getApplicationName()
            ?.takeUnless(String::isEmpty)
            ?.let { appName -> dictionaryService?.getOrCreateDictionaryFromCachedSources(appName) }

    return dictionary?.let {
        NavigationGutterIconBuilder
            .create(it.lineMarkerIcon())
            .setTargets(it)
            .setTooltipText("Navigate to application dictionary file")
            .createLineMarkerInfo(leafNode)
    }
}

private fun ApplicationDictionary.lineMarkerIcon(): Icon = (this as? FakePsiElement)?.getIcon(false) ?: getIcon(0)
