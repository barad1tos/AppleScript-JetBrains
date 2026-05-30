---
plan: 05-06
phase: 05-v1-4-psi-hierarchy-property-syntax
status: complete
requirements: [PSI-07, PSI-08, PSI-09]
---

# Plan 05-06 — Phase Verification Gate + Release Checkpoints

Closes Phase 5 with the verification gate, the v1.4.0 CHANGELOG entry, and the two
operator-gated release checkpoints (deploy-smoke approved; Marketplace publish deferred).

## Tasks

### Task 1 — Verification gate (PSI-07 / PSI-08) ✅
Run against the adjusted phase definition (PSI-04 regen drift gate deferred; `testTracksWhose` known pre-existing):

| Gate | Result |
|------|--------|
| `./gradlew build` | GREEN |
| default `./gradlew test` | GREEN — `PsiGetterJvmSignatureTest` (PSI-03), `ParserUtilContractTest` (frozen), persistence golden round-trip, sdef + lexer suites; 0 failures |
| `./gradlew test -PincludeHeavyTests=true` | Local-macOS infra stall (documented flakiness, same as 02-04/02-05/03-12); fast suites 0 failures. **CI Linux is the authority** for the heavy suite. No new failures observed. |
| `./gradlew verifyPlugin` | GREEN — IC 2025.1.7.1 + 2025.2.6.2, no compat problems, no INTERNAL_API failures (PSI-08) |
| `./gradlew check` | GREEN — `verifyGeneratedSourcesMatch` correctly NOT wired (PSI-04 deferred) |

### Task 2 — CHANGELOG v1.4.0 (PSI-09 prep) ✅ (commit `2260b6b`)
`## [1.4.0] - TBD` added in user-facing language (internal-maintenance framing; v1.4.0 has no user-observable behavioral change). Grep gate clean (no Phase/PSI-0/@get:JvmName/internal terminology). Release date kept TBD pending publish decision.

### Task 3 — Deploy-smoke checkpoint (PSI-08) ✅ APPROVED
Operator ran `/deploy-to-ide` into IntelliJ IDEA 2026.1 (fresh purge-build, bytecode-verified: property-naming preserved `getPsiType/isClassProperty/isRecordProperty/getMyClass/getMyRecord/getAccessType+setAccessType/getTypeSpecifier`). Real-IDE result: **plugin works** — dictionary-backed editor live, zero regression from the getter→property + sealing refactor.
- The 39 parser errors on `fetch_tracks.applescript` are the **documented, unchanged** pre-existing AppleScript grammar backlog (memory `v2-grammar-hardening-origin`: same file, same 39-count from the Phase-2 SDEF-18 smoke). Verified Phase 5 made **zero diff** to `AppleScript.bnf` / `_AppleScriptLexer.flex` / `src/main/gen/**`, so the parser is byte-identical and cannot have changed the count. These 39 are Phase-8 / v2.0 grammar-hardening scope, not a Phase-5 defect.

### Task 4 — Marketplace publish checkpoint (PSI-09) ⊘ DEFERRED (operator decision 2026-05-30)
Publish DEFERRED; CHANGELOG release date stays TBD. Consistent with the HOTFIX-04 / SDEF-19 / COROUTINE-09 / SERVICE-14 deferred-publish cadence. Rationale: PSI-04 (regen baseline) is deferred to v2.0 and the heavy suite awaits CI Linux confirmation — publishing now would be premature. v1.4.0 is staged and ready for a future operator publish once CI is green.

## Requirement status
- PSI-07 ✅ (parser-util hot path green, ParserUtilContract frozen)
- PSI-08 ✅ (verifyPlugin matrix clean; real-IDE deploy-smoke approved)
- PSI-09 ◐ CHANGELOG staged; Marketplace publish deferred (operator)

## Self-Check: PASSED
All gate results reported honestly; deferred/known items (PSI-04, testTracksWhose, publish) explicitly flagged, not papered over.
