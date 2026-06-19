package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_PROPERTY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SET
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER
import com.intellij.psi.tree.IElementType

internal object CommandHandlerAssignmentGuard {
    fun isObjectOperandBeforeTerminator(builder: PsiBuilder): Boolean =
        builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_ASSIGNMENT_STATEMENT) == true &&
            builder.lookAhead(1) === TO &&
            isObjectPointer(AppleScriptParserTrivia.previousNonSpaceToken(builder))

    fun isTargetPhraseBeforeTerminator(builder: PsiBuilder): Boolean =
        builder.getUserData(AppleScriptGeneratedParserUtil.PARSING_COMMAND_ASSIGNMENT_STATEMENT) == true &&
            isAssignmentTargetIntroducer(AppleScriptParserTrivia.previousNonSpaceToken(builder)) &&
            hasAssignmentTerminatorAfterTargetPhrase(builder)

    private fun isAssignmentTargetIntroducer(tokenType: IElementType?): Boolean =
        tokenType === SET || isObjectPointer(tokenType)

    private fun hasAssignmentTerminatorAfterTargetPhrase(builder: PsiBuilder): Boolean {
        var offset = 0
        var tokenType = builder.tokenType
        var consumedTargetWord = false
        while (isAssignmentTargetWord(tokenType)) {
            consumedTargetWord = true
            offset += 1
            tokenType = builder.lookAhead(offset)
        }
        return consumedTargetWord && tokenType === TO
    }

    private fun isAssignmentTargetWord(tokenType: IElementType?): Boolean =
        tokenType === VAR_IDENTIFIER ||
            tokenType === BUILT_IN_PROPERTY ||
            tokenType === SET ||
            FallbackDictionaryTermPredicates.isContextualPropertyTerm(tokenType)

    private fun isObjectPointer(tokenType: IElementType?): Boolean = tokenType === OF || tokenType === IN
}
