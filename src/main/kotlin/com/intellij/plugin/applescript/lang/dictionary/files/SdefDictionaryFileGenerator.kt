package com.intellij.plugin.applescript.lang.dictionary.files

import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.discovery.DeveloperToolsNotInstalledException
import com.intellij.plugin.applescript.lang.dictionary.discovery.NotScriptableApplicationException
import com.intellij.plugin.applescript.lang.dictionary.discovery.XcodeDetectionService
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val LOG: Logger = Logger.getInstance("#${SdefDictionaryFileGenerator::class.java.name}")

private const val DICTIONARY_GENERATION_TIMEOUT_SECONDS: Long = 5L

private val DICTIONARY_FILE_EXTENSIONS: Set<String> = setOf("xml", "sdef")

internal object SdefDictionaryFileGenerator {
    fun generateDictionaryFile(
        applicationName: String,
        applicationIoFile: File,
        generateCacheFile: (() -> Boolean)? = null,
    ): File? {
        val isDictionaryFile = applicationIoFile.extension in DICTIONARY_FILE_EXTENSIONS
        val serializePath = serializeDictionaryPathForApplication(applicationName)
        val targetFile = File(serializePath)
        val parentDirectoryReady = targetFile.parentFile.exists() || targetFile.parentFile.mkdirs()
        if ((!SystemInfo.isMac && !isDictionaryFile) || !parentDirectoryReady) return null

        LOG.debug("=== Caching Dictionary for application [$applicationName] ===")
        var isGenerated = false
        try {
            isGenerated =
                generateCacheFile?.invoke()
                    ?: writeCacheFile(
                        DictionaryGenerationRequest(
                            applicationName,
                            applicationIoFile,
                            targetFile,
                            serializePath,
                            isDictionaryFile,
                        ),
                    )
        } finally {
            if (!isGenerated) {
                LOG.warn("Error occurred while generating file.")
                if (targetFile.delete()) LOG.warn("Created file was deleted")
            }
        }

        return targetFile.takeIf { isGenerated && it.exists() }
    }

    fun recoverDictionaryFile(
        applicationName: String,
        applicationIoFile: File,
        copyDictionaryFile: (String, File, File, Boolean) -> Boolean = ::copyDictionaryFileToCacheDir,
    ): File? {
        LOG.warn("Will try to find application scripting definition file...")
        val sdefFile = findSdefForApplication(applicationIoFile)
        if (sdefFile == null || !sdefFile.exists()) {
            LOG.warn("Scripting definition was not found for application ${applicationIoFile.absolutePath}")
            return null
        }
        val targetFile = File(serializeDictionaryPathForApplication(applicationName))
        if (!copyDictionaryFile(applicationName, sdefFile, targetFile, true)) {
            throw IOException("Failed to cache bundled dictionary for $applicationName")
        }
        return targetFile
    }

    private fun writeCacheFile(request: DictionaryGenerationRequest): Boolean =
        if (SystemInfo.isMac) {
            val cmdName = if (request.isDictionaryFile) "cat" else "sdef"
            doGenerateDictionaryFile(
                request.applicationName,
                request.serializePath,
                cmdName,
                request.applicationIoFile.path,
            )
        } else {
            copyDictionaryFileToCacheDir(
                request.applicationName,
                request.applicationIoFile,
                request.targetFile,
                true,
            )
        }

