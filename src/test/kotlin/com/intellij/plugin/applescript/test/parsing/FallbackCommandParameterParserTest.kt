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
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_CLASS_IDENTIFIER_PLURAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL
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
