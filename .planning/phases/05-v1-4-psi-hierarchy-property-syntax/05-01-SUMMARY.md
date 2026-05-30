---
phase: 05-v1-4-psi-hierarchy-property-syntax
plan: 01
subsystem: testing
tags: [kotlin, java-interop, jvmname, sealed-interface, psi, reflective-test, grammar-kit]

requires:
  - phase: 04-v1-3-service-decomposition
    provides: ParserUtilContractTest reflective FROZEN_CONTRACT template (SERVICE-07)
provides:
  - PSI-01 sealing audit (05-SEALING-AUDIT.md) with reproducible per-interface seal verdicts
  - PSI-03 reflective JVM-getter contract test (PsiGetterJvmSignatureTest) wired into default suite
  - PSI-02 property-bridge mechanic empirically validated on the AppleScriptPropertyDefinition pilot
  - Empirical finding: @get:JvmName is NOT applicable on abstract interface property accessors (Kotlin 2.3.21)
affects: [05-02-group-a-conversion, 05-04-sealing, 05-05-regen-baseline, group-a-property-conversion-waves]

tech-stack:
  added: []
  patterns:
    - "Property name chosen so its synthesized JVM accessor matches the frozen Java name (replaces @get:JvmName on interface getters)"
    - "FROZEN_GETTERS Map<Class<?>, List<String>> keyed by declaring interface (collision-safe per-interface reflective contract)"

key-files:
  created:
    - .planning/phases/05-v1-4-psi-hierarchy-property-syntax/05-SEALING-AUDIT.md
    - src/test/java/com/intellij/plugin/applescript/test/psi/PsiGetterJvmSignatureTest.java
  modified:
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/AppleScriptPropertyDefinition.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryPropertyImpl.kt
    - src/main/kotlin/com/intellij/plugin/applescript/lang/ide/AppleScriptDocHelper.kt
    - build.gradle.kts

key-decisions:
  - "Java-name preservation on converted interface getters is achieved by property-name choice + the reflective contract test, NOT @get:JvmName — the annotation is not applicable to abstract interface property accessors in Kotlin 2.3.21"
  - "D-04 conservative scope confirmed: seal only the 9 GROUP A 0-gen interfaces; leave all 3 GROUP B grammar-PSI interfaces OPEN this phase"

patterns-established:
  - "Pattern 1: fun getX() -> val x / var x where the property name synthesizes the exact frozen JVM accessor (is-prefix preserved for Boolean getters)"
  - "Pattern 2: reflective FROZEN_GETTERS contract keyed by declaring interface Class, CWE-470-safe via PARAM_TYPE_ALLOWLIST (no Class.forName)"

requirements-completed: [PSI-01, PSI-02, PSI-03]

duration: 26 min
completed: 2026-05-30
---

# Phase 5 Plan 01: PSI-01 Sealing Audit + Property-Conversion Pilot Summary

**PSI-01 sealing-audit verdict table (9 GROUP A seal-safe / 3 GROUP B seal-blocked, each rg-reproducible) plus the AppleScriptPropertyDefinition property pilot locked by a new reflective JVM-getter contract test — which empirically proved `@get:JvmName` is unusable on abstract interface accessors, so JVM names are preserved by property naming instead.**

## Performance

- **Duration:** 26 min
- **Started:** 2026-05-30T09:33:44Z
- **Completed:** 2026-05-30T09:59:38Z
- **Tasks:** 2
- **Files modified:** 6 (2 created, 4 modified)

## Accomplishments

