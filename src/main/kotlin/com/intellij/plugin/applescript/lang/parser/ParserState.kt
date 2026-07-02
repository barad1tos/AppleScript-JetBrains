package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.openapi.util.Key
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.util.Stack

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

    fun withTellSimpleStatement(
        builder: PsiBuilder,
        parse: () -> Boolean,
    ): Boolean =
        withApplicationNameFrame(builder) {
            builder.putUserData(PARSING_TELL_SIMPLE_STATEMENT, true)
            val result = parse()
            // Explicit false, never restore: hasExitedTellSimpleStatement reads `== false`.
            builder.putUserData(PARSING_TELL_SIMPLE_STATEMENT, false)
            result
        }

    fun withTellCompoundStatement(
        builder: PsiBuilder,
        parse: () -> Boolean,
    ): Boolean {
        val wasParsingCompoundStatement = builder.getUserData(PARSING_TELL_COMPOUND_STATEMENT) == true
        return withApplicationNameFrame(builder) {
            builder.putUserData(PARSING_TELL_COMPOUND_STATEMENT, true)
            val result = parse()
            builder.putUserData(PARSING_TELL_COMPOUND_STATEMENT, wasParsingCompoundStatement)
            result
        }
    }

    fun withUsingTermsFromStatement(
        builder: PsiBuilder,
        parse: () -> Boolean,
    ): Boolean {
        val wasParsingUsingTermsFrom =
            builder.getUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT) == true
        return withApplicationNameFrame(builder) {
            builder.putUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT, true)
            val result = parse()
            builder.putUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT, wasParsingUsingTermsFrom)
            result
        }
    }

    fun withTellSimpleObjectReference(
        builder: PsiBuilder,
        parse: () -> Boolean,
    ): Boolean {
        builder.putUserData(PARSING_TELL_SIMPLE_OBJECT_REF, true)
        val result = parse()
        builder.putUserData(PARSING_TELL_SIMPLE_OBJECT_REF, false)
        return result
    }

    fun isInsideTellSimpleStatement(builder: PsiBuilder): Boolean =
        builder.getUserData(PARSING_TELL_SIMPLE_STATEMENT) == true

    /** Explicit `false` only — `null` means a simple tell was never attempted (tri-state). */
    fun hasExitedTellSimpleStatement(builder: PsiBuilder): Boolean =
        builder.getUserData(PARSING_TELL_SIMPLE_STATEMENT) == false

    fun isInsideTellCompoundStatement(builder: PsiBuilder): Boolean =
        builder.getUserData(PARSING_TELL_COMPOUND_STATEMENT) == true

    fun isInsideUsingTermsFromStatement(builder: PsiBuilder): Boolean =
        builder.getUserData(IS_PARSING_USING_TERMS_FROM_STATEMENT) == true

    fun isInsideTellSimpleObjectReference(builder: PsiBuilder): Boolean =
        builder.getUserData(PARSING_TELL_SIMPLE_OBJECT_REF) == true

    // No try/finally: historical parsers leak state on exceptions; changing that would alter
    // observable behavior on cancellation paths.
    private fun withApplicationNameFrame(
        builder: PsiBuilder,
        parse: () -> Boolean,
    ): Boolean {
        val wasApplicationNamePushed = builder.getUserData(APPLICATION_NAME_PUSHED) == true
        builder.putUserData(APPLICATION_NAME_PUSHED, false)
        val result = parse()
        // Pop must precede the flag restore: popApplicationNameIfWasPushed consults the flag.
        ParserApplicationNameStack.popApplicationNameIfWasPushed(builder)
        builder.putUserData(APPLICATION_NAME_PUSHED, wasApplicationNamePushed)
        return result
    }
}

internal object ParserApplicationNameStack {
    fun getTargetApplicationName(builder: PsiBuilder): String =
        peekTargetApplicationName(builder) ?: ApplicationDictionary.COCOA_STANDARD_LIBRARY

    fun peekTargetApplicationName(builder: PsiBuilder): String? {
        val applicationNameStack = builder.getUserData(AppleScriptGeneratedParserUtil.TOLD_APPLICATION_NAME_STACK)
        return if (!applicationNameStack.isNullOrEmpty()) applicationNameStack.peek() else null
    }

    fun pushTargetApplicationName(
        builder: PsiBuilder,
        applicationNameString: String,
    ): Stack<String> {
        val dictionaryNameStack =
            builder.getUserData(AppleScriptGeneratedParserUtil.TOLD_APPLICATION_NAME_STACK) ?: Stack<String>().also {
                builder.putUserData(AppleScriptGeneratedParserUtil.TOLD_APPLICATION_NAME_STACK, it)
            }
        dictionaryNameStack.push(applicationNameString)
        builder.putUserData(ParserState.APPLICATION_NAME_PUSHED, true)
        return dictionaryNameStack
    }

    fun popApplicationNameIfWasPushed(builder: PsiBuilder) {
        if (builder.getUserData(ParserState.APPLICATION_NAME_PUSHED) == true) {
            val dictionaryNameStack = builder.getUserData(AppleScriptGeneratedParserUtil.TOLD_APPLICATION_NAME_STACK)
            if (!dictionaryNameStack.isNullOrEmpty()) {
                dictionaryNameStack.pop()
            }
        }
    }
}
