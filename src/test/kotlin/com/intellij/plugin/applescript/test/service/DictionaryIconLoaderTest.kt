package com.intellij.plugin.applescript.test.service

import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.icons.DictionaryIconLoader
import com.intellij.ui.mac.foundation.Foundation
import com.intellij.ui.mac.foundation.ID
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView

class DictionaryIconLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loadFromBundle falls back to system icon when icns resource is missing`() {
        val applicationBundle = tempDir.resolve("AssetCatalogOnly.app")
        val contents = applicationBundle.resolve("Contents")
        Files.createDirectories(contents.resolve("Resources"))
        contents
            .resolve("Info.plist")
            .toFile()
            .writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <plist version="1.0"><dict><key>CFBundleIconFile</key><string>AppIcon</string></dict></plist>
                """.trimIndent(),
            )

        assertNotNull(DictionaryIconLoader.loadFromBundle(applicationBundle.toFile(), "AssetCatalogOnly"))
    }

    @Test
    fun `loadFromBundle avoids generic system icon for asset catalog application bundles`() {
        assumeTrue(SystemInfo.isMac)
        val applicationBundle = File("/Applications/Things3.app")
        assumeTrue(applicationBundle.isDirectory)
        assumeFalse(File(applicationBundle, "Contents/Resources/AppIcon.icns").exists())

        val loadedIcon = requireNotNull(DictionaryIconLoader.loadFromBundle(applicationBundle, "Things3"))
        val genericSystemIcon = FileSystemView.getFileSystemView().getSystemIcon(applicationBundle)

        assertFalse(
            loadedIcon.renderedPixelsEqual(genericSystemIcon),
            "Asset-catalog-only app bundles must use the native app icon, not FileSystemView's generic .app icon",
        )
    }

    @Test
    fun `loadFromBundle prefers bundle AppIcon asset over native workspace icon`() {
        assumeTrue(SystemInfo.isMac)
        val applicationBundle = File("/Applications/Things3.app")
        assumeTrue(applicationBundle.isDirectory)

        val loadedIcon = requireNotNull(DictionaryIconLoader.loadFromBundle(applicationBundle, "Things3"))
        val bundleAppIcon = requireNotNull(loadBundleImageResourceIcon(applicationBundle, "AppIcon", loadedIcon))

        assertTrue(
            loadedIcon.renderedPixelsEqual(bundleAppIcon),
            "Asset-catalog app bundles should use the named AppIcon resource before NSWorkspace fallback",
        )
    }

    @Test
    fun `loadFromBundle recursively uses app resource icon before nested extension icon`() {
        val applicationBundle = tempDir.resolve("RecursiveIcon.app")
        val appResources = applicationBundle.resolve("Contents/Resources")
        val extensionResources =
            applicationBundle.resolve("Contents/PlugIns/RecursiveShareExtension.appex/Contents/Resources")
        Files.createDirectories(appResources)
        Files.createDirectories(extensionResources)
        appResources.resolve("AppIcon.png").writeSolidIcon(Color.RED)
        extensionResources.resolve("icon.png").writeSolidIcon(Color.BLUE)
        applicationBundle
            .resolve("Contents/Info.plist")
            .toFile()
            .writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <plist version="1.0"><dict><key>CFBundleIconFile</key><string>MissingIcon</string></dict></plist>
                """.trimIndent(),
            )

        val loadedIcon =
            requireNotNull(DictionaryIconLoader.loadFromBundle(applicationBundle.toFile(), "RecursiveIcon"))

        assertIconCenterColor(Color.RED, loadedIcon)
    }
}

private fun Icon.renderedPixelsEqual(other: Icon): Boolean {
    val left = renderIcon()
    val right = other.renderIcon()
    if (left.width != right.width || left.height != right.height) return false

    for (x in 0 until left.width) {
        for (y in 0 until left.height) {
            if (left.getRGB(x, y) != right.getRGB(x, y)) return false
        }
    }
    return true
}

private fun Icon.renderIcon(): BufferedImage {
    val image =
        BufferedImage(
            iconWidth.coerceAtLeast(1),
            iconHeight.coerceAtLeast(1),
            BufferedImage.TYPE_INT_ARGB,
        )
    val graphics = image.createGraphics()
    paintIcon(null, graphics, 0, 0)
    graphics.dispose()
    return image
}

private fun loadBundleImageResourceIcon(
    applicationBundle: File,
    resourceName: String,
    targetIcon: Icon,
): Icon? {
    val nativeImage = loadBundleImageResource(applicationBundle, resourceName) ?: return null
    val iconFile = File.createTempFile("applescript-test-bundle-icon-", ".tiff")
    return try {
        val imageData = Foundation.invoke(nativeImage, "TIFFRepresentation")
        if (Foundation.isNil(imageData)) {
            return null
        }
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
        val scaledImage =
            image.getScaledInstance(
                targetIcon.iconWidth.coerceAtLeast(1),
                targetIcon.iconHeight.coerceAtLeast(1),
                Image.SCALE_SMOOTH,
            )
        ImageIcon(scaledImage)
    } catch (_: IOException) {
        null
    } finally {
        if (!iconFile.delete()) {
            iconFile.deleteOnExit()
        }
    }
}

private fun loadBundleImageResource(
    applicationBundle: File,
    resourceName: String,
): ID? {
    val bundle =
        Foundation.invoke(
            "NSBundle",
            "bundleWithPath:",
            Foundation.nsString(applicationBundle.path),
        )
    if (Foundation.isNil(bundle)) {
        return null
    }
    val nativeImage =
        Foundation.invoke(
            bundle,
            "imageForResource:",
            Foundation.nsString(resourceName),
        )
    return nativeImage.takeUnless(Foundation::isNil)
}

private fun Path.writeSolidIcon(color: Color) {
    val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.color = color
    graphics.fillRect(0, 0, image.width, image.height)
    graphics.dispose()
    ImageIO.write(image, "png", toFile())
}

private fun assertIconCenterColor(
    expectedColor: Color,
    icon: Icon,
) {
    val image = icon.renderIcon()
    val actualColor = Color(image.getRGB(image.width / 2, image.height / 2), true)
    assertFalse(
        actualColor.rgb != expectedColor.rgb,
        "Expected icon center color ${expectedColor.rgb}, got ${actualColor.rgb}",
    )
}
