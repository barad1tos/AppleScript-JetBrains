// FROZEN CONTRACT — Adding/removing any method here MUST happen in the same commit as the change
// to ParsableScriptSuiteRegistryHelper. Phase 5 (v1.4 PSI work) is the next phase allowed to mutate
// this list.
//
// Phase 4 SERVICE-07 (plan 04-01): reflection-based golden test of the 26 @JvmStatic proxies on
// ParsableScriptSuiteRegistryHelper consumed by the generated parser util
// (AppleScriptGeneratedParserUtil) at ~30 call sites. Any signature drift causes the generated
// parser to fail with NoSuchMethodError at runtime — this gate catches it at compile-against-the-
// frozen-list time, BEFORE the regression hits the parser fast path.
//
// Runs in <100 ms (reflection only, no BasePlatformTestCase, no fixture boot). Wired into the
// default ./gradlew test filter via the parser.* matcher in build.gradle.kts — gates every CI
// run, not just heavy-test runs.
package com.intellij.plugin.applescript.test.parser

import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.parser.ParsableScriptSuiteRegistryHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

class ParserUtilContractTest {
    /**
     * Every entry in [FROZEN_CONTRACT] resolves to an actual method on
     * ParsableScriptSuiteRegistryHelper with the declared parameter types and return type. The
     * test fails with a clear message naming the violating signature so the regression is
     * obvious in CI output.
     */
    @Test
    fun everyFrozenMethodIsCallable() {
        val helper = ParsableScriptSuiteRegistryHelper::class.java
        for (signature in FROZEN_CONTRACT) {
            val methodName = signature.substring(0, signature.indexOf('('))
            val paramsRaw = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'))
            val returnRaw = signature.substring(signature.lastIndexOf(':') + 1)

            val params: Array<Class<*>> =
                if (paramsRaw.isEmpty()) {
                    emptyArray()
                } else {
                    paramsRaw.split(",").map { resolveParamType(it) }.toTypedArray()
                }

            val method =
                try {
                    helper.getMethod(methodName, *params)
                } catch (e: NoSuchMethodException) {
                    fail<Unit>(
                        "FROZEN CONTRACT VIOLATION: ParsableScriptSuiteRegistryHelper.$signature" +
                            " is no longer callable. Generated parser util WILL hit NoSuchMethodError. " +
                            "Restore the @JvmStatic method or coordinate the contract change via BNF + " +
                            "AppleScriptGeneratedParserUtil.java updates in the SAME commit.",
                    )
                    return
                }

            val actualReturn = method.returnType.name
            assertEquals(
                returnRaw,
                actualReturn,
                "FROZEN CONTRACT VIOLATION: return type drift for $signature" +
                    " (expected $returnRaw, got $actualReturn)",
            )
        }
    }

    /**
     * Optional companion check — catches accidental additions to the @JvmStatic surface that
     * should have gone through explicit contract review. If a new @JvmStatic method lands on
     * ParsableScriptSuiteRegistryHelper without an entry in [FROZEN_CONTRACT], this
     * test fails and asks the author to extend the contract list explicitly.
     */
    @Test
    fun noNewJvmStaticMethodsLeakIntoContract() {
        val helper = ParsableScriptSuiteRegistryHelper::class.java
        val publicStaticCount =
            helper.declaredMethods
                .filter { Modifier.isPublic(it.modifiers) }
                .filter { Modifier.isStatic(it.modifiers) }
                .filterNot { it.isSynthetic }
                .count()
        assertEquals(
            FROZEN_CONTRACT.size,
            publicStaticCount,
            "FROZEN CONTRACT EXTENSION: ParsableScriptSuiteRegistryHelper has " +
                "$publicStaticCount public static methods but contract lists " +
                "${FROZEN_CONTRACT.size}. If a new method was added intentionally, " +
                "add it to FROZEN_CONTRACT in this test in the same commit.",
        )
    }

