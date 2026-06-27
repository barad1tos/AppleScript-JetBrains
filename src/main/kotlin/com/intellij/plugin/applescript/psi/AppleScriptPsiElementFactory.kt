package com.intellij.plugin.applescript.psi

import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.AppleScriptFile
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

object AppleScriptPsiElementFactory {
    @JvmStatic
    fun createHandlerParameterLabel(
        project: Project,
        labelName: String?,
    ): AppleScriptHandlerParameterLabel? {
        val newLabelName = labelName?.takeIf { it.isNotEmpty() } ?: "to"
        // Build a labeled-parameters handler DEFINITION (`on h <label> p`) to host the label.
        // The previous call-form source (`name <label> "value"`) parsed as a dictionary command
        // call, never a labeled-parameters node, so no HANDLER_PARAMETER_LABEL existed to return
        // and this factory always yielded null — silently breaking the rename quick fix.
        val src = "on dummyHandler $newLabelName dummyParameter\nend dummyHandler"
        val file = createFile(project, src)
        return PsiTreeUtil.findChildOfType(file, AppleScriptHandlerParameterLabel::class.java)
    }

    @JvmStatic
    fun createFile(
        project: Project,
        text: String,
    ): AppleScriptFile {
        val name = "dummy_file" + AppleScriptFileType.defaultExtension
        val stamp = System.currentTimeMillis()
        return PsiFileFactory
            .getInstance(project)
            .createFileFromText(name, AppleScriptFileType, text, stamp, false) as AppleScriptFile
    }

    @JvmStatic
    fun createStringLiteral(
        project: Project,
        text: String,
    ): AppleScriptStringLiteralExpression {
        val file = createFile(project, "\"$text\"")
        return checkNotNull(
            PsiTreeUtil.findChildOfType(file, AppleScriptStringLiteralExpression::class.java),
        ) { "text=$text" }
    }

    @JvmStatic
    fun createIdentifierFromText(
        project: Project,
        name: String,
    ): AppleScriptIdentifier? {
        val file = createFile(project, name)
        return PsiTreeUtil.findChildOfAnyType(file, AppleScriptIdentifier::class.java)
    }
}
