package com.intellij.plugin.applescript.lang.dictionary.discovery

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.JDOMUtil
import org.jdom.Element
import org.jdom.JDOMException
import java.io.File
import java.io.IOException

private const val CF_BUNDLE_IDENTIFIER_KEY = "CFBundleIdentifier"
private const val INFO_PLIST_PATH = "Contents/Info.plist"
private const val PLIST_SIGNATURE_BUFFER_SIZE = 64

internal object ApplicationBundleInfoReader {
    private val LOG: Logger = Logger.getInstance("#${ApplicationBundleInfoReader::class.java.name}")

    fun readBundleIdentifier(applicationBundleFile: File): String? {
        val infoPlist = File(applicationBundleFile, INFO_PLIST_PATH)
        val dictionary =
            if (!infoPlist.exists() || infoPlist.isDirectory || !isXmlPlist(infoPlist)) {
                null
            } else {
                try {
                    JDOMUtil.load(infoPlist)
                } catch (e: JDOMException) {
                    logInfoPlistParseFailure(applicationBundleFile, e)
                    null
                } catch (e: IOException) {
                    logInfoPlistParseFailure(applicationBundleFile, e)
                    null
                }
            }

        return dictionary?.getChild("dict")?.findStringValue(CF_BUNDLE_IDENTIFIER_KEY)
    }

    private fun logInfoPlistParseFailure(
        applicationBundleFile: File,
        throwable: Throwable,
    ) {
        LOG.warn("Cannot parse Info.plist for ${applicationBundleFile.nameWithoutExtension}", throwable)
    }

    private fun isXmlPlist(infoPlist: File): Boolean =
        try {
            infoPlist.inputStream().buffered().use { input ->
                val signatureBuffer = ByteArray(PLIST_SIGNATURE_BUFFER_SIZE)
                val bytesRead = input.read(signatureBuffer)
                val signature = String(signatureBuffer, 0, bytesRead.coerceAtLeast(0)).trimStart()
                bytesRead > 0 && (signature.startsWith("<?xml") || signature.startsWith("<plist"))
            }
        } catch (e: IOException) {
            LOG.debug("Cannot read Info.plist signature for ${infoPlist.parentFile?.nameWithoutExtension}", e)
            false
        }
}

private fun Element.findStringValue(keyName: String): String? {
    children.forEachIndexed { keyIndex, element ->
        val isTargetKey = element.name == "key" && element.textTrim == keyName
        if (isTargetKey) {
            val value = children.getOrNull(keyIndex + 1)
            return value?.takeIf { it.name == "string" }?.textTrim?.takeIf { it.isNotBlank() }
        }
    }
    return null
}
