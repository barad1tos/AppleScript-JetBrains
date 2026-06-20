package com.intellij.plugin.applescript.test.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.parser.GeneratedParserUtilBase.TRUE_CONDITION
import com.intellij.lang.parser.GeneratedParserUtilBase._COLLAPSE_
import com.intellij.lang.parser.GeneratedParserUtilBase.adapt_builder_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.lang.parser.AppleScriptGeneratedParserUtil
import com.intellij.plugin.applescript.lang.parser.AppleScriptParser
import com.intellij.plugin.applescript.lang.parser.AppleScriptParserDefinition
import com.intellij.plugin.applescript.lang.parser.DictionaryCommandParameterParser
import com.intellij.plugin.applescript.lang.parser.FallbackCommandParameterParser
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandImpl
import com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameterData
import com.intellij.plugin.applescript.lang.sdef.CommandParameterImpl
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_CLASS_IDENTIFIER_PLURAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.REFERENCE_EXPRESSION
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.lang.reflect.Proxy

class FallbackCommandParameterParserTest : BasePlatformTestCase() {
    fun testFullParserCommandHandlerCallExposesParameterNodes() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "display dialog \"Hello\" default answer \"Name\"",
            )

        assertNoParserErrors(psiFile)

        assertEquals(listOf("default answer"), psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR))
        assertEquals(listOf("\"Hello\""), psiFile.node.textsOf(DIRECT_PARAMETER_VAL))
    }

    fun testFullParserStandardAdditionsCommandInsideObjectReferenceConsumesListParameters() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                set appLaunch to text returned of (display dialog "" default answer "" buttons {"Go"} default button "Go")
                if appLaunch contains "" then
                    error number -128
                end if
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("buttons"))
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("default button"))
    }

    fun testFullParserErrorNumberBeforeElseIf() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                if applaunch contains "" then
                    error number -128
                else if applaunch contains applaunch then
                    set didMatch to true
                end if
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testFullParserDialogAssignmentBeforeElseIf() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                set applaunch to text returned of (display dialog "" default answer "" buttons {"Go"} default button "Go")
                if applaunch contains "" then
                    error number -128
                else if applaunch contains applaunch then
                    set didMatch to true
                end if
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testFullParserVariableApplicationTellBlock() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application applaunch
                    quit
                end tell
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testFullParserVariableApplicationTellInsideTry() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                try
                    tell application applaunch
                        quit
                    end tell
                on error
                    set didFail to true
                end try
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testFullParserSystemEventsProcessLookupBlock() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "System Events"
                    set {processList, pidList} to the {name, unix id} of (every process whose name contains applaunch)
                    if processList contains applaunch then
                        do shell script "kill -KILL " & pidList
                    end if
                end tell
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testFullParserSystemEventsExistsFileCondition() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "System Events"
                    if exists file pauseFilePath then
                        delete file pauseFilePath
                    end if
                end tell
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testFullParserNestedTellTryHandlerWithFileCommands() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                on run
                    set logMessages to ""
                    set now to current date

                    try
                        tell application "System Events"
                            if exists file pauseFilePath then
                                try
                                    set pauseContent to read file pauseFilePath
                                    set pauseUntil to my parsePauseDate(pauseContent)

                                    if now < pauseUntil then
                                        set logMessages to logMessages & "Script is paused until " & (my formatDate(pauseUntil)) & return
                                        my appendToFile(logFilePath, logMessages)
                                        return logMessages
                                    else
                                        delete file pauseFilePath
                                    end if
                                on error errMsg
                                    set logMessages to logMessages & "Error reading pause file: " & errMsg & return
                                end try
                            end if
                        end tell

                        set logMessages to logMessages & "Script ended at " & (my formatDate(current date)) & return
                        my appendToFile(logFilePath, logMessages)
                        return logMessages
                    on error errorMessage
                        set logMessages to logMessages & "Error: " & errorMessage & return
                        display dialog "Unexpected Error: " & errorMessage buttons {"OK"} default button "OK"
                    end try

                    return logMessages
                end run

                on appendToFile(filePath, contents)
                    try
                        set fileHandle to open for access file filePath with write permission
                        write contents to fileHandle starting at eof
                        close access fileHandle
                    on error
                        try
                            close access file filePath
                        end try
                    end try
                end appendToFile
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testFullParserSystemSettingsUiScriptingSpecifier() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "System Settings"
                    activate
                    reveal anchor "displaysDisplayTab" of pane id "com.apple.preference.displays"
                    tell application "System Events"
                        delay 1
                        set value of slider 1 of group 1 of tab group 1 of window 1 of process "System Preferences" to 1
                    end tell
                    quit
                end tell
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testFullParserTabTextConstantStillParsesOutsideDictionaryClassPhrase() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                set tabText to tab
                set joinedText to tab & "value"
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
    }

    fun testContainingSetOperandEmitsDictionaryClassName() {
        // The `of containing set` operand parses `containing set` as a plural dictionary class
        // identifier. Before this fallback, `containing` parsed as a bare reference and the `set`
        // keyword dangled as a parser error. Keep SET accepted only as a second class word in OF/IN
        // operand position so the assignment statement keeps its normal parse path.
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "tell application \"Typinator\"\nset containerid to unique id of containing set\nend tell",
            )

        assertEquals(listOf("containing set"), psiFile.node.textsOf(DICTIONARY_CLASS_IDENTIFIER_PLURAL))
        assertNoParserErrors(psiFile)
    }

    fun testIntroducedAndIndexedRuleSetOperandsEmitDictionaryClassNames() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                tell application "Typinator"
                    set ruleNames to name of every rule set
                    set matchingRule to first rule set whose unique id is containerId
                    set parentId to unique id in containing set
                end tell
                """.trimIndent(),
            )

        val dictionaryClassNames = psiFile.node.textsOf(DICTIONARY_CLASS_IDENTIFIER_PLURAL)
        assertTrue(
            "introduced class fallback must preserve the two-word `rule set` class: $dictionaryClassNames",
            dictionaryClassNames.contains("rule set"),
        )
        assertTrue(
            "IN operands must accept `containing set` without treating SET as assignment: $dictionaryClassNames",
            dictionaryClassNames.contains("containing set"),
        )
        assertNoParserErrors(psiFile)
    }

    fun testFallbackParametersExposeSelectorAndDirectParameterNodes() {
        val builder = createBuilder("\"Hello\" default answer \"Name\" with title \"Prompt\" giving up after 3")

        val ast =
            parseWithRootSection(builder) {
                FallbackCommandParameterParser.parseParameters(builder, 0, "display dialog")
            }

        assertEquals(
            listOf("default answer", "with title", "giving up after"),
            ast.textsOf(COMMAND_PARAMETER_SELECTOR),
        )
        assertEquals(listOf("\"Hello\""), ast.textsOf(DIRECT_PARAMETER_VAL))
    }

    fun testLabeledNotificationTailExposesSelectors() {
        // The `display notification` labeled tail must expose precise selector nodes even when the
        // dictionary is unavailable. `with title` is a WITH + identifier selector; `subtitle` and
        // `sound name` are bare-label selectors. The direct parameter is the leading string literal.
        val builder =
            createBuilder("\"No id\" with title \"Edit Snippet\" subtitle \"Error:\" sound name \"Basso\"")

        val ast =
            parseWithRootSection(builder) {
                FallbackCommandParameterParser.parseParameters(builder, 0, "display notification")
            }

        assertEquals(
            listOf("with title", "subtitle", "sound name"),
            ast.textsOf(COMMAND_PARAMETER_SELECTOR),
        )
        assertEquals(listOf("\"No id\""), ast.textsOf(DIRECT_PARAMETER_VAL))
    }

    fun testLabeledNotificationTailStopsVariableValuesBeforeNextSelector() {
        val builder = createBuilder("messageText with title titleText subtitle subtitleText sound name soundName")

        val ast =
            parseWithRootSection(builder) {
                FallbackCommandParameterParser.parseParameters(builder, 0, "display notification")
            }

        assertEquals(
            listOf("with title", "subtitle", "sound name"),
            ast.textsOf(COMMAND_PARAMETER_SELECTOR),
        )
        assertEquals(listOf("messageText"), ast.textsOf(DIRECT_PARAMETER_VAL))
    }

    fun testGenericSingleWordHeadProducesParameters() {
        // A generic single-word command head accepted by the permissive command-name fallback gets
        // the unknown-command tail consumer. Without the parser-state flag, a bare unknown single
        // word still declines and falls through to the normal expression/error path.
        val builder = createBuilder("width for labels theStrings")
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED, true)

        val ast =
            parseWithRootSection(builder) {
                FallbackCommandParameterParser.parseParameters(builder, 0, "max")
            }

        assertEquals(
            listOf(
                "width for labels theStrings",
            ),
            ast.textsOf(COMMAND_PARAMETER),
        )
    }

    fun testPermissiveKeywordSelectorTailProducesParameterChunks() {
        val builder =
            createBuilder(
                "rule width 400 placeholder text \"x\" left inset 8 total width 400 field left 100",
            )
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_PERMISSIVE_COMMAND_ALLOWED, true)

        val ast =
            parseWithRootSection(builder) {
                FallbackCommandParameterParser.parseParameters(builder, 0, "create side labeled field")
            }

        assertEquals(
            listOf(
                "rule width 400",
                "placeholder text \"x\"",
                "left inset 8",
                "total width 400",
                "field left 100",
            ),
            ast.textsOf(COMMAND_PARAMETER),
        )
    }

    fun testBareUnknownSingleWordWithoutAcceptorFlagReturnsNoParameters() {
        // Pitfall 1/2 guard: WITHOUT the acceptor flag, a bare unknown single word must NOT be granted
        // OptionalDirectParameter — parameterMode returns null so the fallback declines (no consumption,
        // letting a genuine error / plain-variable position fall through unharmed).
        val builder = createBuilder("width for labels theStrings")

        assertFalse(FallbackCommandParameterParser.parseParameters(builder, 0, "max"))
        assertEquals("width", builder.tokenText)
    }

    fun testFullParserGenericHeadSplitsSelectorTailAtPreposition() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "set maxLabelWidth to max width for labels theLabelStrings",
            )

        assertNoParserErrors(psiFile)
        assertEquals(listOf("width for labels theLabelStrings"), psiFile.node.textsOf(COMMAND_PARAMETER))
    }

    fun testFullParserGenericHeadAcceptsKeywordLabels() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                subscribe to someTarget
                sign using someKey
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
        assertEquals(listOf("to someTarget", "using someKey"), psiFile.node.textsOf(COMMAND_PARAMETER))
    }

    fun testFullParserGenericHeadDoesNotHijackAsCoercion() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "set tidNum to tid as integer",
            )

        assertNoParserErrors(psiFile)
        assertEquals(emptyList<String>(), psiFile.node.textsOf(COMMAND_PARAMETER))
    }

    fun testFullParserGenericHeadAcceptsObjectReferenceValue() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "quick search name of front window",
            )

        assertNoParserErrors(psiFile)
        assertEquals(listOf("name of front window"), psiFile.node.textsOf(COMMAND_PARAMETER))
    }

    fun testPermissiveParameterAcceptsObjectReferenceValueAfterSelector() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "notify with message name of front window",
            )

        assertNoParserErrors(psiFile)
        assertEquals(listOf("with message name of front window"), psiFile.node.textsOf(COMMAND_PARAMETER))
    }

    fun testFullParserGenericHeadAcceptsAdditionalSelectorKeywords() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "sort by column 1 over itemsList",
            )

        assertNoParserErrors(psiFile)
        assertEquals(listOf("by column 1", "over itemsList"), psiFile.node.textsOf(COMMAND_PARAMETER))
    }

    fun testStandardAdditionsWriteStartingAtSelector() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "write (lineText & linefeed) to fh starting at eof",
            )

        assertNoParserErrors(psiFile)
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("starting at"))
    }

    fun testStandardAdditionsWriteStartingAtSelectorWithIdentifierDirectParameter() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "write contents to fileHandle starting at eof",
            )

        assertNoParserErrors(psiFile)
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("starting at"))
    }

    fun testStandardAdditionsOpenForAccessAcceptsFileDirectParameterWithSelector() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "set pauseFile to open for access file pauseFilePath with write permission",
            )

        assertNoParserErrors(psiFile)
        assertEquals(listOf("file pauseFilePath"), psiFile.node.textsOf(DIRECT_PARAMETER_VAL))
        assertEquals(listOf("with write permission"), psiFile.node.textsOf(COMMAND_PARAMETER))
    }

    fun testStandardAdditionsCloseAccessAcceptsFileDirectParameter() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "close access file filePath",
            )

        assertNoParserErrors(psiFile)
        assertEquals(listOf("file filePath"), psiFile.node.textsOf(DIRECT_PARAMETER_VAL))
    }

    fun testChooseFromListDefaultItemsAndMultipleSelectionsSelector() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                set choice to choose from list opts with prompt "Choose:" default items {"Path+Size"} without multiple selections allowed
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("default items"))
    }

    fun testFullParserChooseFileAssignmentAcceptsLabeledParameters() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                set applic to choose file with prompt "Choose Application" default location "/Applications"
                set applic to POSIX path of applic
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("with prompt"))
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("default location"))
    }

    fun testFullParserChooseFileAcceptsOfTypeListParameter() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                set ffile to (choose file with prompt "Get contents from..." of type {"txt", "md", "markdown", "rtf"})
                set ffcont to read ffile
                """.trimIndent(),
            )

        assertNoParserErrors(psiFile)
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("with prompt"))
        assertTrue(psiFile.node.textsOf(COMMAND_PARAMETER_SELECTOR).contains("of type"))
    }

    fun testPermissiveParameterAcceptsConstantValues() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "configure with enabled true without waiting false given missing value using current date",
            )

        assertNoParserErrors(psiFile)
        assertEquals(
            listOf(
                "with enabled true",
                "without waiting false",
                "given missing value",
                "using current date",
            ),
            psiFile.node.textsOf(COMMAND_PARAMETER),
        )
    }

    fun testUnterminatedPermissiveBracketReportsErrorWithoutSwallowingNextStatement() {
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                """
                quick search {"x"
                set done to true
                """.trimIndent(),
            )

        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        assertTrue("unterminated bracket should stay visible as a parser error", errors.isNotEmpty())
        assertFalse(
            "unterminated command tail must not swallow the following statement",
            psiFile.node.textsOf(COMMAND_PARAMETER).any { parameterText -> "set done" in parameterText },
        )
    }

    fun testFallbackBooleanParameterConsumesOnlyBooleanSelector() {
        val builder = createBuilder("\"printf test\" without confirmation")

        val ast =
            parseWithRootSection(builder) {
                FallbackCommandParameterParser.parseParameters(builder, 0, "do shell script")
            }

        assertEquals(listOf("without"), ast.textsOf(COMMAND_PARAMETER_SELECTOR))
        assertEquals(listOf("\"printf test\""), ast.textsOf(DIRECT_PARAMETER_VAL))
    }

    fun testGeneratedCommandParameterSelectorIsInactiveOutsideFallbackContext() {
        val builder = createBuilder("default answer \"Name\"")

        assertFalse(AppleScriptParser.commandParameterSelector(builder, 0))
        assertEquals("default", builder.tokenText)
    }

    fun testCommandParameterExpressionRestoresOuterParserContext() {
        for (previousContext in COMMAND_PARAMETER_CONTEXT_VALUES) {
            val builder = createBuilder("1")
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS,
                previousContext,
            )

            assertTrue(AppleScriptGeneratedParserUtil.parseCommandParametersExpression(builder, 0))

            assertEquals(
                previousContext,
                builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS),
            )
        }
    }

    fun testDictionaryCommandParametersRestoreOuterParserContext() {
        for (previousContext in COMMAND_PARAMETER_CONTEXT_VALUES) {
            val builder = createBuilder("")
            val command = emptyDictionaryCommand()
            builder.putUserData(
                AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS,
                previousContext,
            )

            parseWithRootSection(builder) {
                DictionaryCommandParameterParser.parseParametersForCommand(builder, 0, command)
            }

            assertEquals(
                previousContext,
                builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS),
            )
        }
    }

    fun testDictionaryCommandValueStopsBeforeNextSelector() {
        val builder = createBuilder("(lineText & linefeed) to fh starting at eof")
        val command =
            dictionaryCommand(
                name = "write",
                directParameterType = "anything",
                parameters = listOf("to", "starting at"),
            )

        val ast =
            parseWithRootSection(builder) {
                DictionaryCommandParameterParser.parseParametersForCommand(builder, 0, command)
            }

        assertEquals(listOf("to", "starting at"), ast.textsOf(COMMAND_PARAMETER_SELECTOR))
    }

    fun testDictionaryNumberDirectParameterKeepsFullExpression() {
        val builder = createBuilder("1 + upperLimit from 0 to 10")
        val command =
            dictionaryCommand(
                name = "random number",
                directParameterType = "number",
                parameters = listOf("from", "to"),
            )

        val ast =
            parseWithRootSection(builder) {
                DictionaryCommandParameterParser.parseParametersForCommand(builder, 0, command)
            }

        assertEquals(listOf("1 + upperLimit"), ast.textsOf(DIRECT_PARAMETER_VAL))
        assertEquals(listOf("from", "to"), ast.textsOf(COMMAND_PARAMETER_SELECTOR))
    }

    fun testDictionaryNumberDirectParameterKeepsIdentifierExpression() {
        val builder = createBuilder("hhour * danswer")
        val command =
            dictionaryCommand(
                name = "delay",
                directParameterType = "number",
                parameters = emptyList(),
            )

        val ast =
            parseWithRootSection(builder) {
                DictionaryCommandParameterParser.parseParametersForCommand(builder, 0, command)
            }

        assertEquals(listOf("hhour * danswer"), ast.textsOf(DIRECT_PARAMETER_VAL))
    }

    fun testDictionaryBracketedDirectParameterKeepsExpressionPsiBeforeSelector() {
        val builder = createBuilder("{docRef} saving no")
        val command =
            dictionaryCommand(
                name = "close",
                directParameterType = "specifier",
                parameters = listOf("saving"),
            )

        val ast =
            parseWithRootSection(builder) {
                DictionaryCommandParameterParser.parseParametersForCommand(builder, 0, command)
            }

        assertEquals(listOf("{docRef}"), ast.textsOf(DIRECT_PARAMETER_VAL))
        assertTrue(ast.textsOf(REFERENCE_EXPRESSION).contains("docRef"))
        assertEquals(listOf("saving"), ast.textsOf(COMMAND_PARAMETER_SELECTOR))
    }

    fun testDictionaryModifierListDirectParameterKeepsSelectorAfterRawBracketFallback() {
        val builder = createBuilder("{command down} using selectedWindow")
        val command =
            dictionaryCommand(
                name = "keystroke",
                directParameterType = "anything",
                parameters = listOf("using"),
            )

        val ast =
            parseWithRootSection(builder) {
                DictionaryCommandParameterParser.parseParametersForCommand(builder, 0, command)
            }

        assertEquals(listOf("{command down}"), ast.textsOf(DIRECT_PARAMETER_VAL))
        assertEquals(listOf("using"), ast.textsOf(COMMAND_PARAMETER_SELECTOR))
    }

    private fun createBuilder(text: String): PsiBuilder {
        val parserDefinition = AppleScriptParserDefinition()
        val anchorFile = myFixture.configureByText(AppleScriptFileType, "")
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(
                project,
                anchorFile.node,
                parserDefinition.createLexer(project),
                AppleScriptLanguage,
                text,
            )
        return adapt_builder_(
            parserDefinition.fileNodeType,
            builder,
            AppleScriptParser(),
            AppleScriptParser.EXTENDS_SETS_,
        )
    }

    private fun parseWithRootSection(
        builder: PsiBuilder,
        parse: () -> Boolean,
    ): ASTNode {
        val marker = enter_section_(builder, 0, _COLLAPSE_, null)
        val result = parse()
        assertTrue("fallback parser should consume all command parameters", builder.eof())
        exit_section_(
            builder,
            0,
            marker,
            AppleScriptParserDefinition().fileNodeType,
            result,
            true,
            TRUE_CONDITION,
        )
        assertTrue(result)
        return builder.treeBuilt
    }

    private fun assertNoParserErrors(psiFile: PsiFile) {
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        if (errors.isEmpty()) return

        val text = psiFile.text
        val report =
            errors.joinToString("\n") { error ->
                val offset = error.textRange.startOffset
                val line = text.substring(0, offset).count { it == '\n' } + 1
                val snippet = error.text.replace("\n", "\\n").take(40)
                "  line $line offset $offset: '$snippet' - ${error.errorDescription}"
            }
        fail("fallback command parameter fixture has ${errors.size} parser error(s):\n$report")
    }

    private fun dictionaryCommand(
        name: String,
        directParameterType: String,
        parameters: List<String>,
    ): AppleScriptCommandImpl {
        val suite = stubSuite()
        val xmlTag = stubXmlTag()
        val command = AppleScriptCommandImpl(suite, name, name, xmlTag)
        command.directParameter = CommandDirectParameter(command, directParameterType, null)
        val commandParameters: List<CommandParameter> =
            parameters.map { parameterName ->
                CommandParameterImpl(
                    command,
                    CommandParameterData(name = parameterName, code = "----", type = "anything", optional = true),
                    xmlTag,
                )
            }
        command.parameters = commandParameters
        return command
    }

    private fun emptyDictionaryCommand(): AppleScriptCommand {
        val suite = stubSuite()
        val xmlTag = stubXmlTag()

        return AppleScriptCommandImpl(
            suite,
            "empty command",
            "empt",
            xmlTag,
        )
    }

    private fun stubSuite(): Suite =
        Proxy.newProxyInstance(
            Suite::class.java.classLoader,
            arrayOf(Suite::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "isValid" -> true
                "toString" -> "SuiteStub@${System.identityHashCode(proxy)}"
                else -> null
            }
        } as Suite

    private fun stubXmlTag(): XmlTag =
        Proxy.newProxyInstance(
            XmlTag::class.java.classLoader,
            arrayOf(XmlTag::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "isValid" -> true
                "toString" -> "XmlTagStub@${System.identityHashCode(proxy)}"
                else -> null
            }
        } as XmlTag

    private fun ASTNode.textsOf(elementType: IElementType): List<String> {
        val texts = mutableListOf<String>()
        collectTextsOf(elementType, texts)
        return texts
    }

    private fun ASTNode.collectTextsOf(
        elementType: IElementType,
        texts: MutableList<String>,
    ) {
        if (this.elementType === elementType) {
            texts += text
        }
        var child = firstChildNode
        while (child != null) {
            child.collectTextsOf(elementType, texts)
            child = child.treeNext
        }
    }

    private companion object {
        val COMMAND_PARAMETER_CONTEXT_VALUES: List<Boolean?> = listOf(null, false, true)
    }
}
