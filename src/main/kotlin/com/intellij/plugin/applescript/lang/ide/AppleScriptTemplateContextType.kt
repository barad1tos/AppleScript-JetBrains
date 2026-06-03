package com.intellij.plugin.applescript.lang.ide

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.plugin.applescript.AppleScriptFileType

class AppleScriptTemplateContextType : TemplateContextType("AppleScript") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean =
        templateActionContext.file.fileType === AppleScriptFileType

    override fun createHighlighter(): SyntaxHighlighter? =
        SyntaxHighlighterFactory.getSyntaxHighlighter(
            AppleScriptFileType,
            null,
            null,
        )
}
