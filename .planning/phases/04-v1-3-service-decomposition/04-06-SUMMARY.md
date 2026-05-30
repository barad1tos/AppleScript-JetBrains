---
phase: 04-v1-3-service-decomposition
plan: 06
subsystem: infra
tags: [service-decomposition, facade-final, changelog, marketplace-checkpoint, deploy-smoke]

requires:
  - phase: 04-05
    provides: SdefIndexService extraction — facade reduced to 870 LOC with all 5 services live
provides:
  - Final facade pass — dead straggler removed, facade validated as pure delegation over 5 services + @State orchestration
  - CHANGELOG.md v1.3.0 entry (user-facing wording, TBD release date)
  - SERVICE-13 cold-start deploy smoke approved by operator on IntelliJ IDEA 2026.1
  - SERVICE-14 Marketplace publish decision recorded as DEFER (milestone-close batch)
affects: [phase-05-v1-4-psi-hierarchy, phase-07-v1-6-milestone-closure]

tech-stack:
  added: []
  patterns:
    - "Pattern A facade retention — @State registry orchestration + ParsableScriptHelper trampolines stay on the facade by design (class identity tied to user cache)"

key-files:
  created:
    - .planning/phases/04-v1-3-service-decomposition/04-06-SUMMARY.md
  modified:
    - src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/AppleScriptSystemDictionaryRegistryService.kt
    - CHANGELOG.md

key-decisions:
  - "Option A (operator): accept ~852 LOC facade, keep RESEARCH §2 Pattern A — do NOT create a 6th service to hit the 130-200 LOC target"
  - "SERVICE-13 deploy smoke: APPROVED — cold-start clean on existing user cache"
  - "SERVICE-14 Marketplace publish: DEFER to milestone close (Phase 7 / v1.6), per HOTFIX-04/SDEF-19/COROUTINE-09 precedent"

patterns-established:
  - "Residual facade-only content is documented, not eliminated: the @State-adjacent registry orchestration is an intentional facade responsibility, not decomposition debt"

requirements-completed: [SERVICE-06, SERVICE-08, SERVICE-11, SERVICE-12, SERVICE-13, SERVICE-14]

duration: ~50min (across recovery + Option-A continuation)
completed: 2026-05-25
---

# Phase 4 Wave 6: Facade Finalization Summary

**Final facade pass — dead straggler removed (870→852 LOC), facade validated as pure delegation over 5 Light Services + intentional @State orchestration; CHANGELOG v1.3.0 stub landed; deploy smoke approved; Marketplace publish deferred to milestone close.**

## Performance

- **Duration:** ~50 min (spanned a mid-wave reboot recovery + Option-A continuation)
- **Started:** 2026-05-24T23:59Z (Task 1 commit)
- **Completed:** 2026-05-25 (operator checkpoint resolution)
- **Tasks:** 4 (2 auto + 2 human checkpoints)
- **Files modified:** 2 (facade + CHANGELOG)

## Accomplishments
- Removed the dead, zero-caller `@Suppress("unused")` `initDictionariesFromCachedFiles` straggler from the facade (also carried an inverted-logic bug — moot since dead).
- Confirmed all 5 service trampolines wired into `runInitChain`; facade reads as pure delegation + the frozen `@State` surface.
- Added `## [1.3.0] - TBD` CHANGELOG entry with user-facing wording only (no internal "Phase 4 / service split" terminology), no concrete release date.
- Built + deployed plugin to IntelliJ IDEA 2026.1; bytecode-verified all 5 services + 4 sealed result types present in the composed JAR.
- Operator-approved cold-start deploy smoke (SERVICE-13) and recorded the Marketplace publish deferral (SERVICE-14).

## Task Commits

1. **Task 1: Final facade cleanup** - `8046df4` (refactor) — removed dead `initDictionariesFromCachedFiles`; 870 → 852 LOC
2. **Task 2: CHANGELOG v1.3.0 stub** - `49b88b7` (docs) — user-facing wording, TBD date
3. **Task 3: SERVICE-13 deploy smoke** - checkpoint (no code commit — operator-driven verification)
4. **Task 4: SERVICE-14 publish decision** - checkpoint (no code commit — operator decision: defer)

**Worktree merge:** `b1fc409` (chore: merge executor worktree)

## Files Created/Modified
- `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/AppleScriptSystemDictionaryRegistryService.kt` — removed dead method; 870 → 852 LOC
- `CHANGELOG.md` — +10 lines, `## [1.3.0] - TBD` user-facing entry

## Checkpoint Outcomes

### Task 3 — SERVICE-13 cold-start deploy smoke: APPROVED
Plugin v1.3 built clean (`buildPlugin --no-build-cache --rerun-tasks`, exit 0), installed into
IntelliJ IDEA 2026.1 (`untilBuild=null` → 261 in range). Bytecode verification confirmed all 6
classes (5 services + facade) and all 4 sealed result types in the composed JAR. Operator
cold-started the IDE on the existing `appleScriptCachedDictionariesInfo.xml` cache and confirmed:
- No migration popup; no `ClassNotFoundException`/`NoSuchMethodError` in `idea.log`.
- Completion works in `tell application "Music"`; `.sdef` opens as XML with highlighting.
- The 39 parser errors on `fetch_tracks.applescript` are the **Phase 8 v2.0 grammar baseline**
  (same class as the documented `ParserRegressionTest.testTracksWhose` failure carried since Wave 3),
  NOT a Phase 4 regression — Phase 4 touched zero `.bnf`/`.flex`/`/gen/`/parser sources.

