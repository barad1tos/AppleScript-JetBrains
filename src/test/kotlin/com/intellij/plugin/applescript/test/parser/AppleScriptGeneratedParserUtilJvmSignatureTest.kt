package com.intellij.plugin.applescript.test.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.plugin.applescript.lang.parser.AppleScriptGeneratedParserUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class AppleScriptGeneratedParserUtilJvmSignatureTest {
    @Test
    fun everyFrozenGeneratedParserUtilSignatureIsCallable() {
        val parserUtil = AppleScriptGeneratedParserUtil::class.java
        for (signature in FROZEN_CONTRACT) {
            val method = findMethod(parserUtil, signature)

            assertEquals(
                signature.returnTypeName,
                method.returnType.name,
                "FROZEN CONTRACT VIOLATION: return type drift for ${signature.render()}" +
                    " (expected ${signature.returnTypeName}, got ${method.returnType.name})",
            )
            assertTrue(
                Modifier.isStatic(method.modifiers),
                "FROZEN CONTRACT VIOLATION: ${signature.render()} is no longer static. " +
                    "Generated Java parser code calls this method as a static parser-util entry point.",
            )
            assertEquals(
                signature.visibility,
                Visibility.fromModifiers(method.modifiers),
                "FROZEN CONTRACT VIOLATION: visibility drift for ${signature.render()}",
            )
        }
    }

    @Test
    fun everyFrozenGeneratedParserUtilFieldIsCallable() {
        val parserUtil = AppleScriptGeneratedParserUtil::class.java
        for (field in FROZEN_FIELDS) {
            val actual =
                try {
                    parserUtil.getField(field.name)
                } catch (exception: NoSuchFieldException) {
                    fail(
                        "FROZEN CONTRACT VIOLATION: AppleScriptGeneratedParserUtil.${field.name} " +
                            "is no longer callable from Java. Restore the @JvmField static field " +
                            "or coordinate the generated-parser contract change in the same commit.",
                        exception,
                    )
                }

            assertEquals(
                field.typeName,
                actual.type.name,
                "FROZEN CONTRACT VIOLATION: field type drift for ${field.name}" +
                    " (expected ${field.typeName}, got ${actual.type.name})",
            )
            assertTrue(
                Modifier.isStatic(actual.modifiers),
                "FROZEN CONTRACT VIOLATION: ${field.name} is no longer static. " +
                    "Generated parser state is stored through this static parser-util key.",
            )
        }
    }

    private fun findMethod(
        parserUtil: Class<AppleScriptGeneratedParserUtil>,
        signature: FrozenSignature,
    ): Method {
        val parameterTypes = signature.parameterTypeNames.map { resolveParamType(it) }.toTypedArray()
        return try {
            when (signature.visibility) {
                Visibility.PUBLIC -> parserUtil.getMethod(signature.name, *parameterTypes)
                Visibility.PACKAGE -> parserUtil.getDeclaredMethod(signature.name, *parameterTypes)
            }
        } catch (exception: NoSuchMethodException) {
            fail(
                "FROZEN CONTRACT VIOLATION: AppleScriptGeneratedParserUtil.${signature.render()} " +
                    "is no longer callable from the generated parser. Restore the JVM-visible " +
                    "method shape or coordinate the BNF/generated-parser contract change in the same commit.",
                exception,
            )
        }
    }

    companion object {
        private val FROZEN_CONTRACT =
            listOf(
                FrozenSignature.publicStatic(
                    "parseCommandHandlerCallExpression",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseApplicationHandlerDefinitionSignature",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseAssignmentStatementInner",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseLiteralExpression",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                    PARSER,
                ),
                FrozenSignature.publicStatic(
                    "parseTellSimpleStatementInner",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseTellSimpleObjectReference",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseUsedApplicationNameExternal",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                    PARSER,
                ),
                FrozenSignature.publicStatic(
                    "parseUseStatement",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                    PARSER,
                ),
                FrozenSignature.publicStatic(
                    "parseExpression",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                    STRING,
                    PARSER,
                ),
                FrozenSignature.publicStatic(
                    "parseTellCompoundStatement",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseUsingTermsFromStatement",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "pushStdLibrary",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseApplicationName",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                    PARSER,
                ),
                FrozenSignature.publicStatic(
                    "isTellStatementStart",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                // Kotlin cannot emit Java package-private @JvmStatic companion methods.
                // These same-package generated-parser helpers must stay static and callable by name.
                FrozenSignature.publicStatic(
                    "typeSpecifier",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "singularClassName",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseCommandParameterSelector",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "isPossessivePronoun",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseDictionaryCommandName",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseIncompleteCommandCall",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseDictionaryPropertyName",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseDictionaryClassName",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                    BOOLEAN,
                    PARSER,
                ),
                FrozenSignature.publicStatic(
                    "parseCheckForUseStatements",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseCommandParametersExpression",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
                FrozenSignature.publicStatic(
                    "parseDictionaryConstant",
                    "boolean",
                    PSI_BUILDER,
                    INT,
                ),
            )

        private val FROZEN_FIELDS =
            listOf(
                FrozenField("TOLD_APPLICATION_NAME_STACK", KEY),
                FrozenField("TOLD_APPLICATION_ID_STACK", KEY),
            )

        private const val PSI_BUILDER = "com.intellij.lang.PsiBuilder"
        private const val INT = "int"
        private const val BOOLEAN = "boolean"
        private const val STRING = "java.lang.String"
        private const val PARSER = "com.intellij.lang.parser.GeneratedParserUtilBase.Parser"
        private const val KEY = "com.intellij.openapi.util.Key"

        private val PARAM_TYPE_ALLOWLIST: Map<String, Class<*>> =
            mapOf(
                PSI_BUILDER to PsiBuilder::class.java,
                INT to Integer.TYPE,
                BOOLEAN to java.lang.Boolean.TYPE,
                STRING to String::class.java,
                PARSER to GeneratedParserUtilBase.Parser::class.java,
                KEY to Key::class.java,
            )

        private fun resolveParamType(typeName: String): Class<*> =
            PARAM_TYPE_ALLOWLIST[typeName] ?: error(
                "Parameter type '$typeName' is not in PARAM_TYPE_ALLOWLIST. Add it explicitly " +
                    "with the frozen parser-util signature that uses it to keep reflection review safe.",
            )
    }

    private data class FrozenSignature(
        val name: String,
        val returnTypeName: String,
        val visibility: Visibility,
        val parameterTypeNames: List<String>,
    ) {
        fun render(): String = "$name(${parameterTypeNames.joinToString(",")}):$returnTypeName"

        companion object {
            fun publicStatic(
                name: String,
                returnTypeName: String,
                vararg parameterTypeNames: String,
            ): FrozenSignature = FrozenSignature(name, returnTypeName, Visibility.PUBLIC, parameterTypeNames.toList())
        }
    }

    private data class FrozenField(
        val name: String,
        val typeName: String,
    )

    private enum class Visibility {
        PUBLIC,
        PACKAGE,
        ;

        companion object {
            fun fromModifiers(modifiers: Int): Visibility =
                if (Modifier.isPublic(modifiers)) {
                    PUBLIC
                } else {
                    PACKAGE
                }
        }
    }
}