- **PSI-01 sealing audit** — `05-SEALING-AUDIT.md` records a seal-safe/seal-blocked verdict for all 12 audited interfaces, each backed by a reproducible `rg -l "implements .*<Iface>" src/main/gen/` command. Live counts verified 2026-05-30: GROUP A all `0` (9 interfaces), GROUP B `AppleScriptExpression`=38, `AppleScriptHandler`=1, `AppleScriptComponent`=11. D-04 conservative scope (seal GROUP A only, leave GROUP B OPEN) documented, plus the `getName`/`getSuite` override-narrowing GROUP A↔B seam caveat and the Kotlin same-module permitted-subtype note for the PSI-05 wave.
- **PSI-03 reflective contract test** — `PsiGetterJvmSignatureTest.java`, the sibling of `ParserUtilContractTest`, freezes the 8 Java-visible accessor names of `AppleScriptPropertyDefinition` via a `Map<Class<?>, List<String>>` keyed by declaring interface (Pitfall 3 collision-safe). CWE-470-safe via a hardcoded `PARAM_TYPE_ALLOWLIST` (no `Class.forName`). Wired into the default `./gradlew test` filter (`psi.*` matcher, unconditional, alongside `parser.*`).
- **PSI-02 property pilot** — `AppleScriptPropertyDefinition`'s 8 accessors converted from `fun getX()` to `val`/`var` properties; the `is`-prefix preserved on `isClassProperty`/`isRecordProperty`; the `getAccessType`/`setAccessType` pair became `var accessType`. `DictionaryPropertyImpl` overrides updated to `override val`/`override var`. The conversion is GREEN against the contract test and the full default suite.

## Task Commits

1. **Task 1: PSI-01 sealing audit document** — `e97e311` (docs)
2. **Task 2 RED: reflective JVM-getter contract test** — `a2165a3` (test)
3. **Task 2 GREEN: AppleScriptPropertyDefinition property conversion** — `259bd9e` (feat)

_TDD gate sequence satisfied: test(RED) `a2165a3` → feat(GREEN) `259bd9e`. No REFACTOR commit needed (conversion was clean)._

## Files Created/Modified

- `.planning/phases/05-v1-4-psi-hierarchy-property-syntax/05-SEALING-AUDIT.md` — PSI-01 verdict table (created)
- `src/test/java/com/intellij/plugin/applescript/test/psi/PsiGetterJvmSignatureTest.java` — reflective JVM-getter contract test (created)
- `src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/AppleScriptPropertyDefinition.kt` — 8 getters → properties (pilot)
- `src/main/kotlin/com/intellij/plugin/applescript/lang/sdef/DictionaryPropertyImpl.kt` — `override fun` → `override val/var`; backing fields renamed
- `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/AppleScriptDocHelper.kt` — Kotlin call sites switched to property syntax (`.accessType`, `.typeSpecifier`)
- `build.gradle.kts` — `psi.*` test-filter matcher added (unconditional, outside `includeHeavy`)

## Decisions Made

- **JVM-name preservation without `@get:JvmName`.** Kotlin 2.3.21 rejects `@JvmName` on an abstract interface property accessor ("'@JvmName' annotation is not applicable to this declaration"), and `-Xjvm-default=all` does NOT lift the restriction (both tried). The Java-visible name is instead preserved by choosing the property name so its synthesized accessor matches the frozen name (`psiType`→`getPsiType`, `accessType`→`getAccessType`/`setAccessType`, `isClassProperty`→`isClassProperty`). `javap` on the compiled interface confirms all 8 expected JVM signatures. The forward-looking name lock is the reflective `PsiGetterJvmSignatureTest` (asserts compiled bytecode — stronger than a source annotation).
- **D-04 conservative scope confirmed** by the live audit: 9 GROUP A interfaces seal-safe (0 gen impls), 3 GROUP B seal-blocked.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `@get:JvmName` on abstract interface accessors does not compile**
- **Found during:** Task 2 (GREEN conversion)
- **Issue:** The plan's prescribed mechanic (and D-03 blanket-`@get:JvmName` policy) — `@get:JvmName("getPsiType") val psiType` etc. on the interface — fails to compile in Kotlin 2.3.21 with "'@JvmName' annotation is not applicable to this declaration" on every annotated accessor. Adding `-Xjvm-default=all` did not resolve it (the restriction is intrinsic to abstract interface accessors, not a default-method-mode issue).
- **Fix:** Dropped `@get:JvmName`/`@set:JvmName` from the interface; preserved the exact Java-visible names by property-name choice (verified via `javap`: `getPsiType`, `isClassProperty`, `isRecordProperty`, `getMyClass`, `getMyRecord`, `getAccessType`, `setAccessType`, `getTypeSpecifier`). Documented each locked JVM name in KDoc on the property. The reflective `PsiGetterJvmSignatureTest` enforces the contract over the compiled bytecode.
- **Files modified:** `AppleScriptPropertyDefinition.kt`, `build.gradle.kts` (the `-Xjvm-default=all` experiment was reverted — net zero change to compiler args)
- **Verification:** `./gradlew build` GREEN; `PsiGetterJvmSignatureTest` GREEN (asserts all 8 frozen JVM names resolve); `ParserUtilContractTest` stays GREEN.
- **Committed in:** `259bd9e` (Task 2 GREEN commit)

