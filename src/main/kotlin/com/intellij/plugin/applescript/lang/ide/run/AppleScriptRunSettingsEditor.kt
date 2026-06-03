package com.intellij.plugin.applescript.lang.ide.run

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.text.StringUtil
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class AppleScriptRunSettingsEditor(
    project: Project,
) : SettingsEditor<AppleScriptRunConfiguration>() {
    // Initialised by the IntelliJ UI Designer instrumentation from AppleScriptRunSettingsEditor.form.
    private lateinit var mainPanel: JPanel
    private lateinit var scriptTextField: TextFieldWithBrowseButton
    private lateinit var parametersTextField: JTextField
    private lateinit var scriptOptionsTextField: JTextField
    private lateinit var showAppleEventsCheckBox: JCheckBox

    init {
        val descriptor =
            FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Choose Script")
                .withDescription("Please choose script to run")
        scriptTextField.addBrowseFolderListener(project, descriptor)
    }

    override fun resetEditorFrom(configuration: AppleScriptRunConfiguration) {
        val scriptPath = configuration.scriptPath
        if (!StringUtil.isEmpty(scriptPath)) {
            // Kotlin flow-analysis cannot see through the opaque StringUtil.isEmpty guard above;
            // requireNotNull asserts (and smart-casts) the non-null invariant.
            requireNotNull(scriptPath) { "scriptPath non-null: guarded by !StringUtil.isEmpty above" }
            scriptTextField.text = scriptPath
            val parts = scriptPath.split("/")
            if (parts.isNotEmpty()) configuration.name = parts.last()
        }
        configuration.scriptParameters?.let { if (it.isNotEmpty()) parametersTextField.text = it }
        configuration.scriptOptions?.let { if (it.isNotEmpty()) scriptOptionsTextField.text = it }
        showAppleEventsCheckBox.isSelected = configuration.showAppleEvents
    }

    @Throws(ConfigurationException::class)
    override fun applyEditorTo(configuration: AppleScriptRunConfiguration) {
        configuration.scriptPath = scriptTextField.text.trim()
        configuration.scriptParameters = parametersTextField.text.trim()
        configuration.scriptOptions = scriptOptionsTextField.text.trim()
        configuration.showAppleEvents = showAppleEventsCheckBox.isSelected
    }

    override fun createEditor(): JComponent = mainPanel
}
