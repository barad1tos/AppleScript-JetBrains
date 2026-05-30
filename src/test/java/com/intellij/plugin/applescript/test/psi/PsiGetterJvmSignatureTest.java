// FROZEN GETTER CONTRACT — Adding/removing any signature here MUST happen in the same commit as the
// property conversion of the declaring interface. This is the Phase 5 (v1.4) PSI-03 sibling of
// ParserUtilContractTest: where that test freezes the 26 @JvmStatic proxies on
// ParsableScriptSuiteRegistryHelper, this one freezes the Java-visible getter names produced by
// converting GROUP A interface getters (fun getX()) to Kotlin properties (val x + @get:JvmName).
//
// Failure mode it catches: a property conversion that drops or renames a Java-visible accessor name
// (e.g. `val classProperty` synthesizing getClassProperty() instead of the required isClassProperty(),
// or a `val displayName` rename silently producing getDisplayName()). The Java consumers
// (AppleScriptGeneratedParserUtil + src/main/gen/*.java) call getX()/isX() by reflection-equivalent
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
package com.intellij.plugin.applescript.test.psi;

import com.intellij.plugin.applescript.lang.sdef.AccessType;
import com.intellij.plugin.applescript.lang.sdef.AppleScriptPropertyDefinition;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PsiGetterJvmSignatureTest {

    /**
     * Frozen Java-visible getter contract per declaring GROUP A interface. Format:
     * <pre>"methodName(parameterFqn,...):returnFqn"</pre>
     * Parameter and return types use FQN; primitives use the Java primitive name. No-arg getters have
     * an empty parameter list; the access-type setter carries its single parameter FQN.
     *
     * <p>Keyed by declaring interface {@code Class} (Pitfall 3) so each assertion is scoped to the
     * exact interface that declares the getter — colliding names across interfaces (e.g.
     * getParameters) resolve against the right declaration.
     *
     * <p>Wave 0 (plan 05-01) seeds only {@link AppleScriptPropertyDefinition} — the property-conversion
     * pilot. Its 8 accessors exercise every mechanic: plain getter (getTypeSpecifier), nullable getter
     * (getMyClass/getMyRecord), is-prefixed Boolean (isClassProperty/isRecordProperty — the {@code is}
     * prefix is PRESERVED), and a getter/setter pair (getAccessType/setAccessType → var accessType).
     */
    private static final Map<Class<?>, List<String>> FROZEN_GETTERS = Map.of(
        AppleScriptPropertyDefinition.class, List.of(
            "getPsiType():com.intellij.plugin.applescript.lang.sdef.PsiType",
            "isClassProperty():boolean",                                          // is-prefix PRESERVED
            "isRecordProperty():boolean",                                         // is-prefix PRESERVED
            "getMyClass():com.intellij.plugin.applescript.lang.sdef.AppleScriptClass",
            "getMyRecord():com.intellij.plugin.applescript.lang.sdef.DictionaryRecord",
            "getAccessType():com.intellij.plugin.applescript.lang.sdef.AccessType",
            "getTypeSpecifier():java.lang.String",
            "setAccessType(com.intellij.plugin.applescript.lang.sdef.AccessType):void"
        )
    );

    /**
     * Every signature in {@link #FROZEN_GETTERS} resolves to an actual method on its declaring
     * interface with the declared parameter types and return type. After property conversion the
     * synthesized JVM names ({@code getX}/{@code isX}/{@code setX}) MUST still resolve — if a
     * conversion dropped or renamed a Java-visible name, the lookup fails here with a clear message
     * naming the violating signature and the {@code @get:JvmName} fix.
     */
    @Test
    void everyConvertedGetterIsCallableFromJavaUnderExpectedName() {
        FROZEN_GETTERS.forEach((iface, signatures) -> {
            for (String signature : signatures) {
                String methodName = signature.substring(0, signature.indexOf('('));
                String paramsRaw = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'));
                String returnRaw = signature.substring(signature.lastIndexOf(':') + 1);

                Class<?>[] params = paramsRaw.isEmpty()
                    ? new Class<?>[0]
                    : Arrays.stream(paramsRaw.split(","))
                        .map(PsiGetterJvmSignatureTest::resolveParamType)
                        .toArray(Class<?>[]::new);

                Method method;
                try {
                    method = iface.getMethod(methodName, params);
                } catch (NoSuchMethodException e) {
                    fail("MISSING JVM GETTER: " + iface.getSimpleName() + "." + signature
                        + " — property conversion dropped or renamed the Java-visible name. "
                        + "Add @get:JvmName(\"" + methodName + "\") (or @set:JvmName for setters) "
                        + "on the converted property to lock the name. Java consumers "
                        + "(AppleScriptGeneratedParserUtil + src/main/gen) call this by JVM name "
                        + "and would hit NoSuchMethodError at runtime.");
                    return;
                }

                String actualReturn = method.getReturnType().getName();
                assertEquals(
                    returnRaw,
                    actualReturn,
                    "JVM-NAME / RETURN DRIFT on " + iface.getSimpleName() + "." + signature
                        + " (expected return " + returnRaw + ", got " + actualReturn + ")"
                );
            }
        });
    }

    /**
     * Allowlist of parameter types referenced by {@link #FROZEN_GETTERS}. Resolving via a static Map
     * (instead of reflective class loading from an arbitrary FQN string) eliminates the CWE-470
     * unsafe-reflection surface even though every FQN here is a hardcoded literal in the same file
     * (mirrors
     * {@code ParserUtilContractTest.PARAM_TYPE_ALLOWLIST}). Return types are compared by name (see
     * {@link Method#getReturnType()}) and never resolved through this map.
     *
     * <p>Adding a new entry here is a deliberate contract-extension act: it MUST be paired with a
     * corresponding FROZEN_GETTERS signature that uses it, in the same commit.
     */
    private static final Map<String, Class<?>> PARAM_TYPE_ALLOWLIST = Map.of(
        "com.intellij.plugin.applescript.lang.sdef.AccessType", AccessType.class
    );

    private static Class<?> resolveParamType(String fqn) {
        Class<?> resolved = PARAM_TYPE_ALLOWLIST.get(fqn);
        if (resolved == null) {
            throw new IllegalStateException(
                "Parameter type '" + fqn + "' is not in PARAM_TYPE_ALLOWLIST. Add it explicitly "
                    + "(paired with the new FROZEN_GETTERS signature that uses it) to preserve the "
                    + "contract review surface and avoid CWE-470 unsafe reflection.");
        }
        return resolved;
    }
}