    @Throws(
        NotScriptableApplicationException::class,
        DeveloperToolsNotInstalledException::class,
        InterruptedException::class,
        IOException::class,
    )
    private fun doGenerateDictionaryFile(
        applicationName: String,
        serializePath: String,
        cmdName: String,
        appFilePath: String,
    ): Boolean {
        val shellCommand = arrayOf("/bin/bash", "-c", " $cmdName \"$appFilePath\" > $serializePath")
        LOG.debug("executing command: ${shellCommand.contentToString()}")
        val execStart = System.currentTimeMillis()
        val process = Runtime.getRuntime().exec(shellCommand)
        val isFinished = waitForProcess(process, DICTIONARY_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val execEnd = System.currentTimeMillis()
        if (!isFinished) {
            if (service<XcodeDetectionService>().isXcodeInstalled()) {
                throw NotScriptableApplicationException(
                    applicationName,
                    "Waiting time elapsed for command ${shellCommand.contentToString()}. " +
                        "Seems that application \"$applicationName\" is not scriptable.",
                )
            } else {
                throw DeveloperToolsNotInstalledException()
            }
        }
        LOG.debug("Waiting time elapsed. Execution time: ${execEnd - execStart} ms.")
        return true
    }

    private fun findSdefForApplication(applicationIoFile: File): File? {
        val appResources = File(applicationIoFile, "/Contents/Resources")
        val files = appResources.listFiles { _, fileName -> fileName.endsWith("sdef") }
        return if (!files.isNullOrEmpty()) files[0] else null
    }
}

private data class DictionaryGenerationRequest(
    val applicationName: String,
    val applicationIoFile: File,
    val targetFile: File,
    val serializePath: String,
    val isDictionaryFile: Boolean,
)

internal fun waitForProcess(
    process: Process,
    timeout: Long,
    timeUnit: TimeUnit,
    killTree: (Process) -> Boolean = OSProcessUtil::killProcessTree,
): Boolean =
    try {
        process.waitFor(timeout, timeUnit).also { isFinished ->
            if (!isFinished) terminateProcessTree(process, killTree)
        }
    } catch (interruption: InterruptedException) {
        terminateProcessTree(process, killTree).forEach(interruption::addSuppressed)
        throw interruption
    }

private fun terminateProcessTree(
    process: Process,
    killTree: (Process) -> Boolean,
): List<Throwable> {
    val issues = mutableListOf<TerminationIssue>()
    val platformKill = processAttempt { killTree(process) }
    platformKill.exceptionOrNull()?.let { issues += TerminationIssue("platform tree kill failed", it) }
    if (platformKill.getOrDefault(false)) return emptyList()

    val descendantLookup = processAttempt { process.descendants().use { it.toList().asReversed() } }
    descendantLookup.exceptionOrNull()?.let { issues += TerminationIssue("descendant discovery failed", it) }
    val descendants = descendantLookup.getOrDefault(emptyList())
    descendants.mapNotNullTo(issues, ::terminateHandle)

    val parentLookup = processAttempt(process::toHandle)
    parentLookup.exceptionOrNull()?.let { issues += TerminationIssue("parent handle lookup failed", it) }
    val parent = parentLookup.getOrNull()
    if (parent != null) {
        terminateHandle(parent)?.let(issues::add)
    }

    logTerminationIssues(process, issues)
    return issues.mapNotNull(TerminationIssue::cause)
}

private fun terminateHandle(handle: ProcessHandle): TerminationIssue? {
    val termination =
        processAttempt {
            if (!handle.destroyForcibly() && handle.isAlive) {
                TerminationIssue("forced termination was rejected for PID ${handle.pid()}")
            } else {
                null
            }
        }
    return termination.getOrNull()
        ?: termination.exceptionOrNull()?.let {
            TerminationIssue("forced termination failed for PID ${handle.pid()}", it)
        }
}

private fun logTerminationIssues(
    process: Process,
    issues: List<TerminationIssue>,
) {
    if (issues.isEmpty()) return

    val processId = processAttempt { process.pid().toString() }.getOrDefault("unknown")
    val message =
        "Dictionary process-tree termination was incomplete for PID $processId: " +
            issues.joinToString { it.message }
    val primaryCause = issues.firstNotNullOfOrNull(TerminationIssue::cause)
    if (primaryCause == null) {
        LOG.warn(message)
    } else {
        LOG.warn(message, primaryCause)
    }
}

private data class TerminationIssue(
    val message: String,
    val cause: Throwable? = null,
)

private inline fun <T> processAttempt(action: () -> T): Result<T> =
    try {
        Result.success(action())
    } catch (failure: IllegalStateException) {
        Result.failure(failure)
    } catch (failure: IllegalArgumentException) {
        Result.failure(failure)
    } catch (failure: UnsupportedOperationException) {
        Result.failure(failure)
    } catch (failure: SecurityException) {
        Result.failure(failure)
    }