**2. [Rule 3 - Blocking] Kotlin call sites broke on property conversion**
- **Found during:** Task 2 (GREEN conversion)
- **Issue:** `AppleScriptDocHelper.appendClassProperty` called `prop.getAccessType()` / `prop.getTypeSpecifier()` with method-call syntax on the now-converted interface — Kotlin requires property syntax for converted properties, so compilation failed.
- **Fix:** Switched to `prop.accessType` / `prop.typeSpecifier`. (Other `.getTypeSpecifier()` call sites in `AbstractDictionaryComponent.kt:118` and `AppleScriptCommandImpl.kt:147,176` were verified to target `CommandParameter` — NOT converted this plan — and left unchanged.)
- **Files modified:** `AppleScriptDocHelper.kt`
- **Verification:** `./gradlew build` GREEN; full default suite GREEN.
- **Committed in:** `259bd9e` (Task 2 GREEN commit)

---

**Total deviations:** 2 auto-fixed (both Rule 3 - blocking). **Impact:** Both necessary to compile the prescribed conversion. The first is a load-bearing phase-wide finding: the D-03 `@get:JvmName` policy is unachievable on interface declarations and must be replaced by property-name-driven preservation + the reflective contract test for every remaining GROUP A wave. No scope creep.

## Issues Encountered

None beyond the deviations above. The acceptance criterion literally requiring `@get:JvmName("getPsiType")`/`@get:JvmName("isClassProperty")` source strings in the interface is superseded by the deviation: those annotations are uncompilable on abstract interface accessors. The underlying intent (every converted getter callable from Java under its preserved `getX`/`isX` name) is fully met and enforced by `PsiGetterJvmSignatureTest`.

## Phase-Wide Implication for Downstream Waves

The remaining GROUP A property conversions (05-02 onward: `CommandParameter`, `AppleScriptClass`, `Suite`, `AppleScriptCommand`, `ApplicationDictionary`, `DictionaryComponent`) MUST use property-name-driven JVM-name preservation — NOT `@get:JvmName` on the interface. Update the D-03 mechanic in the planner's mind: blanket `@get:JvmName` applies only to concrete (bodied) accessors and `@JvmStatic` helpers, never to abstract interface property accessors. Each wave extends `FROZEN_GETTERS` with the converting interface's frozen names and gates on `PsiGetterJvmSignatureTest`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 05-02 (broad GROUP A conversion). The pilot mechanic is validated, the contract net (`PsiGetterJvmSignatureTest`) is wired into the default suite, and the seal verdict table (`05-SEALING-AUDIT.md`) locks which interfaces seal in the PSI-05 wave.
- No blockers. The regen-baseline wave (PSI-04, plan 05-05) remains gated separately per RESEARCH Pitfall 1 — untouched here (GROUP A needs no regen).

## Self-Check: PASSED

- `05-SEALING-AUDIT.md` exists on disk — FOUND
- `PsiGetterJvmSignatureTest.java` exists on disk — FOUND
- `AppleScriptPropertyDefinition.kt` converted (0 remaining `fun get/is` declarations) — VERIFIED
- Commits `e97e311`, `a2165a3`, `259bd9e` exist in git log — verified below
- `./gradlew build` GREEN, `PsiGetterJvmSignatureTest` + `ParserUtilContractTest` + full default suite GREEN — VERIFIED

---
*Phase: 05-v1-4-psi-hierarchy-property-syntax*
*Completed: 2026-05-30*
