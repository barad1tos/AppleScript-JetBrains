package com.intellij.plugin.applescript.lang.ide.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.AppleScriptIcons
import com.intellij.psi.PsiDirectory

class CreateAppleScriptFileAction :
    CreateFileFromTemplateAction(
        "AppleScript File",
        "Creates a AppleScript file from the specified template",
        AppleScriptIcons.FILE,
    ),
    DumbAware {
    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder,
    ) {
        builder
            .setTitle("New AppleScript File")
            .addKind("AppleScript file", AppleScriptIcons.FILE, "AppleScript File.scpt")
    }

    override fun getActionName(
        directory: PsiDirectory?,
        newName: String,
        templateName: String?,
    ): String = "Create AppleScript file $newName"
}
