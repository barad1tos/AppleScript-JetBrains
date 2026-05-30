# Phase 05 — Deferred Items

Out-of-scope discoveries logged during execution. Per the executor scope boundary
(only auto-fix issues DIRECTLY caused by the current task), these are NOT fixed here.

## Pre-existing parser-grammar failure: `ParserRegressionTest.testTracksWhose`

- **Discovered during:** plan 05-03, Task 2 (heavy `-PincludeHeavyTests=true` verification)
- **Symptom:** `tracks_whose.scpt` (`set z to (first item of folder 1 whose name is "x")`)
  produces 2 `PsiErrorElement` nodes:
  - line 2 offset 61: got `'1'` — expected `')'`/`:`/`<compare expression>`/`<filter reference>`/...
  - line 3 offset 86: got `'tell'` — expected `of`
- **Root cause:** AppleScript `whose`-clause / filter-reference grammar gap in
  `AppleScript.bnf` — unrelated to the SDEF property conversion this plan performs.
- **Proof it is PRE-EXISTING (not a regression from 05-03):** the test fails
  IDENTICALLY when the 05-03 Task-2 source files are reverted to the Task-1 GREEN
  baseline commit `4210d64` (interfaces still `fun getX()`). The conversion does not
  change parser behavior for the `whose` clause.
- **Scope verdict:** OUT OF SCOPE for plan 05-03 (a `whose`-clause grammar fix is a
  LARGE-tier BNF change per project CLAUDE.md, cascading through parser + PSI + tests).
  The PSI-07 hot-path intent for this plan is satisfied by the 5 passing
  ParserRegressionTest cases that exercise command-parameter parsing
  (`getParameters()`/`getDirectParameter()`/`getParameterByName()`) with no
  NoSuchMethodError, plus a green compile of `AppleScriptGeneratedParserUtil.java`
  against the converted interfaces and a green `PsiGetterJvmSignatureTest`.
- **Suggested owner:** a future grammar-hardening phase (the v2.0 grammar work noted
  in project memory), which owns `whose`/filter-reference parsing.

## PSI-04 regen-baseline reconciliation — DEFERRED to v2.0 (operator decision 2026-05-30)

- **Discovered during:** plan 05-05 (regen-baseline reconciliation), Task 1 escalation gate.
- **Symptom:** regenerating `src/main/gen` with the IPGP-bundled toolchain (JFlex 1.9.x +
  Grammar-Kit 2023.3) is cosmetic for well-formed input (114/250 byte-identical, 136
  cosmetic), but changes parser **error-recovery**: the heavy `ParserRegressionTest` goes
  from 1 failure (the known `testTracksWhose`) to 2 — a NEW `testMusicScript` failure.
  `music_script.scpt` contains the same `(... whose id is tidNum)` filter construct; the
  old gen masked it (0 error nodes), the new toolchain surfaces 6 `PsiErrorElement`s.
- **Root cause:** the SAME `whose`/`library playlist` filter-reference grammar gap above —
  surfaced (not introduced) by the newer toolchain's stricter error-recovery. A genuine
  semantic diff, so the regen was NOT committed (escalation gate held; full analysis in
  `05-REGEN-BASELINE.md`).
- **Decision (operator, 2026-05-30):** DEFER PSI-04 to v2.0. `verifyGeneratedSourcesMatch`
  remains installed-but-not-wired (its pre-phase state). D-04 keeps GROUP B grammar
  interfaces OPEN this phase and no in-phase BNF `{methods=...}` change depends on the
  regen, so deferral is low-cost and does not block the rest of Phase 5.
- **Carry-forward for v2.0:** `05-REGEN-BASELINE.md` documents two generation-procedure
  defects (found, validated, reverted) with a re-apply checklist: (A) `generateParser`
  needs `AppleScriptPsiImplUtil` on its classpath or it silently drops every `{methods=...}`
  accessor; (B) the shared `tmp-gen` dir causes `generateParser`'s `purgeOldFiles` to delete
  the lexer. The clean regen must close the `whose`-filter grammar gap (or re-baseline the
  `testMusicScript` fixture with visual sign-off) first.
- **Phase-5 impact:** PSI-04 NOT delivered in v1.4. ROADMAP success criterion 5's
  `verifyGeneratedSourcesMatch` clause is intentionally unmet (deferred); the rest of
  criterion 5 (ParserUtilContract, parser-regression hot path, persistence round-trip)
  is unaffected.
