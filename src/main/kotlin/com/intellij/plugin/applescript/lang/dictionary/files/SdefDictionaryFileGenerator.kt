package com.intellij.plugin.applescript.lang.dictionary.files

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.dictionary.discovery.DeveloperToolsNotInstalledException
import com.intellij.plugin.applescript.lang.dictionary.discovery.NotScriptableApplicationException
import com.intellij.plugin.applescript.lang.dictionary.discovery.XcodeDetectionService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val LOG: Logger = Logger.getInstance("#${SdefDictionaryFileGenerator::class.java.name}")

private const val DICTIONARY_GENERATION_TIMEOUT_SECONDS: Long = 5L

private val DICTIONARY_FILE_EXTENSIONS: Set<String> = setOf("xml", "sdef")

internal object SdefDictionaryFileGenerator {
    fun createDictionaryInfoForApplication(
        applicationName: String,
        applicationIoFile: File,
    ): DictionaryInfo? {
        fun finishGeneration(
            targetFile: File,
            fileGenerated: Boolean,
        ) {
            if (!fileGenerated) {
                LOG.warn("Error occurred while generating file.")
                if (targetFile.delete()) LOG.warn("Created file was deleted")
            } else if (ApplicationDiscoveryService.getInstance().removeFromNotFoundList(applicationName)) {
                LOG.debug("Application was removed from ignored list")
            }
        }

        val appExtension = applicationIoFile.extension
        val isDictionaryFile = appExtension in DICTIONARY_FILE_EXTENSIONS
        val serializePath = serializeDictionaryPathForApplication(applicationName)
        val targetFile = File(serializePath)
        val parentDirectoryReady = targetFile.parentFile.exists() || targetFile.parentFile.mkdirs()
        if ((!SystemInfo.isMac && !isDictionaryFile) || !parentDirectoryReady) return null

        LOG.debug("=== Caching Dictionary for application [$applicationName] ===")
        var fileGenerated = false
        try {
            fileGenerated =
                generateDictionaryFileForApplication(
                    DictionaryGenerationRequest(
                        applicationName,
                        applicationIoFile,
                        targetFile,
                        serializePath,
                        isDictionaryFile,
                    ),
                )
        } catch (e: NotScriptableApplicationException) {
            LOG.warn("Generation failed: ${e.message}. Adding to ignore list")
            service<SdefPersistenceService>().addNotScriptable(e.applicationName)
        } catch (e: DeveloperToolsNotInstalledException) {
            LOG.warn("Generation failed: ${e.message}")
            fileGenerated =
                recoverDictionaryFileFromBundledSdef(
                    applicationName,
                    applicationIoFile,
                    targetFile,
                )
        } finally {
            finishGeneration(targetFile, fileGenerated)
        }

        return targetFile
            .takeIf { fileGenerated && it.exists() }
            ?.let { registerGeneratedDictionaryInfo(applicationName, it, applicationIoFile, appExtension) }
    }

    private fun generateDictionaryFileForApplication(request: DictionaryGenerationRequest): Boolean =
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

    @Throws(NotScriptableApplicationException::class, DeveloperToolsNotInstalledException::class)
    private fun doGenerateDictionaryFile(
        applicationName: String,
        serializePath: String,
        cmdName: String,
        appFilePath: String,
    ): Boolean {
        fun logFailure(cause: Throwable) {
            LOG.error(
                "Failed to create dictionary file for application [$applicationName] Command:$cmdName " +
                    "target path: $appFilePath Reason: ${cause.cause}",
                cause,
            )
        }

        var isGenerated = false
        try {
            val shellCommand = arrayOf("/bin/bash", "-c", " $cmdName \"$appFilePath\" > $serializePath")
            LOG.debug("executing command: ${shellCommand.contentToString()}")
            val execStart = System.currentTimeMillis()
            val exitStatus =
                Runtime
                    .getRuntime()
                    .exec(shellCommand)
                    .waitFor(DICTIONARY_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            val execEnd = System.currentTimeMillis()
            if (!exitStatus) {
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
            isGenerated = true
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logFailure(e)
        } catch (e: IOException) {
            logFailure(e)
        }
        return isGenerated
    }

    private fun recoverDictionaryFileFromBundledSdef(
        applicationName: String,
        applicationIoFile: File,
        targetFile: File,
    ): Boolean {
        LOG.warn("Will try to find application scripting definition file...")
        val sdefFile = findSdefForApplication(applicationIoFile)
        return if (sdefFile != null && sdefFile.exists()) {
            copyDictionaryFileToCacheDir(applicationName, sdefFile, targetFile, true)
        } else {
            LOG.warn(
                "Scripting definition was not found for application " +
                    applicationIoFile.absolutePath,
            )
            service<SdefPersistenceService>().addNotScriptable(applicationName)
            false
        }
    }

    private fun findSdefForApplication(applicationIoFile: File): File? {
        val appResources = File(applicationIoFile, "/Contents/Resources")
        val files = appResources.listFiles { _, fileName -> fileName.endsWith("sdef") }
        return if (!files.isNullOrEmpty()) files[0] else null
    }

    private fun registerGeneratedDictionaryInfo(
        applicationName: String,
        targetFile: File,
        applicationIoFile: File,
        appExtension: String,
    ): DictionaryInfo {
        val applicationBundle =
            applicationIoFile.takeIf {
                ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS.contains(appExtension)
            }
        val dictionaryInfo = DictionaryInfo(applicationName, targetFile, applicationBundle)
        service<SdefPersistenceService>().addDictionaryInfo(dictionaryInfo)
        LOG.debug("Dictionary file generated for application [$applicationName]$targetFile")
        return dictionaryInfo
    }
}

private data class DictionaryGenerationRequest(
    val applicationName: String,
    val applicationIoFile: File,
    val targetFile: File,
    val serializePath: String,
    val isDictionaryFile: Boolean,
)
