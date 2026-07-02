package com.intellij.plugin.applescript.lang.parser

import com.intellij.openapi.util.Key

/**
 * Single owner of the mutable tell/use parser state that AppleScript parser helpers share
 * through PsiBuilder user data.
 *
 * Contract: flag keys are tri-state — `null` (never entered), `true` (inside), `false`
 * (entered and exited). Readers such as [TellStatementParser.isInTellStatement] distinguish
 * explicit `false` from `null`, so writers must preserve the historical write sequences.
 *
 * [AppleScriptGeneratedParserUtil.TOLD_APPLICATION_NAME_STACK] stays in the generated-parser
 * facade: it is ABI-frozen by AppleScriptGeneratedParserUtilJvmSignatureTest.
 */
internal object ParserState {
    val PARSING_TELL_SIMPLE_STATEMENT: Key<Boolean> =
        Key.create("applescript.parsing.tell.simple.statement")
    val PARSING_TELL_SIMPLE_OBJECT_REF: Key<Boolean> =
        Key.create("applescript.parsing.tell.simple.object.ref")
    val PARSING_TELL_COMPOUND_STATEMENT: Key<Boolean> =
        Key.create("applescript.parsing.tell.compound.statement")
    val APPLICATION_NAME_PUSHED: Key<Boolean> =
        Key.create("applescript.parsing.tell.statement.application.name.pushed")
    val USED_APPLICATION_NAMES: Key<Set<String>> =
        Key.create("applescript.parsing.use.statement.application.name.set")
    val WAS_USE_STATEMENT_USED: Key<Boolean> =
        Key.create("applescript.parsing.is.use.statement.used")
    val IS_PARSING_USING_TERMS_FROM_STATEMENT: Key<Boolean> =
        Key.create("applescript.parsing.using.terms.from.statement")
    val PARSING_COMMAND_ASSIGNMENT_STATEMENT: Key<Boolean> =
        Key.create("applescript.parsing.assignment.statement")
    val PARSING_LITERAL_EXPRESSION: Key<Boolean> =
        Key.create("applescript.parsing.literal.expression")
}
