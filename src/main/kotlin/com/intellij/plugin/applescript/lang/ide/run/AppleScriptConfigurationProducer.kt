@file:Suppress("DEPRECATION")

package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.psi.PsiElement

class AppleScriptConfigurationProducer : RunConfigurationProducer<AppleScriptRunConfiguration>(AppleScriptConfigurationType()) {

    override fun setupConfigurationFromContext(
        configuration: AppleScriptRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val file = context.psiLocation?.containingFile ?: return false
        val shouldSetUp = file.fileType === AppleScriptFileType
        val scriptPath = file.virtualFile?.path
        if (scriptPath != null) {
            configuration.scriptPath = scriptPath
            val parts = scriptPath.split("/")
            if (parts.isNotEmpty()) configuration.name = parts.last()
        }
        return shouldSetUp
    }

    override fun isConfigurationFromContext(
        configuration: AppleScriptRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val file = context.psiLocation?.containingFile ?: return false
        val currentFile = file.virtualFile ?: return false
        return currentFile.path == configuration.scriptPath
    }
}
