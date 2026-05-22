package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import java.nio.charset.Charset

class AppleScriptProcessHandler : KillableColoredProcessHandler {

    @Throws(ExecutionException::class)
    internal constructor(commandLine: GeneralCommandLine) : super(commandLine)

    constructor(process: Process, commandLine: String, charset: Charset) : super(process, commandLine, charset)

    constructor(process: Process, commandLine: String) : super(process, commandLine)
}
