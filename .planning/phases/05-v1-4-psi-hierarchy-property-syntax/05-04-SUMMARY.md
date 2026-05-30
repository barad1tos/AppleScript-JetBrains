---
phase: 05-v1-4-psi-hierarchy-property-syntax
plan: 04
subsystem: sdef
tags: [kotlin, java-interop, property-conversion, sealed-interface, psi, sdef, reflective-test, cross-package]

requires:
  - phase: 05-v1-4-psi-hierarchy-property-syntax
    provides: "05-01 PsiGetterJvmSignatureTest + property-name-driven JVM-name preservation; 05-02 cross-package sealing pilot finding; 05-03 leaf conversion + deferred getSuite override seam"
provides:
  - "DictionaryComponent/DictionarySuite/ApplicationDictionary pure no-arg getters converted to Kotlin properties (PSI-03 supertype half), Java names preserved by property naming"
  - "getSuite() override-narrowing seam resolved in lockstep across all 14 SDEF impl sites (override val suite)"
  - "PsiGetterJvmSignatureTest extended to the full GROUP A surface (DictionaryComponent + ApplicationDictionary)"
  - "PSI-05: 5 same-package-safe GROUP A interfaces sealed; cross-package-aware SEALING-AUDIT correction"
  - "PSI-06 data-class guard verified empty (no PSI-extending data class)"
affects: [05-05-regen-baseline, post-v1.6-aggressive-sealing]

tech-stack:
  added: []
  patterns:
    - "Supertype pure no-arg getX() -> val x; getDescription/setDescription -> var description; getName() KEPT fun (override val 'overrides nothing' from interface position vs PsiNamedElement); setRootTag/setPluralClassName/setResult/setDictionaryDoc stay fun (non-Unit return / no getter)"
    - "Cross-package + test-proxy-aware sealing: seal only when every DIRECT subtype is same-package AND no JDK-Proxy/generated-Java implementer exists; each seal compile/test-verified before commit"

key-files:
  created:
    - .planning/phases/05-v1-4-psi-hierarchy-property-syntax/05-04-SUMMARY.md
  modified:
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryComponent.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionarySuite.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/ApplicationDictionary.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/AppleScriptCommand.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/AppleScriptClass.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/AppleScriptPropertyDefinition.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/CommandParameter.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryRecord.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryEnumeration.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/AbstractDictionaryComponent.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryClass.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/SuiteImpl.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/AppleScriptCommandImpl.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/CommandParameterImpl.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryRecordDefinition.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryEnumerationImpl.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryEnumeratorImpl.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryPropertyImpl.kt
    - src/main/kotlin/com/intellij/plugin/applescript/psi/sdef/impl/ApplicationDictionaryImpl.kt
    - src/main/kotlin/com/intellij/plugin/applescript/psi/sdef/impl/DictionaryComponentBase.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/ide/AppleScriptDocHelper.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/ide/AppleScriptDocumentationProvider.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/ide/search/AppleScriptDictionaryComponentReferencesSearch.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/resolve/AppleScriptDictionaryResolveProcessor.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/parser/SDEF_Parser.kt
    - src/main/kotlin/com/intellij/plugin/applescript/psi/impl/AppleScriptReferenceElementImpl.kt
    - src/test/java/com/intellij/plugin/applescript/test/psi/PsiGetterJvmSignatureTest.java
    - src/test/kotlin/com/intellij/plugin/applescript/test/concurrency/ApplicationDictionaryConcurrencyTest.kt
    - .planning/phases/05-v1-4-psi-hierarchy-property-syntax/05-SEALING-AUDIT.md

