package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import java.util.regex.Pattern

class AppleScriptScriptCommandLineState(
    private val runConfiguration: AppleScriptRunConfiguration,
    env: ExecutionEnvironment,
) : CommandLineState(env) {
    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
        val commandLine = GeneralCommandLine(commandArguments())
        commandLine.enableAppleEventDebugIfNeeded()
        return AppleScriptProcessHandler(commandLine)
    }

    private fun commandArguments(): List<String> =
        buildList {
            add(OSASCRIPT_PATH)
            addAll(runConfiguration.scriptOptions.toArguments())
            // No scriptPath validation here by design: an empty path falls through to osascript,
            // which surfaces its own error, matching the original Java behaviour.
            add(runConfiguration.scriptPath.orEmpty())
            addAll(runConfiguration.scriptParameters.toScriptParameters())
        }

    private fun String?.toArguments(): List<String> =
        takeIf { !it.isNullOrEmpty() }
            ?.split(" ")
            .orEmpty()

    private fun String?.toScriptParameters(): List<String> {
        if (isNullOrEmpty()) return emptyList()

        val parameters = mutableListOf<String>()
        val matcher = PARAM_PATTERN.matcher(this)
        while (matcher.find()) {
            for (groupIndex in 1..matcher.groupCount()) {
                matcher.group(groupIndex)?.takeIf { it.isNotEmpty() }?.let(parameters::add)
            }
        }
        return parameters
    }

    private fun GeneralCommandLine.enableAppleEventDebugIfNeeded() {
        if (!runConfiguration.showAppleEvents) return

        withEnvironment("AEDebugSends", "1")
        withEnvironment("AEDebugReceives", "1")
    }

    private companion object {
        private const val OSASCRIPT_PATH = "/usr/bin/osascript"
        private val PARAM_PATTERN: Pattern = Pattern.compile("\"([^\"]*)\"|(\\w+)")
    }
}
