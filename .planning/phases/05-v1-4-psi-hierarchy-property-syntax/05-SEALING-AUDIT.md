# PSI-01 Sealing Audit — Phase 5 (v1.4 PSI Hierarchy + Property Syntax)

**Audited:** 2026-05-30
**Method:** live `rg` over `src/main/gen/` (Grammar-Kit + JFlex output) for each interface.
**Satisfies:** PSI-01 (seal-safe vs seal-blocked verdict per interface, each backed by a reproducible command).

## Method

For every candidate interface, the generated-Java implementer count is the deciding signal: an
interface with **zero** `src/main/gen/` implementers is implemented exclusively by hand-written
Kotlin in the same Gradle module, so a Kotlin `sealed interface` declaration compiles and is fully
enforced (Kotlin 2.x same-module rule, post KT-29748). An interface with **one or more** generated
implementers is SEAL-BLOCKED: `sealed` is Kotlin-metadata-only, the Java compiler ignores it, and a
generated implementer can reach a `when` the Kotlin compiler believed exhaustive → runtime
`NoWhenBranchMatchedException` (PITFALLS 5.1). D-04 (conservative) therefore seals ONLY the GROUP A
set and leaves ALL GROUP B interfaces OPEN this phase.

Each verdict row below cites the exact reproducible command. Run any row to reproduce its count:

```bash
rg -l "implements .*\b<Iface>\b|extends .*\b<Iface>\b" src/main/gen/ | wc -l
```

## GROUP A — SEAL-SAFE (0 generated-Java implementers each)

All in package `com.intellij.plugin.applescript.lang.sdef` (hand-written Kotlin, same Gradle module).

| # | Interface | Package | gen impls | Reproducible command | Verdict |
|---|-----------|---------|-----------|----------------------|---------|
| 1 | `ApplicationDictionary` | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bApplicationDictionary\b\|extends .*\bApplicationDictionary\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |
| 2 | `DictionaryComponent` | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bDictionaryComponent\b\|extends .*\bDictionaryComponent\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |
| 3 | `DictionarySuite` | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bDictionarySuite\b\|extends .*\bDictionarySuite\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |
| 4 | `AppleScriptCommand` | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bAppleScriptCommand\b\|extends .*\bAppleScriptCommand\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |
| 5 | `AppleScriptClass` | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bAppleScriptClass\b\|extends .*\bAppleScriptClass\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |
| 6 | `AppleScriptPropertyDefinition` (pilot) | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bAppleScriptPropertyDefinition\b\|extends .*\bAppleScriptPropertyDefinition\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |
| 7 | `Suite` | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bSuite\b\|extends .*\bSuite\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |
| 8 | `CommandParameter` | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bCommandParameter\b\|extends .*\bCommandParameter\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |
| 9 | `CommandDirectParameter` | `com.intellij.plugin.applescript.lang.sdef` | 0 | `rg -l "implements .*\bCommandDirectParameter\b\|extends .*\bCommandDirectParameter\b" src/main/gen/ \| wc -l` | **SEAL-SAFE** |

All 9 verified live on 2026-05-30 (every command above returned `0`).

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

## Sealing Permitted-Subtype Caveat (for the PSI-05 wave)

GROUP A Kotlin implementers span both the `com.intellij.plugin.applescript.lang.sdef` package AND the
`com.intellij.plugin.applescript.psi.sdef.impl` package (e.g. `ApplicationDictionaryImpl`,
`DictionaryIndexes`). Kotlin's sealed-interface rule (KT-29748, Kotlin 2.x) permits subtypes in the
**same Gradle module** even across packages — and both packages are in the single `src/main/kotlin`
source set of the same module. Therefore `sealed interface` compiles for the GROUP A set despite the
cross-package implementer spread. The PSI-05 wave that actually adds `sealed` must verify the exact
permitted-subtype set per declaration (the compiler enumerates it) and confirm no `when` site is left
non-exhaustive.

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