key-decisions:
  - "getName() KEPT as override fun (not override val name): a Kotlin val override of PsiNamedElement.getName() 'overrides nothing' from an interface-supertype position in Kotlin 2.3.21 — per the override-lockstep rule it stays fun; JVM name getName() is unchanged either way"
  - "getSuite() converted to override val suite in lockstep across ALL 14 SDEF impl sites (incl. DictionaryRecord/DictionaryEnumeration deferred from 05-03) — supertype + every narrowing override moved together"
  - "Sealing is gated by the same-PACKAGE rule (not same-module) in this project, AND by JDK-Proxy test stubs: only 5 of 9 GROUP A interfaces are sealable; ApplicationDictionary (cross-package impl), AppleScriptCommand + Suite (Proxy test stubs) are seal-blocked; CommandDirectParameter is a data class"
  - "ParserRegressionTest.testTracksWhose remains the sole heavy-suite failure — pre-existing whose-clause grammar gap (deferred-items.md), NOT a regression from this wave"

patterns-established:
  - "Pattern 1: supertype property conversion drives a transitive override-lockstep cascade (getSuite across 14 impls, getDictionaryParentComponent in DictionaryComponentBase) converted in one wave + atomic commit"
  - "Pattern 2: empirical per-seal verification (add sealed -> compileKotlin/build/heavy-test -> commit or single-file revert) — cross-package and JDK-Proxy constraints are only discoverable by compiling/running, not by static gen-only audit"

requirements-completed: [PSI-03, PSI-05, PSI-06, PSI-07]

duration: 113 min
completed: 2026-05-30
---

# Phase 5 Plan 04: GROUP A Supertype Property Conversion + Cross-Package-Aware Sealing Summary

**Converted the three GROUP A SDEF supertypes (`DictionaryComponent`, `DictionarySuite`, `ApplicationDictionary`) pure no-arg getters to Kotlin properties — resolving the `getName`/`getSuite` override-narrowing seam in lockstep across all 14 SDEF impl sites — then sealed the 5 truly same-package-safe GROUP A interfaces (PSI-05), empirically marking the other 4 seal-blocked (cross-package impl / JDK-Proxy test stubs / data class), with the reflective test extended to the full GROUP A surface and the PSI-06 data-class guard passing.**

## Performance

- **Duration:** 113 min
- **Started:** 2026-05-30T12:04:25Z
- **Completed:** 2026-05-30T13:57:33Z
- **Tasks:** 2 (Task 1 TDD: RED + GREEN)
- **Files modified:** 29 (1 created, 28 modified) — 24 src/main Kotlin, 1 test Java, 1 test Kotlin, 1 SEALING-AUDIT.md, this SUMMARY

## Accomplishments

- **GROUP A supertype property conversion (PSI-03 supertype half).** `DictionaryComponent` (12 pure getters: documentation/code/cocoaClassName/nameIdentifiers/qualifiedPath/qualifiedName/description/suite/dictionaryParentComponent/type/dictionary + the getName seam), `DictionarySuite` (no convertible members — all arg-taking), `ApplicationDictionary` (11 no-arg getters: dictionaryFile/applicationBundle/the 6 no-arg maps/applicationName/rootTag/allCommands). Java names preserved by property naming; `javap` confirms `getDocumentation`, `getSuite`, `getName`, `getApplicationName`, `getRootTag`, `setRootTag`, `getAllCommands`, etc.
- **getSuite override lockstep resolved.** Converted `override fun getSuite(): Suite` → `override val suite: Suite` across `AppleScriptCommand`, `AppleScriptClass`, `DictionaryRecord`, `DictionaryEnumeration` interfaces and all concrete impls (`DictionaryClass`, `AppleScriptCommandImpl`, `CommandParameterImpl`, `DictionaryRecordDefinition`, `DictionaryEnumerationImpl`, `DictionaryEnumeratorImpl`, `DictionaryPropertyImpl`, `SuiteImpl`, `ApplicationDictionaryImpl`, `AbstractDictionaryComponent`) — 14 sites total, mutually consistent (`override fun getSuite` count == 0, `override val suite` count == 14).
- **PsiGetterJvmSignatureTest extended** with `DictionaryComponent` (12 sigs) + `ApplicationDictionary` (11 sigs) — full GROUP A surface now frozen. GREEN; `ParserUtilContractTest` GREEN.
- **PSI-05 sealing — cross-package-aware.** Sealed the 5 same-package-safe interfaces: `DictionaryComponent`, `DictionarySuite`, `AppleScriptClass`, `AppleScriptPropertyDefinition`, `CommandParameter`. Empirically blocked: `ApplicationDictionary` (impl in `psi.sdef.impl`, compileKotlin same-package error), `AppleScriptCommand` + `Suite` (JDK `Proxy.newProxyInstance` test stubs throw `IllegalArgumentException` on sealed interfaces), `CommandDirectParameter` (a data class). GROUP B left OPEN (D-04).
- **PSI-06 guard passes** — no PSI-extending `data class` (only KDoc comments mentioning the anti-pattern).
- **PSI-07** — `./gradlew build` green; heavy `ParserRegressionTest` green for 6/7 cases (only the pre-existing `testTracksWhose` grammar gap fails); no `NoWhenBranchMatchedException`, no `NoSuchMethodError`.
- **SEALING-AUDIT.md verdict column CORRECTED** from the gen-only "9 SEAL-SAFE" to the cross-package + test-surface-aware "5 SEALED / 4 seal-blocked" result, with reproduction commands.

