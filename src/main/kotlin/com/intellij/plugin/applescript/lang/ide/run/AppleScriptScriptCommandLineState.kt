package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment

class AppleScriptScriptCommandLineState(
    private val runConfiguration: AppleScriptRunConfiguration,
    env: ExecutionEnvironment,
) : CommandLineState(env) {
    // Not unit-tested: this is the IntelliJ run-config adapter, and startProcess launches a real
    // osascript process. The command/argument/quoting logic it delegates to is covered directly by
    // AppleScriptRunCommandPlanTest; exercising startProcess itself would need a live execution env.
    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
        val commandLine = GeneralCommandLine(commandArguments())
        commandLine.enableAppleEventDebugIfNeeded()
        return AppleScriptProcessHandler(commandLine)
    }

    private fun commandArguments(): List<String> =
        AppleScriptRunCommandPlan.osascriptCommandLine(
            runConfiguration.scriptPath,
            runConfiguration.scriptOptions,
            runConfiguration.scriptParameters,
        )

    private fun GeneralCommandLine.enableAppleEventDebugIfNeeded() {
        if (!runConfiguration.showAppleEvents) return
        AppleScriptRunCommandPlan.APPLE_EVENT_DEBUG_ENVIRONMENT.forEach { (key, value) ->
            withEnvironment(key, value)
        }
    }
}
