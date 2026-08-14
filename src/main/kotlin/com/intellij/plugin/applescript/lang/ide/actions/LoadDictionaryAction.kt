package com.intellij.plugin.applescript.lang.ide.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.extensionSupported
import java.util.Queue
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue

private val LOG: Logger = Logger.getInstance("#${LoadDictionaryAction::class.java.name}")
private const val LOAD_TASK_TITLE = "Loading AppleScript dictionaries"

class LoadDictionaryAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val view: IdeView = LangDataKeys.IDE_VIEW.getData(dataContext) ?: return
        val directories = view.directories
        val currentDirectory = directories.firstOrNull()
        val project = e.getData(CommonDataKeys.PROJECT) ?: return

        val directoryFile: VirtualFile? =
            currentDirectory?.virtualFile ?: project.projectDirectory()
        openLoadDirectoryDialog(project, directoryFile, null)
    }
}

@Service(Service.Level.PROJECT)
internal class DictionaryLoadQueue(
    private val startTask: (Task.Backgroundable) -> Unit = { task -> task.queue() },
) {
    private val pendingTasks = ArrayDeque<Task.Backgroundable>()
    private var isRunning = false

    fun submit(createTask: (() -> Unit) -> Task.Backgroundable) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        pendingTasks.addLast(createTask(::taskFinished))
        if (!isRunning) startNext()
    }

    private fun taskFinished() {
        ApplicationManager.getApplication().assertIsDispatchThread()
        check(isRunning) { "Cannot finish a dictionary load when none is running" }
        startNext()
    }

    private fun startNext() {
        val nextTask =
            generateSequence(pendingTasks::removeFirstOrNull)
                .firstOrNull { task -> task.project?.isDisposed != true }
        isRunning = nextTask != null
        nextTask?.let(startTask)
    }
}

private fun Project.projectDirectory(): VirtualFile? {
    val path = basePath
    return if (path == null) {
        null
    } else {
        LocalFileSystem.getInstance().findFileByPath(path)
    }
}

internal fun openLoadDirectoryDialog(
    project: Project,
    directoryFile: VirtualFile?,
    appName: String?,
) {
    val singleApplicationName = appName?.takeUnless { StringUtil.isEmpty(it) }
    val descriptor =
        createChooserDescriptor(
            chooseMultiple = singleApplicationName == null,
        )

    FileChooser.chooseFiles(descriptor, project, directoryFile) { files ->
        loadSelectedDictionaries(project, files, singleApplicationName)
    }
}

private fun createChooserDescriptor(chooseMultiple: Boolean): FileChooserDescriptor =
    FileChooserDescriptor(
        true,
        true,
        false,
        false,
        false,
        chooseMultiple,
    )

internal class DictionaryLoadEffects(
    val loadInfo: ((String, VirtualFile) -> DictionaryInfo?)? = null,
    val restartDaemon: (() -> Unit)? = null,
    val afterPublish: () -> Unit = {},
)

internal fun loadSelectedDictionaries(
    project: Project,
    files: List<VirtualFile>,
    singleApplicationName: String?,
    effects: DictionaryLoadEffects = DictionaryLoadEffects(),
) {
    val supportedFiles = files.filter { extensionSupported(it.extension) }
    val dictionaryRequests =
        if (singleApplicationName != null) {
            supportedFiles
                .firstOrNull()
                ?.let { file -> listOf(DictionaryLoadRequest(singleApplicationName, file)) }
                .orEmpty()
        } else {
            supportedFiles.mapNotNull { file ->
                resolveApplicationName(project, file)?.let { applicationName ->
                    DictionaryLoadRequest(applicationName, file)
                }
            }
        }
    if (dictionaryRequests.isEmpty()) return

    val dictionaryService = project.getService(AppleScriptProjectDictionaryService::class.java) ?: return
    val loadQueue = project.getService(DictionaryLoadQueue::class.java) ?: return
    val loadedInfos: Queue<DictionaryInfo> = ConcurrentLinkedQueue()
    queueDictionaryLoads(
        project = project,
        loadQueue = loadQueue,
        requests = dictionaryRequests,
        loadInfo = { applicationName, file ->
            (effects.loadInfo ?: dictionaryService::loadDictionaryInfo)(applicationName, file)?.let(loadedInfos::add)
        },
        publishDictionaries = {
            if (!project.isDisposed) {
                var hasCreatedDictionary = false
                generateSequence(loadedInfos::poll).forEach { info ->
                    if (dictionaryService.createFromLoadedInfo(info) != null) hasCreatedDictionary = true
                }
                if (hasCreatedDictionary) {
                    effects.restartDaemon?.invoke()
                        ?: DaemonCodeAnalyzer.getInstance(project).settingsChanged()
                }
            }
            effects.afterPublish()
        },
    )
}

private fun queueDictionaryLoads(
    project: Project,
    loadQueue: DictionaryLoadQueue,
    requests: List<DictionaryLoadRequest>,
    loadInfo: (String, VirtualFile) -> Unit,
    publishDictionaries: () -> Unit = {},
) {
    loadQueue.submit { taskFinished ->
        object : Task.Backgroundable(project, LOAD_TASK_TITLE, true) {
            override fun run(indicator: ProgressIndicator) {
                requests.forEachIndexed { index, request ->
                    indicator.checkCanceled()
                    indicator.text = "Loading dictionary for ${request.applicationName}"
                    try {
                        loadInfo(request.applicationName, request.file)
                    } catch (error: ProcessCanceledException) {
                        throw error
                    } catch (error: CancellationException) {
                        throw ProcessCanceledException(error)
                    } catch (error: RuntimeException) {
                        throw IllegalStateException(
                            "Failed to load dictionary '${request.applicationName}' from ${request.file.path}",
                            error,
                        )
                    }
                    indicator.fraction = (index + 1).toDouble() / requests.size
                }
            }

            override fun onThrowable(error: Throwable) {
                LOG.error("Failed to load selected AppleScript dictionaries", error)
            }

            override fun onFinished() {
                try {
                    publishDictionaries()
                } finally {
                    taskFinished()
                }
            }
        }
    }
}

private data class DictionaryLoadRequest(
    val applicationName: String,
    val file: VirtualFile,
)

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
