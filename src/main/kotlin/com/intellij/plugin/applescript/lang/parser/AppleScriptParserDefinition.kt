package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.ParserDefinition.SpaceRequirements
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.AppleScriptFile
import com.intellij.plugin.applescript.AppleScriptLanguage
import com.intellij.plugin.applescript.lang.lexer._AppleScriptLexer
import com.intellij.plugin.applescript.psi.AppleScriptTypes
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class AppleScriptParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = FlexAdapter(_AppleScriptLexer(null))

    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createParser(project: Project?): PsiParser = AppleScriptParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun createFile(viewProvider: FileViewProvider): PsiFile = AppleScriptFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): SpaceRequirements =
        SpaceRequirements.MAY

    override fun createElement(node: ASTNode): PsiElement = AppleScriptTypes.Factory.createElement(node)

    companion object {
        private val WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
        private val COMMENTS: TokenSet = TokenSet.create(AppleScriptTypes.COMMENT)

        @JvmField
        val FILE: IFileElementType = IFileElementType(AppleScriptLanguage)
    }
}
