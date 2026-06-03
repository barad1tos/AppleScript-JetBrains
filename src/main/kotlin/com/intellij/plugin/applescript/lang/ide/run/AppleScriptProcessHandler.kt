package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler

internal class AppleScriptProcessHandler
    @Throws(ExecutionException::class)
    constructor(
        commandLine: GeneralCommandLine,
    ) : KillableColoredProcessHandler(commandLine)
