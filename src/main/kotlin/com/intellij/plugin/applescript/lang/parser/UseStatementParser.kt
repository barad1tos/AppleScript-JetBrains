package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser
import com.intellij.lang.parser.GeneratedParserUtilBase._NONE_
import com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken
import com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_
import com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.dictionary.discovery.ApplicationDiscoveryService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.psi.AppleScriptTypes.APP
import com.intellij.plugin.applescript.psi.AppleScriptTypes.APPLICATION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.APPLICATION_REFERENCE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DICTIONARY_CLASS_NAME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ID
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SCRIPTING_ADDITIONS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THE_KW

internal object UseStatementParser {
    fun parseUsedApplicationNameExternal(
        builder: PsiBuilder,
        level: Int,
        isImporting: Parser,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseUsedApplicationNameExternal") &&
            nextTokenIs(builder, "parseUsedApplicationNameExternal", APPLICATION, APP, SCRIPTING_ADDITIONS)
        ) {
            val applicationName = parseApplicationNameReference(builder, level)
            result = applicationName != null
            registerUsedApplicationName(builder, isImporting.parse(builder, level + 1), applicationName)
        }
        return result
    }

    fun parseUseStatement(
        builder: PsiBuilder,
        level: Int,
        useStatement: Parser,
    ): Boolean {
        if (!recursion_guard_(builder, level, "parseUseStatement")) return false
        val result = useStatement.parse(builder, level + 1)
        val previousPass = builder.getUserData(AppleScriptGeneratedParserUtil.WAS_USE_STATEMENT_USED) == true
        builder.putUserData(AppleScriptGeneratedParserUtil.WAS_USE_STATEMENT_USED, result || previousPass)
        return result
    }

    fun pushStdLibrary(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        if (!recursion_guard_(builder, level, "pushStdLibrary")) return false
        val result = consumeToken(builder, SCRIPTING_ADDITIONS)
        if (result) {
            ParserApplicationNameStack.pushTargetApplicationName(
                builder,
                ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY,
            )
        }
        return result
    }

    fun parseApplicationName(
        builder: PsiBuilder,
        level: Int,
        tellStatementStartCondition: Parser,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseApplicationName")) {
            consumeToken(builder, THE_KW)
            val hasApplicationToken = parseApplicationNameClassToken(builder, level)
            result = hasApplicationToken && parseApplicationNameLiteral(builder, level, tellStatementStartCondition)
        }
        return result
    }

    private fun parseApplicationNameReference(
        builder: PsiBuilder,
        level: Int,
    ): String? {
        var result = consumeToken(builder, SCRIPTING_ADDITIONS)
        val applicationName =
            if (result) {
                ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
            } else {
                val applicationReferenceMarker = enter_section_(builder, level, _NONE_, "<application reference>")
                val classMarker = enter_section_(builder, level, _NONE_, "<dictionary class name>")
                result = consumeToken(builder, APPLICATION)
                if (!result) result = consumeToken(builder, APP)
                exit_section_(builder, level, classMarker, DICTIONARY_CLASS_NAME, result, false, null)
                val referencedApplicationName = parseReferencedApplicationName(builder, result)
                result = referencedApplicationName != null
                exit_section_(builder, level, applicationReferenceMarker, APPLICATION_REFERENCE, result, false, null)
                referencedApplicationName
            }
        return applicationName
    }

    private fun parseReferencedApplicationName(
        builder: PsiBuilder,
        hasApplicationToken: Boolean,
    ): String? = if (hasApplicationToken) parseApplicationNameToken(builder) else null

    private fun registerUsedApplicationName(
        builder: PsiBuilder,
        isImporting: Boolean,
        applicationName: String?,
    ) {
        if (isImporting && !StringUtil.isEmpty(applicationName)) {
            val usedApplicationNames: Set<String> =
                builder.getUserData(AppleScriptGeneratedParserUtil.USED_APPLICATION_NAMES).orEmpty() +
                    requireNotNull(applicationName)
            builder.putUserData(AppleScriptGeneratedParserUtil.USED_APPLICATION_NAMES, usedApplicationNames)
        }
    }

    private fun parseApplicationNameClassToken(
        builder: PsiBuilder,
        level: Int,
    ): Boolean {
        val classMarker = enter_section_(builder, level, _NONE_, "<parse application name>")
        var hasApplicationToken = consumeToken(builder, APPLICATION)
        if (!hasApplicationToken) hasApplicationToken = consumeToken(builder, APP)
        exit_section_(builder, level, classMarker, DICTIONARY_CLASS_NAME, hasApplicationToken, false, null)
        return hasApplicationToken
    }

    private fun parseApplicationNameLiteral(
        builder: PsiBuilder,
        level: Int,
        tellStatementStartCondition: Parser,
    ): Boolean {
        var result = false
        if (nextTokenIs(builder, "", STRING_LITERAL, ID)) {
            val applicationNameString = parseApplicationNameToken(builder)
            result = applicationNameString != null
            if (result &&
                !StringUtil.isEmptyOrSpaces(applicationNameString) &&
                tellStatementStartCondition.parse(builder, level + 1)
            ) {
                ParserApplicationNameStack.pushTargetApplicationName(builder, requireNotNull(applicationNameString))
            }
        }
        return result
    }

    private fun parseApplicationNameToken(builder: PsiBuilder): String? {
        val isApplicationIdReference = consumeToken(builder, ID)
        val applicationNameString = builder.tokenText?.replace("\"", "")
        val result = consumeToken(builder, STRING_LITERAL) || (!isApplicationIdReference && consumeToken(builder, ID))
        return applicationNameString
            ?.takeIf { result && !StringUtil.isEmptyOrSpaces(it) }
            ?.let { applicationName ->
                if (isApplicationIdReference) {
                    ApplicationDiscoveryService
                        .getInstance()
                        .findKnownApplicationNameByBundleIdentifier(applicationName) ?: applicationName
                } else {
                    applicationName
                }
            }
    }
}
