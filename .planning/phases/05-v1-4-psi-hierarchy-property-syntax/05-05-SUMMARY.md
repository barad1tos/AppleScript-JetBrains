---
plan: 05-05
phase: 05-v1-4-psi-hierarchy-property-syntax
status: deferred
requirements: [PSI-04]
outcome: deferred-to-v2.0
decided_by: operator
decided_on: 2026-05-30
---

# Plan 05-05 — Regen-Baseline Reconciliation (PSI-04) — DEFERRED

> **This plan was NOT delivered.** It reached its escalation gate, surfaced a semantic
> regression, and was deferred to v2.0 by operator decision. PSI-04 is unmet in v1.4.

## Outcome

**DEFERRED to v2.0 (operator decision, 2026-05-30).**

The executor regenerated `src/main/gen` with the IPGP-bundled toolchain (JFlex 1.9.x +
Grammar-Kit 2023.3) and proved the diff is cosmetic for well-formed input (114/250
byte-identical, 136 cosmetic — toolchain banner, JFlex char-map re-encoding, private-helper
renames, parser sub-rule renumbering, `@NotNull`/`@Override` additions). **But** the regen
changes parser **error-recovery**: the heavy `ParserRegressionTest` goes from 1 failure
(known/deferred `testTracksWhose`) to 2 — a NEW `testMusicScript` failure on the same
`(... whose id is ...)` filter construct. This is a genuine semantic diff rooted in the
`whose`/filter-reference grammar gap (v2.0 grammar-hardening territory).

Per the plan's escalation gate, the regen was **NOT committed**. No changes to
`src/main/gen` or `build.gradle.kts`. `verifyGeneratedSourcesMatch` remains
installed-but-not-wired (its pre-phase state). Task 2 (wire the gate into `check`) was not
executed.

## Why deferral is low-cost (operator rationale)

- D-04 keeps GROUP B grammar interfaces OPEN this phase — nothing seals the grammar PSI.
- No in-phase BNF `{methods=...}` change depends on the regen baseline.
- The blocker is the `whose`-filter grammar gap, explicitly owned by the v2.0/Phase-8
  grammar-hardening milestone.

## Artifacts

- `05-REGEN-BASELINE.md` (commit `9ca3fc1`) — full 250-file diff classification, the
  semantic-regression proof, and a re-apply checklist for two generation-procedure defects
  (generateParser classpath + shared tmp-gen dir) for whoever does the clean regen in v2.0.
- `deferred-items.md` — PSI-04 deferral record.

## Requirement status

- **PSI-04:** NOT delivered (deferred to v2.0).
- ROADMAP success criterion 5's `verifyGeneratedSourcesMatch` clause: intentionally unmet.
  The other criterion-5 clauses (ParserUtilContract, parser-regression hot path, persistence
  round-trip) are satisfied independently by plans 05-01/05-03.

## Self-Check: N/A (plan deferred, not executed to completion)
