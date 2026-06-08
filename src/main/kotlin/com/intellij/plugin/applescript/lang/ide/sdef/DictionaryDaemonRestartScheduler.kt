package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager

internal object DictionaryDaemonRestartScheduler {
    fun restartOpenProjectDaemons() {
        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode || application.isHeadlessEnvironment) {
            return
        }

        application.invokeLater {
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) {
                    DaemonCodeAnalyzer.getInstance(project).settingsChanged()
                }
            }
        }
    }
}
