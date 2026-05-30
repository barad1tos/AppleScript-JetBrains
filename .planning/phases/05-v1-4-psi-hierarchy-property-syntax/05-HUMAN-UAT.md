---
status: partial
phase: 05-v1-4-psi-hierarchy-property-syntax
source: [05-VERIFICATION.md]
started: 2026-05-30
updated: 2026-05-30
---

## Current Test

[awaiting CI Linux heavy-suite confirmation — no git remote yet; runs at push/PR time]

## Tests

### 1. CI Linux heavy-suite confirmation (PSI-08)
expected: `./gradlew test -PincludeHeavyTests=true` on CI Linux shows `ParserRegressionTest.testTracksWhose` as the SOLE failure (pre-existing `whose`-clause grammar gap, unchanged from baseline `4210d64`); no new failures introduced by the Phase-5 SDEF refactor.
why_manual: the heavy BasePlatformTestCase suite stalls on the local macOS machine (documented infra flakiness, same as 02-04/02-05/03-12); CI Linux is the established authority.
result: [pending — resolve when the branch is pushed / a PR runs CI]

### 2. Deploy-smoke in IntelliJ IDEA 2026.1 (PSI-08)
expected: plugin loads; dictionary-backed completion lists application commands; Cmd+Click resolves a command into the dictionary; no `NoSuchMethodError` / `NoWhenBranchMatchedException` in the IDE log from the getter→property + sealing refactor.
result: PASSED — operator-approved 2026-05-30 in IntelliJ IDEA 2026.1. Plugin works; the 39 parser errors on `fetch_tracks.applescript` are the unchanged pre-existing AppleScript grammar backlog (memory `v2-grammar-hardening-origin`; Phase 5 made zero diff to BNF/lexer/gen), tracked under Phase 8 / v2.0.

## Summary

total: 2
passed: 1
issues: 0
pending: 1
skipped: 0
blocked: 0

## Gaps
