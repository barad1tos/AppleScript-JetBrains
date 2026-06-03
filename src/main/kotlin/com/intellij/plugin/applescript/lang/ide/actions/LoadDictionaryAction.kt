@file:Suppress("DEPRECATION")

package com.intellij.plugin.applescript.lang.ide.actions

import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.extensionSupported

class LoadDictionaryAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val view: IdeView = LangDataKeys.IDE_VIEW.getData(dataContext) ?: return
        val directories = view.directories
        val currentDirectory = directories.firstOrNull()
        val project = e.getData(CommonDataKeys.PROJECT) ?: return

        val directoryFile: VirtualFile? = currentDirectory?.virtualFile ?: project.baseDir
        openLoadDirectoryDialog(project, directoryFile, null)
    }
}

internal fun openLoadDirectoryDialog(
    project: Project,
    directoryFile: VirtualFile?,
    appName: String?,
) {
    val singleApplicationName = appName?.takeUnless { StringUtil.isEmpty(it) }
    val descriptor =
        createDictionaryFileChooserDescriptor(
            chooseMultiple = singleApplicationName == null,
        )

    FileChooser.chooseFiles(descriptor, project, directoryFile) { files ->
        loadSelectedDictionaries(project, files, singleApplicationName)
    }
}

private fun createDictionaryFileChooserDescriptor(chooseMultiple: Boolean): FileChooserDescriptor =
    FileChooserDescriptor(
        true,
        true,
        false,
        false,
        false,
        chooseMultiple,
    )

private fun loadSelectedDictionaries(
    project: Project,
    files: List<VirtualFile>,
    singleApplicationName: String?,
) {
    val dictionaryService =
        project.getService(AppleScriptProjectDictionaryService::class.java) ?: return
    val supportedFiles = files.filter { extensionSupported(it.extension) }

    if (singleApplicationName != null) {
        supportedFiles.firstOrNull()?.let { file ->
            dictionaryService.createDictionaryFromFile(singleApplicationName, file)
        }
        return
    }

    val dictionaryRequests =
        supportedFiles.mapNotNull { file ->
            resolveApplicationName(project, file)?.let { applicationName ->
                applicationName to file
            }
        }

    dictionaryRequests.forEach { (applicationName, file) ->
        dictionaryService.createDictionaryFromFile(applicationName, file)
    }
}

private fun resolveApplicationName(
    project: Project,
    file: VirtualFile,
): String? {
    val applicationName =
        if (ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS.contains(file.extension)) {
            file.nameWithoutExtension
        } else {
            Messages.showInputDialog(
                project,
                "Please specify application name for dictionary ${file.name}",
                "Enter Application Name",
                null,
                file.nameWithoutExtension,
                null,
            )
        }

    return applicationName?.takeUnless { StringUtil.isEmpty(it) }
}
