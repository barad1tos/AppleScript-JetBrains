package com.intellij.plugin.applescript.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_
import com.intellij.openapi.util.Ref
import com.intellij.plugin.applescript.psi.AppleScriptTypes.VAR_IDENTIFIER

internal object FallbackCommandParser {
    fun parseCommandName(
        builder: PsiBuilder,
        level: Int,
        parsedName: Ref<String>,
    ): Boolean {
        var result = false
        if (recursion_guard_(builder, level, "parseWellKnownCommandFallback") &&
            builder.tokenType === VAR_IDENTIFIER
        ) {
            result =
                KnownFallbackCommandNameParser.parse(builder, parsedName) ||
                GenericFallbackCommandHeadParser.parse(builder, parsedName)
        }
        return result
    }
}
