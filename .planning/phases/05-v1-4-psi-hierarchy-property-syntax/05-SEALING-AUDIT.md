# PSI-01 / PSI-05 Sealing Audit — Phase 5 (v1.4 PSI Hierarchy + Property Syntax)

**Audited:** 2026-05-30 (PSI-01 gen-only) — **CORRECTED:** 2026-05-30 (PSI-05, cross-package-aware, plan 05-04)
**Satisfies:** PSI-01 (initial gen-only verdict) + PSI-05 (the actual seal, with the corrected verdict below).

> **CORRECTION NOTICE (plan 05-04).** The original PSI-01 verdict column declared all 9 GROUP A
> interfaces "SEAL-SAFE" on a **generated-Java-implementer-count-only** basis. The 05-02 sealing
> pilot (`05-SEALING-PILOT-FINDING.md`) empirically falsified the assumption behind that method: the
> binding constraint for a Kotlin `sealed interface` in THIS project is the **same-PACKAGE rule**
> (compiler error: *"A class can only extend a sealed class or interface declared in the same
> package"*), not the same-module rule, and it applies to ALL real implementers — including
> hand-written Kotlin classes in a different package AND the JDK-dynamic-proxy stubs used in tests.
> The gen-only count under-counts the true implementer set. The verdict column below is the
> **corrected, cross-package + test-surface-aware** result, each row empirically compile/test-verified
> in plan 05-04.

## Corrected Method (PSI-05)

For each candidate interface, enumerate ALL real implementers across `src/main` (count
`class X : Iface` / `: Iface(` / `implements Iface` DECLARATIONS only — NOT return-type or parameter
usages), determine each implementer's package, and additionally check `src/test` for JDK
`Proxy.newProxyInstance(arrayOf(Iface::class.java))` stubs. **Seal an interface ONLY IF every DIRECT
subtype (interface or class) is declared in the SAME package as the interface AND no implementer is
created via a mechanism that rejects sealed types (generated Java, JDK dynamic Proxy).** Each
intended seal was compile-verified (`./gradlew compileKotlin compileJava`) and the seal that broke
compile/test was reverted and marked seal-blocked.

Key empirical findings (plan 05-04):
- Kotlin 2.3.21 in this project enforces the **same-package** sealed rule. Sealing
  `ApplicationDictionary` fails at `compileKotlin` because `ApplicationDictionaryImpl` lives in
  `com.intellij.plugin.applescript.psi.sdef.impl` — a DIFFERENT package.
- The same-package rule constrains **direct** subtypes only. `DictionaryComponent` / `DictionarySuite`
  seal cleanly even though `ApplicationDictionaryImpl` is a *transitive* implementer, because their
  only direct subtypes are interfaces (`ApplicationDictionary`, `AppleScriptCommand`, …) and abstract
  classes (`AbstractDictionaryComponent`) all in `lang.sdef`.
- JDK `Proxy.newProxyInstance` rejects a sealed interface with `IllegalArgumentException` at runtime.
  Tests stub `AppleScriptCommand` and `Suite` this way (`SuiteAddCommandTest`,
  `ApplicationDictionaryOverloadTest`, `LeafDataClassTest`) → sealing those two breaks the suite even
  though they compile. `Proxy` only inspects the directly-named interface, so a proxy of the (open)
  `AppleScriptCommand` is fine even when its supertype `DictionaryComponent` is sealed.

## GROUP A — CORRECTED VERDICT (cross-package + test-surface aware)

| # | Interface | Pkg | Direct impls (pkg) | Test Proxy stub? | Verdict |
|---|-----------|-----|--------------------|------------------|---------|
| 1 | `DictionaryComponent` | `lang.sdef` | `AbstractDictionaryComponent` (`lang.sdef`) + subtype interfaces (`lang.sdef`) | no | **SEALED** ✅ compile-verified |
| 2 | `DictionarySuite` | `lang.sdef` | `ApplicationDictionary` interface (`lang.sdef`) | no | **SEALED** ✅ compile-verified |
| 3 | `AppleScriptClass` | `lang.sdef` | `DictionaryClass` (`lang.sdef`) | no | **SEALED** ✅ compile-verified |
| 4 | `AppleScriptPropertyDefinition` (pilot) | `lang.sdef` | `DictionaryPropertyImpl` (`lang.sdef`) | no | **SEALED** ✅ (pilot, 05-01) |
| 5 | `CommandParameter` | `lang.sdef` | `CommandParameterImpl` (`lang.sdef`) | no | **SEALED** ✅ compile-verified |
| 6 | `ApplicationDictionary` | `lang.sdef` | `ApplicationDictionaryImpl` (**`psi.sdef.impl`** — cross-pkg) | yes (`Suite`/`ApplicationDictionary`) | **SEAL-BLOCKED** ❌ cross-package impl (compileKotlin: same-package error) |
| 7 | `AppleScriptCommand` | `lang.sdef` | `AppleScriptCommandImpl` (`lang.sdef`) | **yes** — `Proxy(arrayOf(AppleScriptCommand))` in 2 tests | **SEAL-BLOCKED** ❌ JDK-proxy test stub (IllegalArgumentException) |
| 8 | `Suite` | `lang.sdef` | `SuiteImpl` (`lang.sdef`) | **yes** — `Proxy(arrayOf(Suite))` in 2 tests | **SEAL-BLOCKED** ❌ JDK-proxy test stub (IllegalArgumentException) |
| 9 | `CommandDirectParameter` | `lang.sdef` | — (it is a `data class`, not an interface) | n/a | **N/A** — not an interface; PSI-06 data-class guard only |

**Outcome: 5 of the 9 GROUP A interfaces are sealable; 4 are seal-blocked.** This is the expected,
acceptable conservative result (D-04) — the truly-safe subset is sealed and the rest are documented
with their empirical blocker. The original gen-only "9 SEAL-SAFE" verdict is SUPERSEDED by this row set.

### Reproduce the corrected implementer enumeration

```bash
# Direct class/object implementers per interface (DECLARATIONS only):
for I in ApplicationDictionary DictionaryComponent DictionarySuite AppleScriptCommand \
         AppleScriptClass AppleScriptPropertyDefinition Suite CommandParameter; do
  echo "## $I"
  rg -nU --multiline "(class|object)\s+\w+[^{]*?:\s*[^{]*?\b${I}\b" src/main/kotlin/
done
# Test JDK-proxy stubs that block sealing:
rg -n "newProxyInstance|arrayOf\(\w+::class\.java\)" src/test/
```

## GROUP B — SEAL-BLOCKED (≥1 generated-Java implementer)

All in package `com.intellij.plugin.applescript.psi` (grammar-PSI tree, implemented by `src/main/gen`).

| Interface | Package | gen impls | Reproducible command | Verdict |
|-----------|---------|-----------|----------------------|---------|
| `AppleScriptExpression` | `com.intellij.plugin.applescript.psi` | **38** | `rg -l "implements .*\bAppleScriptExpression\b\|extends .*\bAppleScriptExpression\b" src/main/gen/ \| wc -l` | **SEAL-BLOCKED** |
| `AppleScriptHandler` | `com.intellij.plugin.applescript.psi` | **1** | `rg -l "implements .*\bAppleScriptHandler\b\|extends .*\bAppleScriptHandler\b" src/main/gen/ \| wc -l` | **SEAL-BLOCKED** |
| `AppleScriptComponent` | `com.intellij.plugin.applescript.psi` | **11** | `rg -l "implements .*\bAppleScriptComponent\b\|extends .*\bAppleScriptComponent\b" src/main/gen/ \| wc -l` | **SEAL-BLOCKED** |

The single `AppleScriptHandler` generated implementer is
`src/main/gen/com/intellij/plugin/applescript/psi/AppleScriptHandlerInterleavedParametersDefinition.java`
(`public interface AppleScriptHandlerInterleavedParametersDefinition extends AppleScriptHandler`).
This is the minimal blast-radius GROUP B case and the designated sealing-viability pilot (PSI-02,
later wave) — it is sealed on a throwaway branch only to answer "build break vs runtime-only", then
**unsealed** regardless of outcome (D-04 keeps all GROUP B OPEN this phase).

All 3 verified live on 2026-05-30 (counts 38 / 1 / 11 respectively).

## Scope Decision (D-04 — Conservative)

**D-04 seals ONLY the GROUP A set (the 9 rows above) and leaves ALL GROUP B interfaces OPEN this
phase.** The gen-implemented PSI grammar interfaces (`AppleScriptExpression`, `AppleScriptHandler`,
`AppleScriptComponent`, and the rest of the grammar-PSI family) stay `interface` (not `sealed`).
Aggressive gen-interface sealing is deferred to post-v1.6. Service-internal sealed types adopted in
v1.3 (`DictionaryLoadResult`, `IngestResult`, `LookupResult`, `SdefIndexSnapshot`) are retained
unchanged.

Sealing of the GROUP A set lands in the PSI-05 wave, NOT this plan. This plan (05-01) only records
the verdict (PSI-01) and runs the property-conversion pilot (PSI-02/03) on
`AppleScriptPropertyDefinition` WITHOUT sealing it.

## GROUP A ↔ GROUP B Seam Caveats (override-narrowing)

Several GROUP A getters are `override` members that narrow a supertype declaration. These overrides
constrain which getters can convert to properties and in which wave — they are NOT free to convert
in isolation:

1. **`DictionaryComponent.getName()` overrides `AppleScriptComponent.getName()`.**
   `AppleScriptComponent` lives in `com.intellij.plugin.applescript.psi` (GROUP B / the PSI tree).
   Converting `DictionaryComponent.getName` to `override val name` requires the GROUP B
   `AppleScriptComponent` supertype to expose a compatible `name` shape. If `AppleScriptComponent`
   stays `fun getName()`, the override here must also stay `override fun getName()` — do NOT convert
   it independently. This is the GROUP A ↔ GROUP B seam: a GROUP A property conversion can be blocked
   by an un-converted GROUP B supertype.

2. **`AppleScriptCommand.getSuite()` and `AppleScriptClass.getSuite()` both
   `override fun getSuite(): Suite`** — narrowing `DictionaryComponent.getSuite(): Suite?` (nullable)
   to non-null `Suite`. (Verified: `AppleScriptCommand.kt:23`, `AppleScriptClass.kt:11`.) The
   override must stay an override on the converted property (`override val suite: Suite`), and
   `DictionaryComponent.suite` must convert in the SAME wave or the narrowing override won't compile.
   These getters must move together — never split `DictionaryComponent.getSuite` from its narrowing
   overrides across waves.

## Sealing Permitted-Subtype Caveat — CORRECTED (plan 05-04)

> The original note here claimed Kotlin's sealed rule permits subtypes in the **same Gradle module**
> across packages (KT-29748). **Plan 05-04 empirically falsified that for THIS project:** sealing
> `ApplicationDictionary` failed at `compileKotlin` with *"A class can only extend a sealed class or
> interface declared in the same package"* — the binding rule here is **same PACKAGE**, not same
> module. `ApplicationDictionaryImpl` lives in `psi.sdef.impl`, the interface in `lang.sdef`, so the
> cross-package implementer blocks the seal.

The correct permitted-subtype rule for this codebase:
1. Only **direct** subtypes must be in the same package as the sealed interface (transitive
   implementers reached through an intermediate same-package interface are fine — that is why
   `DictionaryComponent`/`DictionarySuite` seal despite the cross-package `ApplicationDictionaryImpl`,
   which only implements the open leaf `ApplicationDictionary`).
2. JDK `Proxy.newProxyInstance(arrayOf(Iface::class.java))` in `src/test` is a real implementer
   surface — `Proxy` throws `IllegalArgumentException` on a sealed interface. Any interface stubbed
   this way is seal-blocked unless the stub is migrated to a real impl instance.
3. No new exhaustive `when` site was added; existing `else` branches retained → no
   `NoWhenBranchMatchedException` risk on the 5 sealed types (verified by the heavy
   `ParserRegressionTest`, green except the pre-existing `testTracksWhose` grammar gap).

## Reproduction Note

Re-run the whole audit at any time:

```bash
for I in ApplicationDictionary DictionaryComponent DictionarySuite AppleScriptCommand \
         AppleScriptClass AppleScriptPropertyDefinition Suite CommandParameter CommandDirectParameter \
         AppleScriptExpression AppleScriptHandler AppleScriptComponent; do
  C=$(rg -l "implements .*\b${I}\b|extends .*\b${I}\b" src/main/gen/ | wc -l | tr -d ' ')
  echo "${I} = ${C}"
done
```

Expected (2026-05-30): GROUP A all `0`; `AppleScriptExpression = 38`, `AppleScriptHandler = 1`,
`AppleScriptComponent = 11`.