## Task Commits

1. **Task 1 RED: freeze DictionaryComponent + ApplicationDictionary JVM getter names** — `ecb0101` (test)
2. **Task 1 GREEN: convert GROUP A SDEF supertypes to property accessors** — `ea9d97c` (feat)
3. **Task 2: seal the same-package-safe GROUP A SDEF interfaces (PSI-05)** — `6ef3ac2` (feat)

_TDD gate sequence satisfied: test(RED) `ecb0101` → feat(GREEN) `ea9d97c`. No REFACTOR commit needed. Task 2 is a non-TDD `auto` task._

## Decisions Made

- **`getName()` stays `override fun`.** A `override val name: String` on the interface produced `'name' overrides nothing` (Kotlin 2.3.21) — the platform `PsiNamedElement.getName()` Java getter is not surfaced as a property override from an interface-supertype position (it works at a concrete class, but not on the abstract interface declaration). Per the override-lockstep rule (KEEP `fun` if the property override does not compile), `getName()` remains a `fun`. The JVM name `getName()` is identical either way, so D-03's Java-callability intent holds and the frozen-test entry `getName():java.lang.String` is valid against the `fun`.
- **`getSuite()` lockstep is whole-family.** The supertype `DictionaryComponent.suite` (nullable) and every narrowing override (`AppleScriptCommand`/`Class`/`Record`/`Enumeration` → non-null `Suite`, plus the open-leaf impls' non-null overrides of the nullable supertype) had to move together. Splitting any across waves would not compile — this is exactly why 05-03 deferred them to this supertype wave.
- **Same-PACKAGE, not same-module, is the binding sealing rule here.** The 05-02 pilot suspected this; 05-04 confirmed it on `ApplicationDictionary` (compileKotlin: *"can only extend a sealed class or interface declared in the same package"*). The rule constrains DIRECT subtypes only, which is why the two supertypes seal despite their transitive cross-package impl.
- **JDK dynamic Proxy is a sealing blocker.** `AppleScriptCommand` and `Suite` are stubbed via `Proxy.newProxyInstance` in three tests; `Proxy` rejects sealed interfaces at runtime. Rather than rewrite working test infrastructure (scope creep + risk), they are documented seal-blocked — the conservative D-04 outcome.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `@get:JvmName` superseded by property naming (phase-wide correction)**
- **Found during:** Task 1. Per the dispatch PHASE_WIDE_CORRECTION + 05-01/05-03 finding, `@get:JvmName` is uncompilable on abstract interface accessors in Kotlin 2.3.21.
- **Fix:** Java names preserved by property-name choice; `getName()` kept `fun`. Locked by the reflective test + `javap`.
- **Committed in:** `ea9d97c`.

**2. [Rule 3 - Blocking] `getName()` override kept as `fun` (override val "overrides nothing")**
- **Found during:** Task 1 GREEN. `override val name` did not compile on the interface.
- **Fix:** Reverted that one member to `override fun getName()` per the plan's explicit fallback; documented in KDoc.
- **Committed in:** `ea9d97c`.

**3. [Rule 3 - Blocking] Transitive override-lockstep cascade beyond the plan's named files**
- **Found during:** Task 1 GREEN. Converting `DictionaryComponent.getSuite/getDictionaryParentComponent/getX` forced lockstep edits in `DictionaryRecord`, `DictionaryEnumeration`, `DictionaryEnumeratorImpl`, `DictionaryPropertyImpl`, `DictionaryRecordDefinition`, `DictionaryEnumerationImpl`, `DictionaryComponentBase`, and 6 consuming call-site files (DocHelper, DocumentationProvider, ReferencesSearch, ResolveProcessor, SDEF_Parser, ReferenceElementImpl) + 1 test (ConcurrencyTest). All mechanical property-syntax migrations scoped to the converted interfaces.
- **Fix:** Converted every override to `override val`; migrated `.getX()`/`.setX(...)` call sites to property syntax/assignment; renamed colliding backing fields (`code`/`name`/`description` → `backingCode`/`backingName`/`backingDescription` in `AbstractDictionaryComponent`; `documentation` → `dictionaryDocumentation` in `ApplicationDictionaryImpl`).
- **Committed in:** `ea9d97c`.

**4. [Rule 1 - Sealing scope correction] Plan's "seal 8 interfaces" reduced to 5 (per SEALING_RESCOPE)**
- **Found during:** Task 2. The plan's gen-only premise (8 seal-safe) was superseded by the dispatch SEALING_RESCOPE. Empirically: `ApplicationDictionary` seal-blocked (cross-package impl), `AppleScriptCommand` + `Suite` seal-blocked (JDK-Proxy test stubs), `CommandDirectParameter` is a data class.
- **Fix:** Sealed only the 5 verified-safe interfaces; reverted each failed seal via single-file `git checkout`; corrected SEALING-AUDIT.md.
- **Committed in:** `6ef3ac2` (seals) + the SEALING-AUDIT correction (docs commit).

---

**Total deviations:** 4 (3 Rule-3 blocking from the prescribed conversion mechanic + cascade; 1 Rule-1 scope correction mandated by the SEALING_RESCOPE). No unmandated scope creep — every call-site edit was a mechanical syntax migration on a converted interface, and the sealing reduction is the rescope's expected conservative outcome.

## Issues Encountered

- **`ParserRegressionTest.testTracksWhose` fails (heavy suite only)** — the documented PRE-EXISTING `whose`-clause grammar gap (`deferred-items.md`; proven identical at the 05-03 baseline `4210d64`). Out of scope (LARGE-tier BNF work, Phase 8 territory). The default `./gradlew test` suite and the other 6 ParserRegressionTest cases are green.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- GROUP A property conversion is COMPLETE across the whole SDEF family (leaves in 05-03, supertypes here). The reflective `PsiGetterJvmSignatureTest` now freezes the full GROUP A getter surface.
- PSI-05 sealing is COMPLETE for the safe subset. The 4 seal-blocked interfaces are documented with empirical blockers; unblocking them (post-v1.6) requires relocating `ApplicationDictionaryImpl` into `lang.sdef` and migrating the JDK-Proxy test stubs to real impl instances.
- No blockers introduced. The pre-existing `testTracksWhose` grammar failure remains tracked in `deferred-items.md`.

## Self-Check: PASSED

- All 24 src/main Kotlin files compile; `./gradlew build` GREEN — VERIFIED
- `PsiGetterJvmSignatureTest` (full GROUP A) + `ParserUtilContractTest` GREEN; `javap` confirms preserved JVM names — VERIFIED
- 5 interfaces sealed, 4 seal-blocked, GROUP B open, v1.3 service sealed types retained — VERIFIED via rg counts
- PSI-06 guard returns empty (only KDoc comment matches) — VERIFIED
- Commits `ecb0101`, `ea9d97c`, `6ef3ac2` exist in git log — verified below
- Heavy ParserRegressionTest 6/7 green (only pre-existing testTracksWhose fails) — VERIFIED

---
*Phase: 05-v1-4-psi-hierarchy-property-syntax*
*Completed: 2026-05-30*
