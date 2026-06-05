package com.intellij.plugin.applescript.lang.formatter.settings

import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class AppleScriptLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = AppleScriptLanguage

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        indentOptions.INDENT_SIZE = DEFAULT_INDENT_SIZE
        indentOptions.CONTINUATION_INDENT_SIZE = DEFAULT_CONTINUATION_INDENT_SIZE
        indentOptions.TAB_SIZE = DEFAULT_TAB_SIZE
    }

    override fun getCodeSample(settingsType: SettingsType): String = CODE_SAMPLE

    override fun customizeSettings(
        consumer: CodeStyleSettingsCustomizable,
        settingsType: SettingsType,
    ) {
        when (settingsType) {
            SettingsType.SPACING_SETTINGS ->
                consumer.showStandardOptions(
                    "SPACE_BEFORE_METHOD_CALL_PARENTHESES",
                    "SPACE_BEFORE_METHOD_PARENTHESES",
                    "SPACE_BEFORE_IF_PARENTHESES",
                    "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                    "SPACE_AROUND_LOGICAL_OPERATORS",
                    "SPACE_AROUND_EQUALITY_OPERATORS",
                    "SPACE_AROUND_RELATIONAL_OPERATORS",
                    "SPACE_AROUND_ADDITIVE_OPERATORS",
                    "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
                    "SPACE_AROUND_SHIFT_OPERATORS",
                    "SPACE_BEFORE_METHOD_LBRACE",
                    "SPACE_BEFORE_ELSE_LBRACE",
                    "SPACE_BEFORE_WHILE_KEYWORD",
                    "SPACE_BEFORE_ELSE_KEYWORD",
                    "SPACE_WITHIN_METHOD_CALL_PARENTHESES",
                    "SPACE_WITHIN_METHOD_PARENTHESES",
                    "SPACE_WITHIN_IF_PARENTHESES",
                    "SPACE_BEFORE_COLON",
                    "SPACE_AFTER_COLON",
                    "SPACE_AFTER_COMMA",
                    "SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS",
                    "SPACE_BEFORE_COMMA",
                    "SPACE_AROUND_UNARY_OPERATOR",
                )
            SettingsType.BLANK_LINES_SETTINGS -> consumer.showStandardOptions("KEEP_BLANK_LINES_IN_CODE")
            SettingsType.WRAPPING_AND_BRACES_SETTINGS ->
                consumer.showStandardOptions(
                    "RIGHT_MARGIN",
                    "KEEP_LINE_BREAKS",
                    "KEEP_FIRST_COLUMN_COMMENT",
                    "BRACE_STYLE",
                    "METHOD_BRACE_STYLE",
                    "CALL_PARAMETERS_WRAP",
                    "CALL_PARAMETERS_LPAREN_ON_NEXT_LINE",
                    "CALL_PARAMETERS_RPAREN_ON_NEXT_LINE",
                    "METHOD_PARAMETERS_WRAP",
                    "METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE",
                    "METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE",
                    "ELSE_ON_NEW_LINE",
                    "ALIGN_MULTILINE_PARAMETERS",
                    "ALIGN_MULTILINE_PARAMETERS_IN_CALLS",
                    "ALIGN_MULTILINE_BINARY_OPERATION",
                    "BINARY_OPERATION_WRAP",
                    "BINARY_OPERATION_SIGN_ON_NEXT_LINE",
                    "PARENTHESES_EXPRESSION_LPAREN_WRAP",
                    "PARENTHESES_EXPRESSION_RPAREN_WRAP",
                    "SPECIAL_ELSE_IF_TREATMENT",
                )
            else -> Unit
        }
    }

    private companion object {
        private const val DEFAULT_INDENT_SIZE = 2
        private const val DEFAULT_CONTINUATION_INDENT_SIZE = 4
        private const val DEFAULT_TAB_SIZE = 2

        private const val CODE_SAMPLE = """searchFiles for ff of minimumValue(2, 3) for "LeChateau"
text from word(copy 5 to c) to word 4 of "We're all in this together"
minimumValue(2, maximumValue(x, y))
if file_type comes before "APPL" then copy 5 to c
on myPoint({x, y})
  display dialog("x = " & x & ", y = " & y)
end myPoint

if reportsToPrint > 0 then
  tell application "ReportWizard"
  -- Statements to print the reports.
  end tell
end if ## had some reports to print
{"this", "is", 1 + 1, "cool"} + 8
myPoint({x, y})

if (x > y) then
  set myMessage to " is greater than "
else if (x < y) then
  set myMessage to " is less than "
else
  set myMessage to " is equal to "
end if
on MoveToEndOfContent()
  tell application "System Events"
    tell process "Mail"
      keystroke "a" using command down
      key code 124 -- right arrow
      keystroke return
      keystroke return
    end tell
  end tell
end MoveToEndOfContent

-- Send a message using application "Mail"
set recipientName to "WhiteHat"
set recipientAddress to "someemail@here.com"
set theSubject to "Type your subject here!"
set theContent to "Type your message content here!"

tell application "Mail"
##Create the message
  set theMessage to make new outgoing message with properties {subject:theSubject, content:theContent, visible:true}
  ##Set a recipient
  tell theMessage
    make new to recipient with properties {name:recipientName, address:recipientAddress}
    display dialog "Dialog title"
    ##Send the Message
    send
  end tell
end tell
"""
    }
}
