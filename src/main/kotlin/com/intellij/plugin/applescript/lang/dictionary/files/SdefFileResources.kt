package com.intellij.plugin.applescript.lang.dictionary.files

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val RESOURCE_LOG = Logger.getInstance("#SdefFileResources")
private val GENERATED_DICTIONARIES_SYSTEM_FOLDER: String = "${PathManager.getSystemPath()}/sdef"

internal fun scriptingAdditionFiles(): Sequence<File> =
    ApplicationDictionary.SCRIPTING_ADDITIONS_FOLDERS
        .asSequence()
        .map(::File)
        .filter { directory -> directory.isDirectory }
        .flatMap { directory -> directory.listFiles()?.asSequence() ?: emptySequence() }

internal fun scriptingAdditionLibraryName(scriptingAdditionFile: File): String? {
    val fullName = scriptingAdditionFile.name
    val dotIndex = fullName.lastIndexOf('.')
    val endIndex = if (dotIndex > 0) dotIndex else fullName.length
    return fullName.substring(0, endIndex).takeUnless { libraryName -> libraryName.isEmpty() }
}

internal fun serializeDictionaryPathForApplication(applicationName: String): String {
    val unescaped = "$GENERATED_DICTIONARIES_SYSTEM_FOLDER/${applicationName}_generated.sdef"
    return unescaped.replace(" ", "_")
}

internal fun copyDictionaryFileToCacheDir(
    applicationName: String,
    applicationDictionaryFile: File,
    targetFile: File,
    rewrite: Boolean,
): Boolean {
    if (!targetFile.parentFile.exists()) return false

    val needsCopy = !targetFile.exists() || rewrite
    if (needsCopy) {
        try {
            if (targetFile.exists() && targetFile.delete()) {
                RESOURCE_LOG.debug("Existing target file deleted: $targetFile")
            }
            Files.copy(
                applicationDictionaryFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (e: IOException) {
            RESOURCE_LOG.error(
                "Failed to move file $applicationDictionaryFile to cache directory: $targetFile",
                e,
            )
        }
    } else {
        RESOURCE_LOG.debug("Generated file already exists for application $applicationName")
    }

    val fileMoved = targetFile.exists()
    if (fileMoved) {
        RESOURCE_LOG.debug("Dictionary file moved to ${targetFile.parent} directory")
    }
    return fileMoved
}

internal fun stream2file(
    input: InputStream?,
    prefix: String,
    suffix: String,
): File {
    val tempFile = File.createTempFile(prefix, suffix)
    tempFile.deleteOnExit()
    FileOutputStream(tempFile).use { out ->
        requireNotNull(input) { "InputStream for $prefix$suffix is null" }
            .use { inputStream ->
                inputStream.copyTo(out)
            }
    }
    tempFile.deleteOnExit()
    return tempFile
}
