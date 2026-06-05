// FROZEN GETTER CONTRACT — Adding/removing any signature here MUST happen in the same commit as the
// property conversion of the declaring interface. This freezes the Java-visible getter names
// produced by converting GROUP A interface getter functions to Kotlin properties
// (val x + @get:JvmName).
//
// Failure mode it catches: a property conversion that drops or renames a Java-visible accessor name
// (e.g. `val classProperty` synthesizing getClassProperty() instead of the required isClassProperty(),
// or a `val displayName` rename silently producing getDisplayName()). The Java consumers
// (AppleScriptGeneratedParserUtil + src/main/gen/*.java) call getter/is-prefixed names by reflection-equivalent
// JVM name at runtime, so a divergence is a runtime NoSuchMethodError — this gate turns that into a
// compile-against-the-frozen-list failure.
//
// Per-interface scoping (Pitfall 3): FROZEN_GETTERS is keyed by the declaring interface Class, NOT a
// flat list, because getParameters() collides across AppleScriptCommand (List<CommandParameter>) and
// AppleScriptHandler (List<AppleScriptHandlerSelectorPart>) with different return types — a flat list
// would mis-assert. Wave 0 (plan 05-01) seeds ONLY AppleScriptPropertyDefinition; later waves extend
// the map as each GROUP A interface converts.
//
// Runs in <100 ms (reflection only, no BasePlatformTestCase, no fixture boot). Wired into the default
// ./gradlew test filter via the psi.* matcher in build.gradle.kts — gates every CI run.
package com.intellij.plugin.applescript.test.psi

import com.intellij.plugin.applescript.lang.sdef.AccessType
import com.intellij.plugin.applescript.lang.sdef.AppleScriptClass
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.DictionaryComponent
import com.intellij.plugin.applescript.lang.sdef.Suite
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

class PsiGetterJvmSignatureTest {
    /**
     * Every signature in [FROZEN_GETTERS] resolves to an actual method on its declaring
     * interface with the declared parameter types and return type. After property conversion the
     * synthesized getter/is-prefixed/setter JVM names MUST still resolve — if a
     * conversion dropped or renamed a Java-visible name, the lookup fails here with a clear message
     * naming the violating signature and the `@get:JvmName` fix.
     */
    @Test
    fun everyConvertedGetterIsCallableFromJavaUnderExpectedName() {
        FROZEN_GETTERS.forEach { (iface, signatures) ->
            for (signature in signatures) {
                val methodName = signature.substring(0, signature.indexOf('('))
                val paramsRaw = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'))
                val returnRaw = signature.substring(signature.lastIndexOf(':') + 1)

                val params: Array<Class<*>> =
                    if (paramsRaw.isEmpty()) {
                        emptyArray()
                    } else {
                        paramsRaw.split(",").map { resolveParamType(it) }.toTypedArray()
                    }

                val method: Method =
                    try {
                        iface.getMethod(methodName, *params)
                    } catch (exception: NoSuchMethodException) {
                        fail(
                            "MISSING JVM GETTER: ${iface.simpleName}.$signature" +
                                " — property conversion dropped or renamed the Java-visible name. " +
                                "Add @get:JvmName(\"$methodName\") (or @set:JvmName for setters) " +
                                "on the converted property to lock the name. Java consumers " +
                                "(AppleScriptGeneratedParserUtil + src/main/gen) call this by JVM name " +
                                "and would hit NoSuchMethodError at runtime.",
                            exception,
                        )
                    }

                val actualReturn = method.returnType.name
                assertEquals(
                    returnRaw,
                    actualReturn,
                    "JVM-NAME / RETURN DRIFT on ${iface.simpleName}.$signature" +
                        " (expected return $returnRaw, got $actualReturn)",
                )
            }
        }
    }