    companion object {
        /**
         * The 26 @JvmStatic methods on ParsableScriptSuiteRegistryHelper. Format:
         * `"methodName(parameterFqn,parameterFqn,...):returnFqn"`
         * Parameter and return types use FQN; primitives use the Java primitive name.
         *
         * This list was captured at Phase 4 Plan 04-01 land time and matches the source 1:1
         * (verified by `rg "^\s*@JvmStatic" src/main/kotlin/.../ParsableScriptSuiteRegistryHelper.kt
         * | wc -l` = 26). Methods 25 and 26 (isInitialized + areAppDictionariesIndexed) were added
         * in Phase 3 (D-01 / D-04 additive facades).
         */
        private val FROZEN_CONTRACT =
            listOf(
                "ensureKnownApplicationInitialized(java.lang.String):boolean",
                "isStdLibClass(java.lang.String):boolean",
                "isApplicationClass(java.lang.String,java.lang.String):boolean",
                "isStdLibClassPluralName(java.lang.String):boolean",
                "isApplicationClassPluralName(java.lang.String,java.lang.String):boolean",
                "isStdClassWithPrefixExist(java.lang.String):boolean",
                "isClassWithPrefixExist(java.lang.String,java.lang.String):boolean",
                "isStdClassPluralWithPrefixExist(java.lang.String):boolean",
                "isClassPluralWithPrefixExist(java.lang.String,java.lang.String):boolean",
                "isStdCommand(java.lang.String):boolean",
                "isApplicationCommand(java.lang.String,java.lang.String):boolean",
                "isCommandWithPrefixExist(java.lang.String,java.lang.String):boolean",
                "isStdCommandWithPrefixExist(java.lang.String):boolean",
                "findStdCommands(com.intellij.openapi.project.Project,java.lang.String):java.util.Collection",
                "findApplicationCommands(com.intellij.openapi.project.Project,java.lang.String,java.lang.String):java.util.List",
                "isStdProperty(java.lang.String):boolean",
                "isApplicationProperty(java.lang.String,java.lang.String):boolean",
                "isStdPropertyWithPrefixExist(java.lang.String):boolean",
                "isPropertyWithPrefixExist(java.lang.String,java.lang.String):boolean",
                "isStdConstant(java.lang.String):boolean",
                "isApplicationConstant(java.lang.String,java.lang.String):boolean",
                "isStdConstantWithPrefixExist(java.lang.String):boolean",
                "isConstantWithPrefixExist(java.lang.String,java.lang.String):boolean",
                "getScriptingAdditions():java.util.HashSet",
                "isInitialized():boolean",
                "areAppDictionariesIndexed():boolean",
            )

        /**
         * Allowlist of parameter types referenced by [FROZEN_CONTRACT]. Resolving via a static
         * Map (instead of `Class.forName(fqn)`) eliminates the CWE-470 unsafe-reflection surface
         * even though every FQN here is a hardcoded literal in the same file. Only String and
         * Project appear as parameter types across the 26 frozen signatures; return types
         * (Collection / List / HashSet / boolean) are compared by name and never resolved through
         * this map.
         *
         * Adding a new entry here is a deliberate contract-extension act: it MUST be paired with
         * a corresponding addition to FROZEN_CONTRACT in the same commit.
         */
        private val PARAM_TYPE_ALLOWLIST: Map<String, Class<*>> =
            mapOf(
                "java.lang.String" to String::class.java,
                "com.intellij.openapi.project.Project" to Project::class.java,
            )

        private fun resolveParamType(fqn: String): Class<*> =
            PARAM_TYPE_ALLOWLIST[fqn] ?: throw IllegalStateException(
                "Parameter type '$fqn' is not in PARAM_TYPE_ALLOWLIST. " +
                    "Add it explicitly (paired with the new FROZEN_CONTRACT entry that uses it) " +
                    "to preserve the contract review surface.",
            )
    }
}
