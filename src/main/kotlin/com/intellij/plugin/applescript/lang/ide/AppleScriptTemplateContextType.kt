package com.intellij.plugin.applescript.lang.ide

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.psi.PsiFile

class AppleScriptTemplateContextType protected constructor() : TemplateContextType("AppleScript", "AppleScript") {

    override fun isInContext(file: PsiFile, offset: Int): Boolean =
        file.fileType === AppleScriptFileType

    override fun createHighlighter(): SyntaxHighlighter? =
        SyntaxHighlighterFactory.getSyntaxHighlighter(AppleScriptFileType, null, null)
}
