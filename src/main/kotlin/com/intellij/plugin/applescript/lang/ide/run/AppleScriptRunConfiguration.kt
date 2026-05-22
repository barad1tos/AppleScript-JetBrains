package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.openapi.util.text.StringUtil
import org.jdom.Element

class AppleScriptRunConfiguration internal constructor(
    project: Project,
    configurationFactory: ConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<RunConfiguration>(project, configurationFactory, name) {

    internal var scriptPath: String? = null
    internal var scriptParameters: String? = null
    internal var scriptOptions: String? = null
    internal var showAppleEvents: Boolean = false

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        AppleScriptRunSettingsEditor(project, this)

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        AppleScriptScriptCommandLineState(this, environment)

    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        super.readExternal(element)
        val scriptUrl = element.getAttributeValue(SCRIPT_PATH_URL)
        val scriptParams = element.getAttributeValue(SCRIPT_PARAMETERS)
        val scriptOptionsAttr = element.getAttributeValue(SCRIPT_OPTIONS)
        val logEvents = element.getAttributeValue(SCRIPT_SHOW_EVENTS)
        if (!StringUtil.isEmpty(scriptUrl)) scriptPath = scriptUrl
        if (!StringUtil.isEmpty(scriptParams)) scriptParameters = scriptParams
        if (!StringUtil.isEmpty(scriptOptionsAttr)) scriptOptions = scriptOptionsAttr
        if (!StringUtil.isEmpty(logEvents)) showAppleEvents = "true" == logEvents
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        scriptPath?.let { if (it.isNotEmpty()) element.setAttribute(SCRIPT_PATH_URL, it) }
        scriptParameters?.let { if (it.isNotEmpty()) element.setAttribute(SCRIPT_PARAMETERS, it) }
        scriptOptions?.let { if (it.isNotEmpty()) element.setAttribute(SCRIPT_OPTIONS, it) }
        element.setAttribute(SCRIPT_SHOW_EVENTS, if (showAppleEvents) "true" else "false")
    }

    private companion object {
        private const val SCRIPT_PATH_URL = "scriptUrl"
        private const val SCRIPT_PARAMETERS = "scriptParameters"
        private const val SCRIPT_OPTIONS = "scriptOptions"
        private const val SCRIPT_SHOW_EVENTS = "logEvents"
    }
}
