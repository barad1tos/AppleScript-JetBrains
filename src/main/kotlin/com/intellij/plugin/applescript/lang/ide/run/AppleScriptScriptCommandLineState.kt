package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import java.util.regex.Pattern

class AppleScriptScriptCommandLineState(
    private val runConfiguration: AppleScriptRunConfiguration,
    env: ExecutionEnvironment,
) : CommandLineState(env) {

    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
        // No scriptPath validation here by design: an empty path falls through to osascript,
        // which surfaces its own error — matching the original Java behaviour. Earlier,
        // richer validation at the run-config (checkConfiguration) level is a backlog item.
        val scriptPath = runConfiguration.scriptPath.orEmpty()
        val scriptParameters = runConfiguration.scriptParameters
        val scriptOptions = runConfiguration.scriptOptions

        val commandString = mutableListOf("/usr/bin/osascript")
        if (!StringUtil.isEmpty(scriptOptions)) {
            // Kotlin flow-analysis cannot see through the opaque StringUtil.isEmpty guard above.
            requireNotNull(scriptOptions) { "scriptOptions non-null: guarded by !StringUtil.isEmpty above" }
            commandString.addAll(scriptOptions.split(" "))
        }
        commandString.add(scriptPath)
        if (!StringUtil.isEmpty(scriptParameters)) {
            // Kotlin flow-analysis cannot see through the opaque StringUtil.isEmpty guard above.
            requireNotNull(scriptParameters) { "scriptParameters non-null: guarded by !StringUtil.isEmpty above" }
            val matchedParams = mutableListOf<String>()
            val matcher = PARAM_PATTERN.matcher(scriptParameters)
            while (matcher.find()) {
                for (i in 1..matcher.groupCount()) {
                    try {
                        matcher.group(i)?.takeIf { it.isNotEmpty() }?.let { matchedParams.add(it) }
                    } catch (e: IllegalStateException) {
                        LOG.warn("Error parsing script parameters: ${e.message}")
                    } catch (e: IndexOutOfBoundsException) {
                        LOG.warn("Error parsing script parameters: ${e.message}")
                    }
                }
            }
            commandString.addAll(matchedParams)
        }

        val commandLine = GeneralCommandLine(commandString)
        if (runConfiguration.showAppleEvents) {
            commandLine.withEnvironment("AEDebugSends", "1")
            commandLine.withEnvironment("AEDebugReceives", "1")
        }
        return AppleScriptProcessHandler(commandLine)
    }

    private val LOG: Logger = Logger.getInstance("#${javaClass.name}")

    private companion object {
        private val PARAM_PATTERN: Pattern = Pattern.compile("\"([^\"]*)\"|(\\w+)")
    }
}
