package com.intellij.plugin.applescript.lang.ide.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.plugin.applescript.AppleScriptFileType
import javax.swing.Icon

class AppleScriptColorsAndFontsPage : ColorSettingsPage {
    override fun getIcon(): Icon = AppleScriptFileType.icon

    override fun getHighlighter(): SyntaxHighlighter = AppleScriptSyntaxHighlighter()

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = OUR_TAGS

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = ATTRS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "AppleScript"

    private companion object {
        private val ATTRS: Array<AttributesDescriptor> =
            arrayOf(
                AttributesDescriptor("Keyword", AppleScriptSyntaxHighlighterColors.KEYWORD),
                AttributesDescriptor("Logical operator", AppleScriptSyntaxHighlighterColors.LOGICAL_OPERATOR),
                AttributesDescriptor("Comparison operator", AppleScriptSyntaxHighlighterColors.COMPARISON_OPERATOR),
                AttributesDescriptor("Language literal", AppleScriptSyntaxHighlighterColors.LANGUAGE_LITERAL),
                AttributesDescriptor("Built-in type", AppleScriptSyntaxHighlighterColors.BUILT_IN_TYPE),
                AttributesDescriptor("Handler call", AppleScriptSyntaxHighlighterColors.HANDLER_CALL),
                AttributesDescriptor("Variable", AppleScriptSyntaxHighlighterColors.VARIABLE),
                AttributesDescriptor("String", AppleScriptSyntaxHighlighterColors.STRING),
                AttributesDescriptor("Operator", AppleScriptSyntaxHighlighterColors.OPERATION_SIGN),
                AttributesDescriptor("Comment", AppleScriptSyntaxHighlighterColors.COMMENT),
                AttributesDescriptor("Dictionary command", AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR),
                AttributesDescriptor(
                    "Command parameter name",
                    AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
                ),
                AttributesDescriptor("Dictionary class", AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR),
                AttributesDescriptor(
                    "Dictionary property",
                    AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR,
                ),
                AttributesDescriptor(
                    "Dictionary constant",
                    AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR,
                ),
            )

        private val OUR_TAGS: Map<String, TextAttributesKey> =
            mapOf(
                "keyword" to AppleScriptSyntaxHighlighterColors.KEYWORD,
                "logical operator" to AppleScriptSyntaxHighlighterColors.LOGICAL_OPERATOR,
                "comparison operator" to AppleScriptSyntaxHighlighterColors.COMPARISON_OPERATOR,
                "language literal" to AppleScriptSyntaxHighlighterColors.LANGUAGE_LITERAL,
                "built-in type" to AppleScriptSyntaxHighlighterColors.BUILT_IN_TYPE,
                "handler call" to AppleScriptSyntaxHighlighterColors.HANDLER_CALL,
                "variable" to AppleScriptSyntaxHighlighterColors.VARIABLE,
                "string" to AppleScriptSyntaxHighlighterColors.STRING,
                "operator" to AppleScriptSyntaxHighlighterColors.OPERATION_SIGN,
                "comment" to AppleScriptSyntaxHighlighterColors.COMMENT,
                "command" to AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR,
                "command parameter" to AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR,
                "dictionary class" to AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR,
                "dictionary property" to AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR,
                "dictionary constant" to AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR,
            )

        private const val DEMO_TEXT: String =
            "<comment>-- saves the text contents of the clipboard to a text file with a name you specify.</comment>\n" +
                "<comment>-- be sure to specify an extension if needed</comment>\n" +
                "set <variable>d</variable> to <command>the clipboard</command> " +
                "as <dictionary class>«class utf8»</dictionary class>\n" +
                "set <variable>fn</variable> to <command>choose file name</command>\n" +
                "set <variable>fid</variable> to <command>open for access</command> <variable>fn</variable> " +
                "<command parameter>with write permission</command parameter>\n" +
                "<command>write</command> <variable>d</variable> <command parameter>to</command parameter> " +
                "<variable>fid</variable>\n" +
                "<command>close access</command> <variable>fid</variable>\n" +
                "\n" +
                "if <variable>reportsToPrint</variable> > 0 then\n" +
                "    tell application \"ReportWizard\"\n" +
                "    -- Statements to print the reports.\n" +
                "    end tell\n" +
                "end if ## had some reports to print\n" +
                "if <variable>track_count</variable> <comparison operator>></comparison operator> 0 " +
                "<logical operator>and</logical operator> <variable>statusText</variable> is not " +
                "<language literal>missing value</language literal> then\n" +
                "  set <variable>statusText</variable> to my " +
                "<handler call>normalize_cloud_status</handler call>(<variable>rawStatus</variable>)\n" +
                "  set <variable>raw_year</variable> to <variable>raw_year</variable> " +
                "as <built-in type>integer</built-in type>\n" +
                "end if\n" +
                "tell application \"TextEdit\"\n" +
                "  <command>make</command> <command parameter>new</command parameter> " +
                "<dictionary class>document</dictionary class> " +
                "<command parameter>with properties</command parameter> " +
                "{name:\"Doc Name\", text:\"Now I can create new documents ¬\n" +
                "using AppleScript.\"}\n" +
                "  get <dictionary property>message id</dictionary property> of it's " +
                "<dictionary class>mailbox</dictionary class>\n" +
                "  <command>close</command>\n" +
                "  get <dictionary class>character</dictionary class>\n" +
                "  set pathToSpec to <dictionary constant>application support</dictionary constant>\n" +
                "  set character to 1234\n" +
                "end tell"
    }
}
