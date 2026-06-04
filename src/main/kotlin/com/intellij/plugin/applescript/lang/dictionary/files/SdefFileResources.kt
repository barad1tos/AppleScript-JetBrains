package com.intellij.plugin.applescript.lang.dictionary.files

import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
