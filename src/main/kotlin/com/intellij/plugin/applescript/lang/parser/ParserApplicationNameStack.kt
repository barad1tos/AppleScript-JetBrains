package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import java.util.Stack

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
        builder.putUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED, true)
        return dictionaryNameStack
    }

    fun popApplicationNameIfWasPushed(builder: PsiBuilder) {
        if (builder.getUserData(AppleScriptGeneratedParserUtil.APPLICATION_NAME_PUSHED) == true) {
            val dictionaryNameStack = builder.getUserData(AppleScriptGeneratedParserUtil.TOLD_APPLICATION_NAME_STACK)
            if (!dictionaryNameStack.isNullOrEmpty()) {
                dictionaryNameStack.pop()
            }
        }
    }
}
