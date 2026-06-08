package com.intellij.plugin.applescript.smoke

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.psi.PsiManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

internal val SMOKE_LOG: Logger = Logger.getInstance("#${AppleScriptSmokeRunner::class.java.name}")

/**
 * Opens the smoke fixture project, waits for dictionary indexing, runs assertions, and
 * closes the project before process exit.
 *
 * Project bootstrap uses [ProjectUtil.openOrImport] first because it mirrors the IDE's
 * File -> Open lifecycle and gives a real [Project] plus [PsiManager]. If it returns
 * null on a platform version, the fallback uses [ProjectManagerEx.openProject] with
 * [OpenProjectTask.build], the public no-argument factory.
 */
class AppleScriptSmokeRunner(
    private val fixtureRoot: String,
    private val fixtureDir: File,
) {
    private val assertions = AppleScriptSmokeAssertions()

    fun run(): Int {
        val failures = mutableListOf<String>()
        var project: Project? = null
        try {
            project = openProjectOnEdt(fixtureDir.toPath())
            val openedProject = project
            if (openedProject == null) {
                failures.add(
                    "ProjectUtil.openOrImport + ProjectManagerEx fallback " +
                        "both returned null for $fixtureRoot",
                )
            } else {
                failures.addAll(collectProjectFailures(openedProject))
            }
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: IllegalStateException) {
            failures.add("smoke aborted: ${exception.message}")
            SMOKE_LOG.error("[applescript-smoke] aborted", exception)
        } catch (exception: IllegalArgumentException) {
            failures.add("smoke aborted: ${exception.message}")
            SMOKE_LOG.error("[applescript-smoke] aborted", exception)
        }

        // Close the opened project so the test environment exits cleanly. Errors
        // here do not affect the assertion outcome; cancellation still propagates.
        project?.let(::closeProjectAfterSmoke)

        return if (failures.isEmpty()) {
            SMOKE_LOG.info("[applescript-smoke] PASS")
            0
        } else {
            failures.forEach { SMOKE_LOG.error("[applescript-smoke] FAIL: $it") }
            1
        }
    }

    private fun openProjectOnEdt(fixturePath: Path): Project? {
        var project: Project? = null
        ApplicationManager.getApplication().invokeAndWait {
            project = openOrCreateProject(fixturePath)
        }
        return project
    }

    private fun collectProjectFailures(openedProject: Project): List<String> {
        val failures = mutableListOf<String>()
        val registry = AppleScriptSystemDictionaryRegistryService.getInstance()
        val appsReady = awaitAppsReady(registry)
        if (appsReady.isFailure) {
            failures.add(
                "dictionary registry did not become ready: " +
                    appsReady.exceptionOrNull()?.message,
            )
        } else {
            DumbService.getInstance(openedProject).waitForSmartMode()
            assertions.run(openedProject, fixtureDir, failures)
        }
        return failures
    }

    private fun awaitAppsReady(registry: AppleScriptSystemDictionaryRegistryService): Result<Unit> {
        val future = CompletableFuture<Result<Unit>>()
        val job =
            registry.serviceScope.launch(start = CoroutineStart.UNDISPATCHED) {
                future.complete(registry.awaitAppsReadyInternal())
            }
        job.invokeOnCompletion { cause ->
            if (cause != null) {
                future.complete(Result.failure(cause))
            }
        }

        return try {
            future.get()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            Result.failure(exception)
        } catch (exception: ExecutionException) {
            Result.failure(exception.cause ?: exception)
        }
    }

    private fun closeProjectAfterSmoke(project: Project) {
        try {
            ApplicationManager.getApplication().invokeAndWait {
                val fileEditorManager = FileEditorManager.getInstance(project)
                fileEditorManager.openFiles.forEach { file ->
                    fileEditorManager.closeFile(file)
                }
                ProjectManager.getInstance().closeAndDispose(project)
            }
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: IllegalStateException) {
            SMOKE_LOG.warn("[applescript-smoke] close project failed", exception)
        } catch (exception: IllegalArgumentException) {
            SMOKE_LOG.warn("[applescript-smoke] close project failed", exception)
        }
    }

    /**
     * Project bootstrap: [ProjectUtil.openOrImport] is the primary path (mirrors the
     * File -> Open lifecycle). Falls back to
     * [ProjectManagerEx.openProject] only if the primary returns null. Both options land
     * us with a real [Project] + [PsiManager] for the assertions.
     *
     * Public-API-only fallback: uses the no-arg static [OpenProjectTask.build] factory
     * (published in `ide-core-impl/api-dump.txt` — the vetted surface), NOT the internal
     * Kotlin-DSL `OpenProjectTask { ... }` extension that lives in
     * `com.intellij.ide.impl.OpenProjectTaskKt` and triggers `INTERNAL_API_USAGES`.
     */
    private fun openOrCreateProject(fixtureDir: Path): Project? {
        val primary =
            try {
                ProjectUtil.openOrImport(fixtureDir, null, true)
            } catch (exception: ProcessCanceledException) {
                throw exception
            } catch (exception: IllegalStateException) {
                SMOKE_LOG.warn("ProjectUtil.openOrImport threw for $fixtureDir", exception)
                null
            } catch (exception: IllegalArgumentException) {
                SMOKE_LOG.warn("ProjectUtil.openOrImport threw for $fixtureDir", exception)
                null
            }
        if (primary != null) return primary

        // Fallback path - useful if openOrImport semantics shift between platform minor
        // versions. OpenProjectTask.build() returns a default-configured task that
        // matches the previous `OpenProjectTask {}` empty-DSL call site (no
        // customizations needed for the smoke fixture).
        return try {
            ProjectManagerEx.getInstanceEx().openProject(fixtureDir, OpenProjectTask.build())
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: IllegalStateException) {
            SMOKE_LOG.warn("ProjectManagerEx.openProject fallback threw for $fixtureDir", exception)
            null
        } catch (exception: IllegalArgumentException) {
            SMOKE_LOG.warn("ProjectManagerEx.openProject fallback threw for $fixtureDir", exception)
            null
        }
    }
}
