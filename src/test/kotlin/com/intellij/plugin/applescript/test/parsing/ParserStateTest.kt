package com.intellij.plugin.applescript.test.parsing

import com.intellij.plugin.applescript.lang.parser.ParserApplicationNameStack
import com.intellij.plugin.applescript.lang.parser.ParserState
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ParserStateTest : BasePlatformTestCase() {
    fun testSimpleTellSetsFlagInsideAndLeavesExplicitFalseAfter() {
        val builder = myFixture.createAppleScriptBuilder("")
        assertNull(builder.getUserData(ParserState.PARSING_TELL_SIMPLE_STATEMENT))

        val result =
            ParserState.withTellSimpleStatement(builder) {
                assertEquals(true, builder.getUserData(ParserState.PARSING_TELL_SIMPLE_STATEMENT))
                true
            }

        assertTrue(result)
        // Explicit false, not null: isInTellStatement distinguishes the two.
        assertEquals(false, builder.getUserData(ParserState.PARSING_TELL_SIMPLE_STATEMENT))
    }

    fun testNestedCompoundTellRestoresOuterFlagAndCoercesToExplicitFalse() {
        val builder = myFixture.createAppleScriptBuilder("")

        ParserState.withTellCompoundStatement(builder) {
            ParserState.withTellCompoundStatement(builder) { true }
            // Inner exit must not clear the outer compound context.
            assertEquals(true, builder.getUserData(ParserState.PARSING_TELL_COMPOUND_STATEMENT))
            true
        }

        // Outer prior state was null; historical code restores it coerced to explicit false.
        assertEquals(false, builder.getUserData(ParserState.PARSING_TELL_COMPOUND_STATEMENT))
    }

    fun testUsingTermsFromRestoresPriorTrue() {
        val builder = myFixture.createAppleScriptBuilder("")

        ParserState.withUsingTermsFromStatement(builder) {
            ParserState.withUsingTermsFromStatement(builder) { true }
            assertEquals(true, builder.getUserData(ParserState.IS_PARSING_USING_TERMS_FROM_STATEMENT))
            true
        }

        assertEquals(false, builder.getUserData(ParserState.IS_PARSING_USING_TERMS_FROM_STATEMENT))
    }

    fun testTellFramePopsApplicationNamePushedInsideIt() {
        val builder = myFixture.createAppleScriptBuilder("")

        ParserState.withTellCompoundStatement(builder) {
            ParserApplicationNameStack.pushTargetApplicationName(builder, "Finder")
            assertEquals("Finder", ParserApplicationNameStack.getTargetApplicationName(builder))
            true
        }

        assertNull(ParserApplicationNameStack.peekTargetApplicationName(builder))
    }

    fun testTellFrameWithoutPushLeavesOuterNameAlone() {
        val builder = myFixture.createAppleScriptBuilder("")
        ParserApplicationNameStack.pushTargetApplicationName(builder, "Music")

        ParserState.withTellCompoundStatement(builder) { true }

        assertEquals("Music", ParserApplicationNameStack.peekTargetApplicationName(builder))
    }

    fun testNestedTellFramesRestoreOuterApplicationName() {
        val builder = myFixture.createAppleScriptBuilder("")

        ParserState.withTellCompoundStatement(builder) {
            ParserApplicationNameStack.pushTargetApplicationName(builder, "Music")
            ParserState.withTellCompoundStatement(builder) {
                ParserApplicationNameStack.pushTargetApplicationName(builder, "Finder")
                assertEquals("Finder", ParserApplicationNameStack.peekTargetApplicationName(builder))
                true
            }
            assertEquals("Music", ParserApplicationNameStack.peekTargetApplicationName(builder))
            true
        }

        assertNull(ParserApplicationNameStack.peekTargetApplicationName(builder))
    }

    fun testTellSimpleObjectReferenceFlagClearsAfterRun() {
        val builder = myFixture.createAppleScriptBuilder("")

        ParserState.withTellSimpleObjectReference(builder) {
            assertEquals(true, builder.getUserData(ParserState.PARSING_TELL_SIMPLE_OBJECT_REF))
            true
        }

        assertEquals(false, builder.getUserData(ParserState.PARSING_TELL_SIMPLE_OBJECT_REF))
    }
}
