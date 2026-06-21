package com.intellij.plugin.applescript.lang.dictionary.icons

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ui.JBImageIcon
import com.intellij.util.ui.JBUI
import org.apache.commons.imaging.ImageReadException
import org.apache.commons.imaging.formats.icns.IcnsImageParser
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.Icon

internal const val ICON_FILE_EXTENSION = "icns"
internal const val ICON_RESOURCES_PATH = "Contents/Resources"

private const val PNG_ICON_EXTENSION = "png"
private const val TIFF_ICON_EXTENSION = "tiff"
private const val SHORT_TIFF_ICON_EXTENSION = "tif"
private const val JPEG_ICON_EXTENSION = "jpg"
private const val LONG_JPEG_ICON_EXTENSION = "jpeg"
private const val DICTIONARY_ICON_SIZE = 13

private val RASTER_ICON_EXTENSIONS =
    setOf(
        PNG_ICON_EXTENSION,
        TIFF_ICON_EXTENSION,
        SHORT_TIFF_ICON_EXTENSION,
        JPEG_ICON_EXTENSION,
        LONG_JPEG_ICON_EXTENSION,
    )
internal val SUPPORTED_ICON_EXTENSIONS = RASTER_ICON_EXTENSIONS + ICON_FILE_EXTENSION

private val LOG: Logger = Logger.getInstance("#${DictionaryIconFileLoader::class.java.name}")

internal object DictionaryIconFileLoader {
    fun loadResourceIcon(
        applicationBundleFile: File,
        iconFileName: String,
        applicationName: String,
    ): Icon? =
        iconFileName
            .iconResourceFileNames()
            .asSequence()
            .map { iconResourceFileName ->
                File(applicationBundleFile, "$ICON_RESOURCES_PATH/$iconResourceFileName")
            }.firstNotNullOfOrNull { iconFile ->
                iconFile
                    .takeIf { it.exists() && !it.isDirectory }
                    ?.let { loadCandidateIcon(it, applicationName) }
            }

    fun loadCandidateIcon(
        iconFile: File,
        applicationName: String,
    ): Icon? =
        when (iconFile.extension.lowercase()) {
            ICON_FILE_EXTENSION -> loadIcnsIcon(iconFile, applicationName)
            in RASTER_ICON_EXTENSIONS -> loadRasterIcon(iconFile, applicationName)
            else -> null
        }

    fun createScaledIcon(image: BufferedImage): Icon {
        val iconSize = JBUI.scale(DICTIONARY_ICON_SIZE)
        val scaledImage: Image = image.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)
        return JBImageIcon(scaledImage)
    }
}

private fun String.iconResourceFileNames(): List<String> =
    if (hasSupportedIconExtension()) {
        listOf(this)
    } else {
        listOf(
            withIconExtension(),
            "$this.$PNG_ICON_EXTENSION",
            "$this.$TIFF_ICON_EXTENSION",
            "$this.$SHORT_TIFF_ICON_EXTENSION",
        )
    }

private fun String.hasSupportedIconExtension(): Boolean =
    substringAfterLast('.', "").lowercase() in SUPPORTED_ICON_EXTENSIONS

private fun String.withIconExtension(): String =
    if (endsWith(".$ICON_FILE_EXTENSION")) this else "$this.$ICON_FILE_EXTENSION"

private fun loadIcnsIcon(
    iconFile: File,
    applicationName: String,
): Icon? =
    try {
        IcnsImageParser()
            .getAllBufferedImages(iconFile)
            ?.lastOrNull()
            ?.let(DictionaryIconFileLoader::createScaledIcon)
    } catch (e: ImageReadException) {
        logIconLoadFailure(applicationName, iconFile, e)
        null
    } catch (e: IOException) {
        logIconLoadFailure(applicationName, iconFile, e)
        null
    }

private fun loadRasterIcon(
    iconFile: File,
    applicationName: String,
): Icon? =
    try {
        ImageIO.read(iconFile)?.let(DictionaryIconFileLoader::createScaledIcon)
    } catch (e: IOException) {
        logIconLoadFailure(applicationName, iconFile, e)
        null
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
