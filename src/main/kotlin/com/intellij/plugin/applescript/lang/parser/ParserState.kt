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
    val PARSING_TELL_SIMPLE_OBJECT_REFERENCE: Key<Boolean> =
        Key.create("applescript.parsing.tell.simple.object.reference")
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

    // Runners are inline with crossinline lambdas: parsing is the editor's reparse hot path, so
    // the scoped writes compile to the same allocation-free straight-line code they replaced, and
    // crossinline makes a non-local return that would skip the exit writes a compile error.
    inline fun withTellSimpleStatement(
        builder: PsiBuilder,
        crossinline parse: () -> Boolean,
    ): Boolean =
        withApplicationNameFrame(builder) {
            // Explicit false, never restore: hasExitedTellSimpleStatement reads `== false`.
            withScopedFlag(builder, PARSING_TELL_SIMPLE_STATEMENT) { parse() }
        }

    inline fun withTellCompoundStatement(
        builder: PsiBuilder,
        crossinline parse: () -> Boolean,
    ): Boolean {
        val wasParsingCompoundStatement = builder.getUserData(PARSING_TELL_COMPOUND_STATEMENT) == true
        return withApplicationNameFrame(builder) {
            builder.putUserData(PARSING_TELL_COMPOUND_STATEMENT, true)
            val result = parse()
            builder.putUserData(PARSING_TELL_COMPOUND_STATEMENT, wasParsingCompoundStatement)
            result
        }
    }

    inline fun withUsingTermsFromStatement(
        builder: PsiBuilder,
        crossinline parse: () -> Boolean,
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

    inline fun withTellSimpleObjectReference(
        builder: PsiBuilder,
        crossinline parse: () -> Boolean,
    ): Boolean = withScopedFlag(builder, PARSING_TELL_SIMPLE_OBJECT_REFERENCE, parse)

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
        builder.getUserData(PARSING_TELL_SIMPLE_OBJECT_REFERENCE) == true

    inline fun withAssignmentStatement(
        builder: PsiBuilder,
        crossinline parse: () -> Boolean,
    ): Boolean = withScopedFlag(builder, PARSING_COMMAND_ASSIGNMENT_STATEMENT, parse)

    inline fun withLiteralExpression(
        builder: PsiBuilder,
        crossinline parse: () -> Boolean,
    ): Boolean = withScopedFlag(builder, PARSING_LITERAL_EXPRESSION, parse)

    fun isInsideAssignmentStatement(builder: PsiBuilder): Boolean =
        builder.getUserData(PARSING_COMMAND_ASSIGNMENT_STATEMENT) == true

    fun isInsideLiteralExpression(builder: PsiBuilder): Boolean =
        builder.getUserData(PARSING_LITERAL_EXPRESSION) == true

    /**
     * Write-through by design: PsiBuilder marker rollback does not rewind user data, so a use
     * statement stays recorded even when an enclosing parse attempt rolls back.
     */
    fun recordUseStatementOutcome(
        builder: PsiBuilder,
        parsed: Boolean,
    ) {
        val previousPass = builder.getUserData(WAS_USE_STATEMENT_USED) == true
        builder.putUserData(WAS_USE_STATEMENT_USED, parsed || previousPass)
    }

    fun recordUsedApplicationName(
        builder: PsiBuilder,
        applicationName: String,
    ) {
        val usedApplicationNames = builder.getUserData(USED_APPLICATION_NAMES).orEmpty() + applicationName
        builder.putUserData(USED_APPLICATION_NAMES, usedApplicationNames)
    }

    fun areThereUseStatements(builder: PsiBuilder): Boolean = builder.getUserData(WAS_USE_STATEMENT_USED) == true

    fun usedApplicationNamesForLookup(
        builder: PsiBuilder,
        areThereUseStatements: Boolean = areThereUseStatements(builder),
    ): Set<String>? = if (areThereUseStatements) builder.getUserData(USED_APPLICATION_NAMES) else null

    // The two helpers below are implementation seams for the inline runners above (a Kotlin
    // inline function cannot call private members); call the named runners, not these.
    // No try/finally anywhere here: historical parsers leak state on exceptions; changing that
    // would alter observable behavior on cancellation paths.

    /** One implementation of the tri-state exit contract: set `true`, parse, write explicit `false`. */
    inline fun withScopedFlag(
        builder: PsiBuilder,
        flag: Key<Boolean>,
        crossinline parse: () -> Boolean,
    ): Boolean {
        builder.putUserData(flag, true)
        val result = parse()
        builder.putUserData(flag, false)
        return result
    }

    inline fun withApplicationNameFrame(
        builder: PsiBuilder,
        crossinline parse: () -> Boolean,
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
