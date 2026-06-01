package com.intellij.plugin.applescript.test.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
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
                    " (expected ${signature.returnTypeName}, got ${method.returnType.name})"
            )
            assertTrue(
                Modifier.isStatic(method.modifiers),
                "FROZEN CONTRACT VIOLATION: ${signature.render()} is no longer static. " +
                    "Generated Java parser code calls this method as a static parser-util entry point."
            )
            assertEquals(
                signature.visibility,
                Visibility.fromModifiers(method.modifiers),
                "FROZEN CONTRACT VIOLATION: visibility drift for ${signature.render()}"
            )
        }
    }

    private fun findMethod(
        parserUtil: Class<AppleScriptGeneratedParserUtil>,
        signature: FrozenSignature
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
                    "method shape or coordinate the BNF/generated-parser contract change in the same commit."
            )
        }
    }

    companion object {
        private val FROZEN_CONTRACT = listOf(
            FrozenSignature.publicStatic(
                "parseCommandHandlerCallExpression",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseApplicationHandlerDefinitionSignature",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseAssignmentStatementInner",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseLiteralExpression",
                "boolean",
                PSI_BUILDER,
                INT,
                PARSER
            ),
            FrozenSignature.publicStatic(
                "parseTellSimpleStatementInner",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseTellSimpleObjectReference",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseUsedApplicationNameExternal",
                "boolean",
                PSI_BUILDER,
                INT,
                PARSER
            ),
            FrozenSignature.publicStatic(
                "parseUseStatement",
                "boolean",
                PSI_BUILDER,
                INT,
                PARSER
            ),
            FrozenSignature.publicStatic(
                "parseExpression",
                "boolean",
                PSI_BUILDER,
                INT,
                STRING,
                PARSER
            ),
            FrozenSignature.publicStatic(
                "parseTellCompoundStatement",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseUsingTermsFromStatement",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "pushStdLibrary",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseApplicationName",
                "boolean",
                PSI_BUILDER,
                INT,
                PARSER
            ),
            FrozenSignature.publicStatic(
                "isTellStatementStart",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.packageStatic(
                "typeSpecifier",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.packageStatic(
                "singularClassName",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseDictionaryPropertyName",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseDictionaryClassName",
                "boolean",
                PSI_BUILDER,
                INT,
                BOOLEAN,
                PARSER
            ),
            FrozenSignature.publicStatic(
                "parseCheckForUseStatements",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseCommandParametersExpression",
                "boolean",
                PSI_BUILDER,
                INT
            ),
            FrozenSignature.publicStatic(
                "parseDictionaryConstant",
                "boolean",
                PSI_BUILDER,
                INT
            )
        )

        private const val PSI_BUILDER = "com.intellij.lang.PsiBuilder"
        private const val INT = "int"
        private const val BOOLEAN = "boolean"
        private const val STRING = "java.lang.String"
        private const val PARSER = "com.intellij.lang.parser.GeneratedParserUtilBase.Parser"

        private val PARAM_TYPE_ALLOWLIST: Map<String, Class<*>> = mapOf(
            PSI_BUILDER to PsiBuilder::class.java,
            INT to Integer.TYPE,
            BOOLEAN to java.lang.Boolean.TYPE,
            STRING to String::class.java,
            PARSER to GeneratedParserUtilBase.Parser::class.java
        )

        private fun resolveParamType(typeName: String): Class<*> =
            PARAM_TYPE_ALLOWLIST[typeName] ?: throw IllegalStateException(
                "Parameter type '$typeName' is not in PARAM_TYPE_ALLOWLIST. Add it explicitly " +
                    "with the frozen parser-util signature that uses it to keep reflection review safe."
            )
    }

    private data class FrozenSignature(
        val name: String,
        val returnTypeName: String,
        val visibility: Visibility,
        val parameterTypeNames: List<String>
    ) {
        fun render(): String =
            "$name(${parameterTypeNames.joinToString(",")}):$returnTypeName"

        companion object {
            fun publicStatic(
                name: String,
                returnTypeName: String,
                vararg parameterTypeNames: String
            ): FrozenSignature =
                FrozenSignature(name, returnTypeName, Visibility.PUBLIC, parameterTypeNames.toList())

            fun packageStatic(
                name: String,
                returnTypeName: String,
                vararg parameterTypeNames: String
            ): FrozenSignature =
                FrozenSignature(name, returnTypeName, Visibility.PACKAGE, parameterTypeNames.toList())
        }
    }

    private enum class Visibility {
        PUBLIC,
        PACKAGE;

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
