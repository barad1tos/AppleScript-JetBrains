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

    companion object {

        @JvmStatic
        fun openLoadDirectoryDialog(project: Project, directoryFile: VirtualFile?, appName: String?) {
            val chooseMultiple = StringUtil.isEmpty(appName)
            val descriptor = FileChooserDescriptor(true, true, false, false, false, chooseMultiple)
            FileChooser.chooseFiles(descriptor, project, directoryFile) { files ->
                val projectDictionaryRegistry =
                    project.getService(AppleScriptProjectDictionaryService::class.java) ?: return@chooseFiles

                for (file in files) {
                    if (!extensionSupported(file.extension)) continue

                    if (chooseMultiple) {
                        val applicationName: String? =
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
                        if (StringUtil.isEmpty(applicationName)) continue
                        // Guarded by the StringUtil.isEmpty(applicationName) continue above; the
                        // opaque library call hides the non-null invariant from Kotlin flow-analysis.
                        requireNotNull(applicationName) { "applicationName non-null: guarded by StringUtil.isEmpty continue above" }
                        projectDictionaryRegistry.createDictionaryFromFile(applicationName, file)
                    } else {
                        // This else branch is reached only when chooseMultiple is false, and
                        // chooseMultiple = StringUtil.isEmpty(appName), so appName is non-empty here.
                        requireNotNull(appName) { "appName non-null: this branch implies chooseMultiple==false, i.e. !StringUtil.isEmpty(appName)" }
                        projectDictionaryRegistry.createDictionaryFromFile(appName, file)
                        return@chooseFiles
                    }
                }
            }
        }
    }
}
