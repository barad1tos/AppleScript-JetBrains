package com.intellij.plugin.applescript.lang.dictionary.icons

import com.intellij.openapi.util.io.FileUtil
import java.io.File
import javax.swing.Icon

private const val MAX_RECURSIVE_ICON_SCAN_DEPTH = 10
private const val MIN_RECURSIVE_ICON_SCORE = 30
private const val NATIVE_OVERRIDE_ICON_SCORE = 100
private const val MAIN_RESOURCE_SCORE = 50
private const val NESTED_RESOURCE_SCORE = 20
private const val APP_EXTENSION_SCORE = -40
private const val PLUGIN_DIRECTORY_SCORE = -20
private const val ICNS_EXTENSION_SCORE = 20
private const val RASTER_EXTENSION_SCORE = 10
private const val APP_ICON_NAME_SCORE = 80
private const val EXACT_APP_NAME_SCORE = 50
private const val CONTAINS_APP_NAME_SCORE = 30
private const val EXACT_ICON_NAME_SCORE = 30
private const val CONTAINS_ICON_NAME_SCORE = 20
private const val APPLICATION_ICON_NAME_SCORE = 15
private const val DISCOURAGED_ICON_HINT_SCORE = -100

private val DISCOURAGED_ICON_HINTS =
    setOf(
        "badge",
        "database",
        "document",
        "extension",
        "generic",
        "mask",
        "placeholder",
        "reminders",
        "share",
        "sidebar",
        "symbol",
        "template",
        "toolbar",
        "widget",
    )

internal data class BundleIconCandidate(
    val icon: Icon,
    val score: Int,
    val depth: Int,
) {
    val shouldOverrideNativeIcon: Boolean
        get() = score >= NATIVE_OVERRIDE_ICON_SCORE
}

internal object BundleIconCandidateResolver {
    fun find(
        applicationBundleFile: File,
        applicationName: String,
        loadIcon: (File) -> Icon?,
    ): BundleIconCandidate? {
        val scorer = BundleIconCandidateScorer(applicationBundleFile, applicationName)
        return applicationBundleFile
            .walkTopDown()
            .onEnter { directory -> shouldScanDirectory(applicationBundleFile, directory) }
            .filter { candidateFile ->
                candidateFile.isFile &&
                    candidateFile.extension.lowercase() in SUPPORTED_ICON_EXTENSIONS
            }.mapNotNull { candidateFile ->
                loadCandidate(applicationBundleFile, candidateFile, scorer, loadIcon)
            }.maxWithOrNull(
                compareBy<BundleIconCandidate> { it.score }
                    .thenBy { -it.depth },
            )
    }

    private fun loadCandidate(
        applicationBundleFile: File,
        iconFile: File,
        scorer: BundleIconCandidateScorer,
        loadIcon: (File) -> Icon?,
    ): BundleIconCandidate? {
        val score = scorer.score(iconFile)
        if (score < MIN_RECURSIVE_ICON_SCORE) {
            return null
        }
        val icon = loadIcon(iconFile) ?: return null
        return BundleIconCandidate(
            icon = icon,
            score = score,
            depth = iconFile.relativeTo(applicationBundleFile).toPath().nameCount,
        )
    }

    private fun shouldScanDirectory(
        applicationBundleFile: File,
        directory: File,
    ): Boolean {
        if (FileUtil.filesEqual(directory, applicationBundleFile)) {
            return true
        }
        val depth = directory.relativeTo(applicationBundleFile).toPath().nameCount
        if (depth > MAX_RECURSIVE_ICON_SCAN_DEPTH) {
            return false
        }
        return directory.name != "MacOS" &&
            directory.name != "_CodeSignature" &&
            !directory.name.endsWith(".lproj")
    }
}

private class BundleIconCandidateScorer(
    private val applicationBundleFile: File,
    applicationName: String,
) {
    private val applicationTokens =
        applicationName
            .normalizedIconToken()
            .let { normalizedName ->
                setOf(
                    normalizedName,
                    normalizedName.removeSuffix("app"),
                    normalizedName.trimEnd(Char::isDigit),
                )
            }.filter { token -> token.length > 2 }
            .toSet()

    fun score(iconFile: File): Int {
        val relativePath = iconFile.relativeTo(applicationBundleFile).invariantSeparatorsPath
        val lowercasePath = relativePath.lowercase()
        val normalizedName = iconFile.nameWithoutExtension.normalizedIconToken()
        var score = scoreResourceLocation(relativePath, lowercasePath)
        score +=
            if (iconFile.extension.equals(ICON_FILE_EXTENSION, ignoreCase = true)) {
                ICNS_EXTENSION_SCORE
            } else {
                RASTER_EXTENSION_SCORE
            }
        score += scoreName(normalizedName)
        if (isDiscouraged(lowercasePath, normalizedName)) {
            score += DISCOURAGED_ICON_HINT_SCORE
        }
        return score
    }

    private fun scoreResourceLocation(
        relativePath: String,
        lowercasePath: String,
    ): Int {
        var score = 0
        if (relativePath.startsWith("$ICON_RESOURCES_PATH/")) {
            score += MAIN_RESOURCE_SCORE
        } else if (relativePath.contains("/$ICON_RESOURCES_PATH/")) {
            score += NESTED_RESOURCE_SCORE
        }
        if (lowercasePath.contains(".appex/")) {
            score += APP_EXTENSION_SCORE
        }
        if (lowercasePath.contains("/plugins/")) {
            score += PLUGIN_DIRECTORY_SCORE
        }
        return score
    }

    private fun scoreName(normalizedName: String): Int {
        var score = 0
        if (normalizedName == "appicon") {
            score += APP_ICON_NAME_SCORE
        }
        if (normalizedName == "icon") {
            score += EXACT_ICON_NAME_SCORE
        } else if (normalizedName.contains("icon")) {
            score += CONTAINS_ICON_NAME_SCORE
        }
        if (normalizedName == "application" || normalizedName == "app") {
            score += APPLICATION_ICON_NAME_SCORE
        }
        score += scoreApplicationNameMatch(normalizedName)
        return score
    }

    private fun scoreApplicationNameMatch(normalizedName: String): Int =
        when {
            applicationTokens.any { applicationToken ->
                normalizedName == applicationToken
            } -> EXACT_APP_NAME_SCORE
            applicationTokens.any { applicationToken ->
                normalizedName.contains(applicationToken)
            } -> CONTAINS_APP_NAME_SCORE
            else -> 0
        }

    private fun isDiscouraged(
        lowercasePath: String,
        normalizedName: String,
    ): Boolean =
        DISCOURAGED_ICON_HINTS.any { hint ->
            lowercasePath.contains(hint) || normalizedName.contains(hint)
        }
}

private fun String.normalizedIconToken(): String =
    lowercase()
        .filter { character -> character.isLetterOrDigit() }
