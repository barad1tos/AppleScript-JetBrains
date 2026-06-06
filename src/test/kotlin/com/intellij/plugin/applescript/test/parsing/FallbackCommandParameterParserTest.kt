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
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_CLASS_NAME
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
        // Step-1 (Bucket A) RED: the `of containing set` operand must parse `containing set` as a
        // DICTIONARY_CLASS_NAME (NLS-terminated, minimal `set`-keyword class case). Today the operand
        // parses `containing` as a bare REFERENCE_EXPRESSION and the `set` keyword dangles as a
        // PsiErrorElement (10-RESEARCH §"PSI evidence for the `set` collision"). This assertion flips
        // to GREEN in Plan 10-01 Task 2 once FallbackDictionaryClassParser accepts SET as a 2nd class word.
        val psiFile =
            myFixture.configureByText(
                AppleScriptFileType,
                "tell application \"Typinator\"\nset containerid to unique id of containing set\nend tell",
            )

        assertEquals(listOf("containing set"), psiFile.node.textsOf(DICTIONARY_CLASS_NAME))
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
