---
phase: 05-v1-4-psi-hierarchy-property-syntax
plan: 02
subsystem: testing
tags: [kotlin, sealed-interface, psi, grammar-kit, kotlin-java-interop, sealing-pilot]

requires:
  - phase: 05-v1-4-psi-hierarchy-property-syntax
    provides: "PSI-01 sealing audit (05-SEALING-AUDIT.md) — seal-safe/seal-blocked verdict per interface"
provides:
  - "PSI-02 empirical finding: sealing AppleScriptHandler breaks compileKotlin (not the predicted gen-Java boundary)"
  - "Audit-scope correction: gen-only rg under-counts implementers — hand-written Kotlin impls in other packages also block sealing"
  - "Reinforced D-04: GROUP B stays OPEN — sealing GROUP B is a hard build break, not just runtime risk"
affects: [psi-05-sealing-wave, post-v1.6-aggressive-sealing, group-b-interfaces]

tech-stack:
  added: []
  patterns:
    - "Sealing-viability pilot: throwaway sealed modifier + compileKotlin/compileJava + single-file revert, zero source diff at end"

key-files:
  created:
    - .planning/phases/05-v1-4-psi-hierarchy-property-syntax/05-SEALING-PILOT-FINDING.md
  modified: []

key-decisions:
  - "D-04 (keep GROUP B OPEN) reinforced — sealing AppleScriptHandler is an immediate compileKotlin failure, not merely runtime-risky"
  - "PITFALLS 5.1 prediction (no build break, runtime-only) falsified for this interface — Kotlin same-package rule fails before the Java boundary is reached"
  - "Gen-only implementer audit (rg src/main/gen/) is insufficient as a sealing gate — hand-written Kotlin impls must be counted too"

patterns-established:
  - "Sealing-viability pilot: probe is throwaway; verify zero source diff via git diff --exit-code on the single file before recording the finding"

requirements-completed: [PSI-02]

duration: ~5 min
completed: 2026-05-30
---

# Phase 5 Plan 02: Sealing-Viability Pilot (AppleScriptHandler) Summary

**Empirical finding: adding `sealed` to `AppleScriptHandler` breaks `compileKotlin` — but via a hand-written Kotlin cross-package implementer (`AppleScriptHandlerInterleavedParameters.kt`), NOT the generated-Java implementer PITFALLS 5.1 predicted; D-04 keep-GROUP-B-OPEN reinforced.**

## Performance

- **Duration:** ~5 min
- **Completed:** 2026-05-30
- **Tasks:** 1
- **Files modified:** 1 created (`05-SEALING-PILOT-FINDING.md`); 0 source files net (probe reverted to zero diff)

## Accomplishments

- Ran the PSI-02 / D-01b sealing-viability probe on the minimal GROUP B case (`AppleScriptHandler`).
- Captured a hard `compileKotlin` failure: `AppleScriptHandlerInterleavedParameters.kt:21:5 — A class can only extend a sealed class or interface declared in the same package.`
- Falsified the PITFALLS 5.1 prediction for this interface and corrected the PSI-01 audit scope (gen-only `rg` under-counts implementers — a hand-written Kotlin impl in package `...psi.impl` also implements `AppleScriptHandler` in `...psi`).
- Established there is no exhaustive `when (handler)` over `AppleScriptHandler` subtypes in `src/main/kotlin` (only `is`/`!is` guards), so runtime exhaustiveness risk is independently nil today.
- Reverted the probe (single-file `git checkout`); `AppleScriptHandler.kt` ends with zero diff and `./gradlew compileKotlin compileJava` is green.

## Task Commits

1. **Task 1: Sealing-viability probe + finding** — `b51e72c` (docs) — `05-SEALING-PILOT-FINDING.md`

_The probe edit to `AppleScriptHandler.kt` was throwaway and reverted — it is NOT a commit (zero source diff at plan end)._

## Files Created/Modified

- `.planning/phases/05-v1-4-psi-hierarchy-property-syntax/05-SEALING-PILOT-FINDING.md` — the PSI-02 empirical build-vs-runtime verdict.

## Decisions Made

- **PITFALLS 5.1 prediction falsified for AppleScriptHandler.** The probe broke `compileKotlin` immediately. The break is a Kotlin same-package sealed-supertype violation on `AppleScriptHandlerInterleavedParameters.kt` (package `...psi.impl`), which pre-empts the generated-Java boundary the prediction targeted. The "Java compiler ignores `sealed`" mechanism was never reached.
- **Audit-scope correction.** `AppleScriptHandler` has two implementer surfaces: the 1 generated-Java interface counted by PSI-01, AND a hand-written Kotlin class not counted by the gen-only `rg`. The hand-written one is what blocks sealing. Future sealing gates must count hand-written Kotlin implementers too.
- **D-04 reinforced.** Sealing GROUP B is a hard build break under the current package layout, not merely "buys nothing / runtime-risky." `AppleScriptHandler` left UNSEALED.

## Deviations from Plan

None - plan executed exactly as written.

The plan anticipated the probe *might* break the build OR merely surface runtime risk, and instructed recording whichever occurred. The build broke; the cause (hand-written Kotlin implementer rather than the generated-Java implementer) is a richer-than-expected empirical result, fully within the plan's "record the outcome and the exact error" mandate — not a deviation.

## Issues Encountered

None. The `compileKotlin` failure is the intended observable signal of the probe, not an execution problem.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- PSI-02 answered empirically; the PSI-05 sealing wave now has a documented warning that gen-only audits under-count implementers and that GROUP B sealing is a hard build break under current package layout.
- GROUP A interfaces (the 9 seal-safe set, all hand-written Kotlin in `...lang.sdef`) remain the only sealing targets this phase — unaffected by this finding.
- No blockers. `./gradlew compileKotlin compileJava` green; source tree clean.

## Self-Check: PASSED

- `05-SEALING-PILOT-FINDING.md` exists on disk: FOUND
- Task commit `b51e72c` exists: FOUND
- `git diff --exit-code` on `AppleScriptHandler.kt`: clean (exit 0) — interface UNSEALED
- `grep -c "sealed interface AppleScriptHandler"`: 0
- `./gradlew compileKotlin compileJava`: BUILD SUCCESSFUL

---
*Phase: 05-v1-4-psi-hierarchy-property-syntax*
*Completed: 2026-05-30*
