package com.intellij.plugin.applescript.test.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.parser.GeneratedParserUtilBase.TRUE_CONDITION
import com.intellij.lang.parser.GeneratedParserUtilBase._COLLAPSE_
import com.intellij.lang.parser.GeneratedParserUtilBase.adapt_builder_
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.plugin.applescript.lang.parser.AppleScriptGeneratedParserUtil
import com.intellij.plugin.applescript.lang.parser.AppleScriptParser
import com.intellij.plugin.applescript.lang.parser.AppleScriptParserDefinition
import com.intellij.plugin.applescript.lang.parser.FallbackCommandParameterParser
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMAND_PARAMETER_SELECTOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIRECT_PARAMETER_VAL
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FallbackCommandParameterParserTest : BasePlatformTestCase() {
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
        val builder = createBuilder("1")
        builder.putUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS, true)

        assertTrue(AppleScriptGeneratedParserUtil.parseCommandParametersExpression(builder, 0))

        assertEquals(true, builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_HANDLER_CALL_PARAMETERS))
    }

    private fun createBuilder(text: String): PsiBuilder {
        val parserDefinition = AppleScriptParserDefinition()
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(
                parserDefinition,
                parserDefinition.createLexer(project),
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
}
