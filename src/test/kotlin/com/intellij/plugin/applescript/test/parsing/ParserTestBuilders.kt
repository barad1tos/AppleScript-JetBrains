package com.intellij.plugin.applescript.test.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.parser.GeneratedParserUtilBase.adapt_builder_
import com.intellij.plugin.applescript.AppleScriptFileType
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.lang.parser.AppleScriptParser
import com.intellij.plugin.applescript.lang.parser.AppleScriptParserDefinition
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

/**
 * Builds a real, Grammar-Kit-adapted [PsiBuilder] over [text] for direct parser-level tests.
 *
 * The empty anchor file keeps the builder attached to a project so parser helpers that read
 * project services behave as in production parsing.
 */
internal fun CodeInsightTestFixture.createAppleScriptBuilder(text: String): PsiBuilder {
    val parserDefinition = AppleScriptParserDefinition()
    val anchorFile = configureByText(AppleScriptFileType, "")
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