    companion object {
        /**
         * Frozen Java-visible getter contract per declaring GROUP A interface. Format:
         * `"methodName(parameterFqn,...):returnFqn"`
         * Parameter and return types use FQN; primitives use the Java primitive name. No-arg getters
         * have an empty parameter list; the access-type setter carries its single parameter FQN.
         *
         * Keyed by declaring interface `Class` (Pitfall 3) so each assertion is scoped to the
         * exact interface that declares the getter — colliding names across interfaces (e.g.
         * getParameters) resolve against the right declaration.
         *
         * Wave 0 (plan 05-01) seeds only [AppleScriptPropertyDefinition] — the property-conversion
         * pilot. Its 8 accessors exercise every mechanic: plain getter (getTypeSpecifier), nullable
         * getter (getMyClass/getMyRecord), is-prefixed Boolean (isClassProperty/isRecordProperty —
         * the `is` prefix is PRESERVED), and a getter/setter pair (getAccessType/setAccessType →
         * var accessType).
         */
        private val FROZEN_GETTERS: Map<Class<*>, List<String>> =
            mapOf(
                AppleScriptPropertyDefinition::class.java to
                    listOf(
                        "getPsiType():com.intellij.plugin.applescript.lang.sdef.PsiType",
                        "isClassProperty():boolean", // is-prefix PRESERVED
                        "isRecordProperty():boolean", // is-prefix PRESERVED
                        "getMyClass():com.intellij.plugin.applescript.lang.sdef.AppleScriptClass",
                        "getMyRecord():com.intellij.plugin.applescript.lang.sdef.DictionaryRecord",
                        "getAccessType():com.intellij.plugin.applescript.lang.sdef.AccessType",
                        "getTypeSpecifier():java.lang.String",
                        "setAccessType(com.intellij.plugin.applescript.lang.sdef.AccessType):void",
                    ),
                // Wave 2 (plan 05-03) — clean leaf CommandParameter: a 3-getter copy of the pilot.
                // isOptional keeps its is-prefix (Boolean), getTypeSpecifier/getMyCommand are plain getters.
                CommandParameter::class.java to
                    listOf(
                        "isOptional():boolean", // is-prefix PRESERVED
                        "getTypeSpecifier():java.lang.String",
                        "getMyCommand():com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand",
                    ),
                // Wave 2 (plan 05-03) — Suite barely converts: only isHidden() is a pure getter; every
                // add*/find*/get*ByName member stays a fun (arg-taking / mutator) and is NOT frozen here.
                Suite::class.java to
                    listOf(
                        "isHidden():boolean", // is-prefix PRESERVED
                    ),
                // Wave 2 (plan 05-03) — AppleScriptCommand PARSER HOT PATH. getParameters/getDirectParameter/
                // getMandatoryParameters/getParameterNames are called from AppleScriptGeneratedParserUtil.java
                // (lines 366-370, 719-862); a dropped JVM name here is a NoSuchMethodError on first parse.
                // getParameterByName(String) stays fun (arg-taking) and setResult(...) stays fun (returns a
                // value) — neither is a property getter, so neither is frozen as one here.
                AppleScriptCommand::class.java to
                    listOf(
                        "getParameters():java.util.List",
                        "getDirectParameter():com.intellij.plugin.applescript.lang.sdef.CommandDirectParameter",
                        "getMandatoryParameters():java.util.List",
                        "getResult():com.intellij.plugin.applescript.lang.sdef.CommandResult",
                        "getParameterNames():java.util.List",
                    ),
                // Wave 2 (plan 05-03) — AppleScriptClass. getProperties is part of a getProperties/setProperties
                // (Unit-returning) pair → var properties. setPluralClassName(...) RETURNS DictionaryClass and so
                // stays fun (a Kotlin property setter must return Unit) — not frozen as a getter.
                AppleScriptClass::class.java to
                    listOf(
                        "getContents():java.util.List",
                        "getProperties():java.util.List",
                        "getParentClassName():java.lang.String",
                        "getParentClass():com.intellij.plugin.applescript.lang.sdef.AppleScriptClass",
                        "getElementNames():java.util.List",
                        "getElements():java.util.List",
                        "getRespondingCommands():java.util.List",
                        "getPluralClassName():java.lang.String",
                    ),
                // Wave 3 (plan 05-04) — DictionaryComponent: the shared SDEF supertype. Every pure no-arg
                // getter converts to a property; Java-visible names stay getter-style/getSuite()/getName() via
                // property naming. getName() is the override-narrowing seam (overrides PsiNamedElement.getName());
                // setDictionaryDoc(...) stays fun (no matching getter) and is NOT frozen as a getter here.
                DictionaryComponent::class.java to
                    listOf(
                        "getDocumentation():java.lang.String",
                        "getCode():java.lang.String",
                        "getCocoaClassName():java.lang.String",
                        "getName():java.lang.String", // override-narrowing seam
                        "getNameIdentifiers():java.util.List",
                        "getQualifiedPath():java.lang.String",
                        "getQualifiedName():java.lang.String",
                        "getDescription():java.lang.String",
                        "getSuite():com.intellij.plugin.applescript.lang.sdef.Suite",
                        "getDictionaryParentComponent():com.intellij.plugin.applescript.lang.sdef.DictionaryComponent",
                        "getType():java.lang.String",
                        "getDictionary():com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary",
                    ),
                // Wave 3 (plan 05-04) — ApplicationDictionary: the largest SDEF interface. ONLY the no-arg
                // getters convert (getApplicationName/getRootTag/getDictionaryFile/getApplicationBundle/
                // getAllCommands + the no-arg get*Map getters). setRootTag(...) RETURNS ApplicationDictionary
                // and stays fun; every find*/process*/add*/arg-taking get* stays fun; the @JvmField companion
                // constants are not getters. None of those are frozen here.
                ApplicationDictionary::class.java to
                    listOf(
                        "getDictionaryFile():com.intellij.openapi.vfs.VirtualFile",
                        "getApplicationBundle():java.io.File",
                        "getDictionaryEnumerationMap():java.util.Map",
                        "getDictionaryEnumeratorMap():java.util.Map",
                        "getDictionaryRecordMap():java.util.Map",
                        "getDictionaryCommandMap():java.util.Map",
                        "getDictionaryClassMap():java.util.Map",
                        "getDictionaryPropertyMap():java.util.Map",
                        "getApplicationName():java.lang.String",
                        "getRootTag():com.intellij.psi.xml.XmlTag",
                        "getAllCommands():java.util.Collection",
                    ),
            )

        /**
         * Allowlist of parameter types referenced by [FROZEN_GETTERS]. Resolving via a static Map
         * (instead of reflective class loading from an arbitrary FQN string) eliminates the CWE-470
         * unsafe-reflection surface even though every FQN here is a hardcoded literal in the same
         * file. Return types are compared by name and never resolved through this map.
         *
         * Adding a new entry here is a deliberate contract-extension act: it MUST be paired with a
         * corresponding FROZEN_GETTERS signature that uses it, in the same commit.
         */
        private val PARAM_TYPE_ALLOWLIST: Map<String, Class<*>> =
            mapOf(
                "com.intellij.plugin.applescript.lang.sdef.AccessType" to AccessType::class.java,
            )

        private fun resolveParamType(fqn: String): Class<*> =
            PARAM_TYPE_ALLOWLIST[fqn] ?: throw IllegalStateException(
                "Parameter type '$fqn' is not in PARAM_TYPE_ALLOWLIST. Add it explicitly " +
                    "(paired with the new FROZEN_GETTERS signature that uses it) to preserve the " +
                    "contract review surface and avoid CWE-470 unsafe reflection.",
            )
    }
}
