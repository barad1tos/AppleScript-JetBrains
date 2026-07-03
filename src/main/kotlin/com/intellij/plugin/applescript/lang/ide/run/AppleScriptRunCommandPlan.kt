package com.intellij.plugin.applescript.lang.ide.run

import java.util.regex.Pattern

/**
 * Pure planning for the `osascript` run command: argument construction, script-parameter parsing,
 * and the default configuration name. IntelliJ run-config objects (command-line state, producer,
 * settings editor) are adapters that read their fields and delegate here, so path and quoting
 * behavior is unit-testable without run-configuration machinery.
 */
internal object AppleScriptRunCommandPlan {
    const val OSASCRIPT_PATH: String = "/usr/bin/osascript"

    val APPLE_EVENT_DEBUG_ENVIRONMENT: Map<String, String> =
        mapOf("AEDebugSends" to "1", "AEDebugReceives" to "1")

    fun osascriptCommandLine(
        scriptPath: String?,
        scriptOptions: String?,
        scriptParameters: String?,
    ): List<String> =
        buildList {
            add(OSASCRIPT_PATH)
            addAll(splitOptions(scriptOptions))
            // No scriptPath validation by design: an empty path falls through to osascript, which
            // surfaces its own error, matching the original behaviour.
            add(scriptPath.orEmpty())
            addAll(parseScriptParameters(scriptParameters))
        }

    /** Last path segment, e.g. `/a/b/script.applescript` -> `script.applescript`. */
    fun defaultConfigurationName(scriptPath: String): String = scriptPath.substringAfterLast('/')

    private fun splitOptions(scriptOptions: String?): List<String> =
        scriptOptions
            ?.takeIf { it.isNotEmpty() }
            ?.split(" ")
            .orEmpty()

    private fun parseScriptParameters(scriptParameters: String?): List<String> {
        if (scriptParameters.isNullOrEmpty()) return emptyList()

        val parameters = mutableListOf<String>()
        val matcher = PARAM_PATTERN.matcher(scriptParameters)
        while (matcher.find()) {
            for (groupIndex in 1..matcher.groupCount()) {
                matcher.group(groupIndex)?.takeIf { it.isNotEmpty() }?.let(parameters::add)
            }
        }
        return parameters
    }

    private val PARAM_PATTERN: Pattern = Pattern.compile("\"([^\"]*)\"|(\\w+)")
}
