package com.intellij.plugin.applescript.lang.dictionary.icons

import com.github.markusbernhardt.proxy.util.PListParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ui.JBImageIcon
import com.intellij.util.ui.JBUI
import org.apache.commons.imaging.ImageReadException
import org.apache.commons.imaging.formats.icns.IcnsImageParser
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.swing.Icon

@Suppress("SpellCheckingInspection")
private const val ICON_FILE_EXTENSION = "icns"

private const val DICTIONARY_ICON_SIZE = 13
private const val INFO_PLIST_PATH = "Contents/Info.plist"
private const val ICON_RESOURCES_PATH = "Contents/Resources"

private val LOG: Logger = Logger.getInstance("#${DictionaryIconLoader::class.java.name}")

internal object DictionaryIconLoader {
    fun loadFromBundle(
        applicationBundleFile: File,
        applicationName: String,
    ): Icon? {
        val infoPlist = File(applicationBundleFile, INFO_PLIST_PATH)
        val iconFileName = loadIconFileName(infoPlist, applicationName) ?: applicationName
        val iconFile = File(applicationBundleFile, "$ICON_RESOURCES_PATH/${iconFileName.withIconExtension()}")
        return if (iconFile.exists() && !iconFile.isDirectory) {
            loadIconFromFile(iconFile, applicationName)
        } else {
            null
        }
    }
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
}

private fun String.withIconExtension(): String = if (hasIconExtension()) this else "$this.$ICON_FILE_EXTENSION"

private fun String.hasIconExtension(): Boolean = endsWith(".$ICON_FILE_EXTENSION")

private fun loadIconFromFile(
    iconFile: File,
    applicationName: String,
): Icon? =
    try {
        createScaledIcon(IcnsImageParser().getAllBufferedImages(iconFile))
    } catch (e: ImageReadException) {
        logIconLoadFailure(applicationName, iconFile, e)
        null
    } catch (e: IOException) {
        logIconLoadFailure(applicationName, iconFile, e)
        null
    }

private fun createScaledIcon(images: List<BufferedImage>?): Icon? {
    if (images.isNullOrEmpty()) {
        return null
    }
    val iconSize = JBUI.scale(DICTIONARY_ICON_SIZE)
    val image: Image = images.last().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)
    return JBImageIcon(image)
}

private fun logInfoPlistParseFailure(
    applicationName: String,
    throwable: Throwable,
) {
    LOG.warn("Cannot parse Info.plist for $applicationName", throwable)
}

private fun logIconLoadFailure(
    applicationName: String,
    iconFile: File,
    throwable: Throwable,
) {
    LOG.warn(
        "Cannot load dictionary icon for $applicationName from ${iconFile.path}",
        throwable,
    )
}
