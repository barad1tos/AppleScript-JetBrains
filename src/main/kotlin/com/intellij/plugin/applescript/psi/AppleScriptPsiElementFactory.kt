package com.intellij.plugin.applescript.psi

import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.AppleScriptFile
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

object AppleScriptPsiElementFactory {
    @JvmStatic
    fun createHandlerParameterLabel(
        project: Project,
        labelName: String?,
    ): AppleScriptHandlerParameterLabel? {
        val newLabelName = labelName?.takeIf { it.isNotEmpty() } ?: "to"
        val src = """dummyHandlerName $newLabelName "some sting val""""
        val file = createFile(project, src)
        file.findChildByClass(AppleScriptHandlerLabeledParametersCallExpression::class.java)
        val handlerCall =
            file.firstChild as? AppleScriptHandlerLabeledParametersCallExpression
                ?: return null
        return handlerCall.children
            .firstOrNull { it.node.elementType === AppleScriptTypes.HANDLER_PARAMETER_LABEL }
            as? AppleScriptHandlerParameterLabel
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

    @Suppress("unused")
    private val placeholder: PsiElement? = null
}
