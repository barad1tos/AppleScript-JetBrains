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
        // TODO: validate scriptPath at the run-config level; for now mirror the Java behaviour.
        val scriptPath = runConfiguration.scriptPath.orEmpty()
        val scriptParameters = runConfiguration.scriptParameters
        val scriptOptions = runConfiguration.scriptOptions

        val commandString = mutableListOf("/usr/bin/osascript")
        if (!StringUtil.isEmpty(scriptOptions)) {
            commandString.addAll(scriptOptions!!.split(" "))
        }
        commandString.add(scriptPath)
        if (!StringUtil.isEmpty(scriptParameters)) {
            val matchedParams = mutableListOf<String>()
            val matcher = PARAM_PATTERN.matcher(scriptParameters!!)
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