This is the central SDEF-13 backward-compat proof: keeping `@State` on the facade preserved the
user-cache class identity across the decomposition.

### Task 4 — SERVICE-14 Marketplace publish: DEFER
v1.3.0 is a pure architectural refactor with zero user-visible change. Per CLAUDE.md Hard Rule #3
("Never push without explicit approval") + HOTFIX-04/SDEF-19/COROUTINE-09 precedent, the v1.x line
publishes as one package at milestone close (Phase 7 / v1.6). CHANGELOG retains the TBD date; no
git push performed.

## Residual facade-only content (Option A — documented planning miscalibration)

The facade is **852 LOC**, not the plan's 130–200 LOC must-have. This target was reachable only by
creating a 6th service (`SdefInitOrchestrator`) to migrate the live `@State` registry orchestration
off the facade — which Task 1's own CONSTRAINT forbids ("do NOT artificially split for size") and
RESEARCH §2 Pattern A forbids (moving the @State-adjacent helpers off the facade invalidates the
SDEF-13 golden-fixture `@State` class identity).

The operator chose **Option A**: accept the facade size, keep Pattern A. The ~351-line real code
floor (852 minus ~417 architectural KDoc/comment lines deliberately recorded in Waves 1–5, minus
~84 blank) is:
- 24 `ParsableScriptHelper` trampolines — irreducible Java-consumed surface (FROZEN_CONTRACT).
- Live `@State` registry orchestration: `getInitializedInfo`, `ensureDictionaryInitialized`,
  `ensureKnownApplicationDictionaryInitialized`, `initStandardSuite`,
  `initDictionariesInfoFromCacheInternal`, `writeToStateInternal`, `removeDictionaryInfoByPathInternal`,
  etc. — must stay on the facade to preserve the user-cache class identity.

The 130–200 must-have is recorded as a **planning miscalibration**: it assumed Wave 5 left ~250–350
LOC, but Pattern A's @State retention means the facade floor is structurally higher. Goal met in
spirit (pure delegation + essential orchestration), not in the literal `wc -l` number.

## Verify Chain Results
- `./gradlew check` — **GREEN** (test + verifyNoBundledCoroutines + verifyNoRunBlocking + verifyBundledCoroutinesVersions + verifyServiceDependencyGraph)
- `verifyServiceDependencyGraph` — **GREEN**, no cycles; facade → all 5 services, correct leaf/edge shape per RESEARCH §5
- `verifyPlugin` — **GREEN**, Compatible on IC-251.29188.36 + IC-252.28539.54
- Heavy suite (ParserRegressionTest, ColdStartRegressionTest, AppCommandGatingTest, PersistenceGoldenFixtureTest, 5× service tests) — **42/43 GREEN**; the 1 failure is the documented `ParserRegressionTest.testTracksWhose` Phase 8 baseline
- `verifyGeneratedSourcesMatch` — FAILED (145 DIFFERS): **pre-existing toolchain drift** documented in 04-01-SUMMARY Deviation §1 (JFlex SNAPSHOT committed gen vs IPGP 1.9.2 bundled). Out of scope — Wave 6 touched zero gen/grammar sources; not on the `check` path.
- `runIdeHeadlessSmoke` — known ~3-min local-macOS wall-clock timeout (IDE booted); pre-existing Phase 2/3 carryforward, CI Linux unaffected. Superseded by the operator's manual `/deploy-to-ide` smoke (Task 3 approved).

## Deviations from Plan

### 1. [Rule 4 — Architectural] Facade LOC target unreachable without forbidden 6th service
- **Found during:** Task 1
- **Issue:** Must-have `wc -l` 130–200 contradicts Task 1's "do not split for size" CONSTRAINT and RESEARCH §2 Pattern A.
- **Fix:** Escalated to operator → Option A (accept ~852 LOC, document residual). No 6th service created.
- **Verification:** facade builds + all verify gates green at 852 LOC.

### 2. [Process] Mid-wave reboot recovery
- **Found during:** Task 1 (first attempt)
- **Issue:** A prior Wave 6 executor hit cwd-drift (split work across two trees) and a laptop reboot interrupted it; uncommitted partial work in the main tree + a broken locked worktree.
- **Fix:** Discarded the untrustworthy drift work, cleaned the worktree, re-ran Wave 6 cleanly from the validated `b96fb50` base. No base damage (all 5 services + 4 sealed types intact at HEAD).
- **Verification:** clean tree + correct HEAD confirmed before re-run; Tasks 1–2 then committed atomically.

---

**Total deviations:** 2 (1 Rule 4 operator-decided, 1 process/recovery)
**Impact on plan:** Facade size goal renegotiated via operator decision; no scope creep. All other must-haves met.

## Issues Encountered
The 39 `fetch_tracks.applescript` parser errors prompted a baseline cross-check — confirmed identical to the documented Phase 8 v2.0 grammar baseline (`testTracksWhose` class), not a Phase 4 regression.

## User Setup Required
None — no external service configuration required.

## Next Phase Readiness
- Phase 4 decomposition complete: 5 Light Services + thin facade, stable index-service API.
- **Phase 5 (v1.4 PSI Hierarchy)** can proceed — it depends on this stable v1.3 service surface so PSI changes don't also absorb service-split churn.
- SERVICE-14 (Marketplace publish) is an open deferred checkpoint — surfaces at milestone close (Phase 7 / v1.6).

---
*Phase: 04-v1-3-service-decomposition*
*Completed: 2026-05-25*
