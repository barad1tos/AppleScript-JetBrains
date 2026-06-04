package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary

internal object DictionaryCommandCollector {
    fun collectCommands(
        builder: PsiBuilder,
        parsedCommandName: String,
        lookupScope: DictionaryCommandLookupScope,
    ): List<AppleScriptCommand> =
        buildList {
            addApplicationCommands(builder, lookupScope.toldApplicationName, parsedCommandName)
            if (ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY == lookupScope.toldApplicationName) {
                addApplicationCommands(builder, ApplicationDictionary.COCOA_STANDARD_LIBRARY, parsedCommandName)
            }
            if (lookupScope.areThereUseStatements) {
                lookupScope.applicationsToImport?.forEach { applicationName ->
                    addApplicationCommands(builder, applicationName, parsedCommandName)
                }
            } else {
                addAll(ParsableScriptSuiteRegistryHelper.findStdCommands(builder.project, parsedCommandName))
            }
            if (isEmpty()) {
                addApplicationCommands(builder, ApplicationDictionary.COCOA_STANDARD_LIBRARY, parsedCommandName)
            }
        }

    private fun MutableList<AppleScriptCommand>.addApplicationCommands(
        builder: PsiBuilder,
        applicationName: String,
        commandName: String,
    ) {
        addAll(
            ParsableScriptSuiteRegistryHelper.findApplicationCommands(
                builder.project,
                applicationName,
                commandName,
            ),
        )
    }
}
