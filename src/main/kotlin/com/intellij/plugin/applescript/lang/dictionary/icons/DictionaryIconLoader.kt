package com.intellij.plugin.applescript.lang.dictionary.icons

import com.github.markusbernhardt.proxy.util.PListParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.mac.foundation.Foundation
import com.intellij.ui.mac.foundation.ID
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView

private const val INFO_PLIST_PATH = "Contents/Info.plist"
private const val NATIVE_ICON_TEMP_FILE_PREFIX = "applescript-dictionary-icon-"
private const val NATIVE_ICON_TEMP_FILE_SUFFIX = ".tiff"

private val LOG: Logger = Logger.getInstance("#${DictionaryIconLoader::class.java.name}")

internal object DictionaryIconLoader {
    fun loadFromBundle(
        applicationBundleFile: File,
        applicationName: String,
    ): Icon? {
        val infoPlist = File(applicationBundleFile, INFO_PLIST_PATH)
        val iconFileName = loadIconFileName(infoPlist, applicationName) ?: applicationName
        DictionaryIconFileLoader.loadResourceIcon(applicationBundleFile, iconFileName, applicationName)?.let {
            return it
        }
        loadBundleImageResourceIcon(applicationBundleFile, iconFileName, applicationName)?.let {
            return it
        }

        val recursiveIconCandidate =
            BundleIconCandidateResolver.find(applicationBundleFile, applicationName) { iconFile ->
                DictionaryIconFileLoader.loadCandidateIcon(iconFile, applicationName)
            }
        val nativeIcon = loadNativeMacIconFromBundle(applicationBundleFile, applicationName)
        return when {
            recursiveIconCandidate?.shouldOverrideNativeIcon == true -> recursiveIconCandidate.icon
            nativeIcon != null -> nativeIcon
            recursiveIconCandidate != null -> recursiveIconCandidate.icon
            else -> loadFileSystemIconFromBundle(applicationBundleFile, applicationName)
        }
    }
}

private fun loadBundleImageResourceIcon(
    applicationBundleFile: File,
    iconFileName: String,
    applicationName: String,
): Icon? {
    if (!SystemInfo.isMac) {
        return null
    }

    return try {
        val bundle =
            Foundation.invoke(
                "NSBundle",
                "bundleWithPath:",
                Foundation.nsString(applicationBundleFile.path),
            )
        if (Foundation.isNil(bundle)) {
            return null
        }

        val nativeImage =
            Foundation.invoke(
                bundle,
                "imageForResource:",
                Foundation.nsString(iconFileName),
            )
        if (Foundation.isNil(nativeImage)) {
            return null
        }

        createIconFromNativeImage(nativeImage)
    } catch (e: RuntimeException) {
        logNativeIconLoadFailure(applicationName, applicationBundleFile, e)
        null
    } catch (e: IOException) {
        logNativeIconLoadFailure(applicationName, applicationBundleFile, e)
        null
    }
}

private fun loadNativeMacIconFromBundle(
    applicationBundleFile: File,
    applicationName: String,
): Icon? {
    if (!SystemInfo.isMac) {
        return null
    }

    return try {
        val workspace = Foundation.invoke("NSWorkspace", "sharedWorkspace")
        val nativeImage =
            Foundation.invoke(
                workspace,
                "iconForFile:",
                Foundation.nsString(applicationBundleFile.path),
            )
        if (Foundation.isNil(nativeImage)) {
            return null
        }

        createIconFromNativeImage(nativeImage)
    } catch (e: RuntimeException) {
        logNativeIconLoadFailure(applicationName, applicationBundleFile, e)
        null
    } catch (e: IOException) {
        logNativeIconLoadFailure(applicationName, applicationBundleFile, e)
        null
    }
}

private fun createIconFromNativeImage(nativeImage: ID): Icon? {
    val imageData = Foundation.invoke(nativeImage, "TIFFRepresentation")
    if (Foundation.isNil(imageData)) {
        return null
    }

    val iconFile = File.createTempFile(NATIVE_ICON_TEMP_FILE_PREFIX, NATIVE_ICON_TEMP_FILE_SUFFIX)
    try {
        val didWrite =
            Foundation
                .invoke(
                    imageData,
                    "writeToFile:atomically:",
                    Foundation.nsString(iconFile.path),
                    true,
                ).booleanValue()
        if (!didWrite) {
            return null
        }

        val image = ImageIO.read(iconFile) ?: return null
        return DictionaryIconFileLoader.createScaledIcon(image)
    } finally {
        if (!iconFile.delete()) {
            iconFile.deleteOnExit()
        }
    }
}

private fun loadFileSystemIconFromBundle(
    applicationBundleFile: File,
    applicationName: String,
): Icon? =
    try {
        FileSystemView.getFileSystemView().getSystemIcon(applicationBundleFile)
    } catch (e: RuntimeException) {
        LOG.warn(
            "Cannot load system icon for $applicationName from ${applicationBundleFile.path}",
            e,
        )
        null
    }

private fun loadIconFileName(
    infoPlist: File,
    applicationName: String,
): String? {
    if (!infoPlist.exists() || infoPlist.isDirectory) {
        return null
    }
    val dictionary =
        try {
            PListParser.load(infoPlist)
        } catch (e: PListParser.XmlParseException) {
            logInfoPlistParseFailure(applicationName, e)
            null
        } catch (e: IOException) {
            logInfoPlistParseFailure(applicationName, e)
            null
        }
    return dictionary?.get("CFBundleIconFile")?.toString()
        ?: dictionary?.get("CFBundleIconName")?.toString()
}

private fun logInfoPlistParseFailure(
    applicationName: String,
    throwable: Throwable,
) {
    LOG.warn("Cannot parse Info.plist for $applicationName", throwable)
}

private fun logNativeIconLoadFailure(
    applicationName: String,
    applicationBundleFile: File,
    throwable: Throwable,
) {
    LOG.warn(
        "Cannot load native macOS icon for $applicationName from ${applicationBundleFile.path}",
        throwable,
    )
}
