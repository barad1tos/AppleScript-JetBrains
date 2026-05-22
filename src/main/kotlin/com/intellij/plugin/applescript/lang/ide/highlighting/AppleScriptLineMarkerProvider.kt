package com.intellij.plugin.applescript.lang.ide.highlighting

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.psi.AppleScriptApplicationReference
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class AppleScriptLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        if (element !is AppleScriptApplicationReference) return
        val leafNode: PsiElement = PsiTreeUtil.firstChild(element) ?: return

        val dictionaryService = element.project.getService(AppleScriptProjectDictionaryService::class.java)
        val appName = element.getApplicationName()
        if (dictionaryService == null || StringUtil.isEmpty(appName)) return
        val dictionary = dictionaryService.getDictionary(appName!!) ?: return

        val builder = NavigationGutterIconBuilder.create(dictionary.getIcon(0))
            .setTargets(dictionary)
            .setTooltipText("Navigate to application dictionary file")
        result.add(builder.createLineMarkerInfo(leafNode))
    }
}
