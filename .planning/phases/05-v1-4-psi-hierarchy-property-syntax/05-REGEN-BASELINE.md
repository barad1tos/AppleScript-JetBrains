# 05-05 Regen-Baseline Reconciliation — Diff Inspection Record (PSI-04)

**Executed:** 2026-05-30
**Verdict:** **ESCALATION — SEMANTIC REGRESSION FOUND. Regen baseline NOT committed.**
**Toolchain:** committed `src/main/gen` = JFlex 1.7.0-SNAPSHOT + older Grammar-Kit; regen = IPGP-bundled JFlex 1.9.2 + Grammar-Kit 2023.3.

---

## TL;DR

The naive toolchain-version diff is overwhelmingly cosmetic, BUT regenerating `src/main/gen`
with the bundled toolchain changes the **parser error-recovery behavior**: the heavy
`ParserRegressionTest` suite gains ONE new failure (`testMusicScript`) that does NOT exist on
the committed baseline. This trips the plan's critical escalation gate
("ANY additional failure beyond the known `testTracksWhose` is a semantic regression → STOP").
The regenerated baseline was **abandoned** (working tree restored to `HEAD`); neither
`src/main/gen` nor the `build.gradle.kts` gate-wiring was committed. PSI-04 remains OPEN.

---

## What was done

1. Read the grammar sources (`AppleScript.bnf`, `_AppleScriptLexer.flex`) as ground truth — **NOT edited**.
2. Regenerated via `./gradlew generateLexer generateParser` (IPGP-bundled toolchain), into a
   stable side-by-side snapshot for classification.
3. Classified every differing file (cosmetic vs semantic).
4. Discovered + fixed two generation-procedure defects (below) that were masking the true diff.
5. Ran the heavy parser-regression suite against the reconciled baseline.
6. Found a NEW failure (`testMusicScript`) → escalated; restored `HEAD`.

## Regenerated-file count

- Total gen files: **250**. After fixing the generation procedure: **114 byte-identical, 136 differing**.

## Generation-procedure defects found (Rule 3 — blocking, fixed during investigation, then reverted with the regen)

These are NOT semantic grammar changes — they are flaws in HOW the regen/diff tasks were wired.
Both fixes were validated to work, but are coupled to the (abandoned) regen baseline, so they
were reverted along with it. They are the correct fixes to re-apply WHEN the semantic
error-recovery problem below is resolved.

### Defect A — `{methods=...}` accessors silently dropped (classpath gap)

`generateParser` (a `JavaExec`) ran WITHOUT the compiled `AppleScriptPsiImplUtil` on its
classpath. Grammar-Kit resolves BNF `{methods=...}` directives by **reflecting** over
`psiImplUtilClass`; with the class absent it logged
`AppleScriptPsiImplUtil class not found (PSI method signatures will not be detected)` and
**silently omitted every `{methods=...}`-bound accessor** (`getTargets`,
`getAssignmentTarget`, `getTargetsToValuesMapping`, `getApplicationName`, `useStandardAdditions`,
`withImporting`, ...), replacing them with `//WARNING: getX(...) is skipped` comments. Committing
that would have deleted methods the parser hot path calls (RESEARCH Pitfall 2).

**Fix (validated):** add `dependsOn("compileKotlin","compileJava")` + the compiled main class
dirs to the `generateParser` task `classpath`. After the fix the warnings vanished and the
`{methods=...}` accessors reappeared identically to the committed baseline.

### Defect B — phantom `MISSING: _AppleScriptLexer.java` (shared tmp-dir cross-purge)

`generateLexer` and `generateParser` both wrote to the SAME `verifyGeneratedSourcesMatch/tmp-gen`
dir. `generateParser` has `purgeOldFiles=true`, which clears the whole target dir at the start of
its run — so when both ran as `verifyGeneratedSourcesMatch` dependencies, the parser purge
DELETED the freshly-generated lexer, making the gate report a phantom
`MISSING in regen: _AppleScriptLexer.java` even against a perfectly reconciled baseline.

**Fix (validated):** point each generate task at its own tmp subdir
(`tmp-gen-lexer` / `tmp-gen-parser`) and have `verifyGeneratedSourcesMatch` resolve each committed
file against whichever root produced it. After the fix the gate printed
`Generated sources match committed src/main/gen (no drift)`.

## Cosmetic diff classification (all non-semantic — verified against the corrected regen)

Every file diff (after Defect A/B fixes) falls into these cosmetic categories. None changes the
parsed grammar or a Java-visible PUBLIC contract that has a live consumer:

| Class | Example | Why cosmetic |
|-------|---------|--------------|
| Toolchain banner | `JFlex 1.7.0-SNAPSHOT` → `JFlex 1.9.2 http://jflex.de/` | comment only |
| Lexer char-map encoding | 3-level `ZZ_CMAP_Z/Y/A` (`[9,6,6]`) → 2-level `ZZ_CMAP_TOP`+`ZZ_CMAP_BLOCKS` | same char→class mapping, denser packing (explains the 3334→2879 line drop) |
| Lexer private helper renames | `zzUnpackCMap`→`zzUnpackcmap_top/_blocks`, `zzUnpackTrans`→`zzUnpacktrans`, `ZZ_CMAP`→`zzCMap` | private scanner internals |
| Parser helper renumber | `parse`→`parse_root_`, `scriptPropertyDeclaration_1`→`_2`, sub-rule `_0_0_0` nesting | internal recursive-descent helper names; same rules |
| `@NotNull` on ctor param | `Impl(ASTNode)` → `Impl(@NotNull ASTNode)` | annotation only |
| `@Override` additions | accessor/visitor methods | annotation only |
| Spurious self-accessor removal | `AppleScriptNameReference.getNameReference()` (self-typed) dropped; same for `getIdReference`, `getFilterReference`, `getIndexReferenceClassForm` | **0 callers** outside gen, **not** BNF/mixin-bound — old codegen quirk the new toolchain correctly suppresses |
| Additive child accessor | `getIdentifier()` added on `AppleScriptApplicationReference` | additive, 0 callers |
| Nullability correctness | `ScriptPropertyDeclaration.getExpression()` `@NotNull`/`findNotNullChildByClass` → `@Nullable`/`findChildByClass` | BNF rule `[COLON propertyInitializerExpression]` is OPTIONAL → new behavior is correct; sole consumer `AbstractAppleScriptComponent.findAssignedValue()` already returns `AppleScriptExpression?` (nullable-safe) |

So at the level of **correct-input grammar parsing**, the diff is provably cosmetic/correctness-improving.

## The semantic regression (the STOP condition)

The cosmetic classification holds for WELL-FORMED input. It does NOT hold for parser
**error recovery** on the `whose`-filter grammar gap.

### Empirical proof (committed baseline vs regenerated baseline)

`./gradlew test -PincludeHeavyTests=true --tests "*ParserRegressionTest"`:

| Baseline | Failures |
|----------|----------|
| **Committed** `HEAD` `src/main/gen` | **1** — `testTracksWhose` only (the known, deferred `whose`-clause gap) |
| **Regenerated** (bundled toolchain) | **2** — `testTracksWhose` **AND** `testMusicScript` (NEW) |

Method used: filesystem-held the regen, `git checkout HEAD -- src/main/gen`, re-ran the suite →
1 failure (`music_script` PASSES on committed). Restored regen → 2 failures. The delta is
unambiguously caused by the regen.

### What `testMusicScript` produces under the regen

`music_script.scpt` contains `(first track of library playlist 1 whose id is tidNum)` — the same
`whose`/`library playlist` construct as `tracks_whose.scpt`. Under the regenerated parser it emits
6 `PsiErrorElement`s clustered around that construct:

```
line 39 offset 1445: '' — end or on expected, got 'set'
line 41 offset 1564: 'error' — var_identifier expected, got 'error'
line 43 offset 1618: 'try' — of expected, got 'try'
line 50 offset 1891: 'artist' — <compare expression>, <filter reference>, <multiplicative expression>, <object reference expression>, NLS or of expected, got 'artist'
line 64 offset 2519: 'tell' — of expected, got 'tell'
line 65 offset 2528: 'tell' — of expected, got 'tell'
```

The line-50 `<filter reference>` expectation points straight at the `whose`-filter rule. The OLD
gen's error-recovery happened to recover from this gap with zero error nodes on THIS script; the
NEW gen's recovery surfaces them. **Same root-cause grammar gap, different (newer) error-recovery
behavior.** That is a genuine semantic change to the generated parser — exactly the escalation
condition the plan defines.

## Decision

- **Regen baseline NOT committed.** `git checkout HEAD -- src/main/gen` — working tree clean.
- **`verifyGeneratedSourcesMatch` NOT wired into `check`** (Task 2 not executed) — the gate would
  green only against a baseline we are not committing.
- **build.gradle.kts generation-procedure fixes NOT committed** — they are correct and validated,
  but coupled to the abandoned regen.
- **PSI-04 remains OPEN.**

## Why this is a separate, larger problem (per RESEARCH Open Question #1)

The regen is blocked not by toolchain cosmetics (those are solved) but by a change in `whose`/
`library playlist` filter-reference **error recovery** — and the underlying grammar gap is
explicitly **v2.0 grammar-hardening territory** (PROJECT.md v2.0 `whose`-filter requirement;
deferred-items.md `testTracksWhose`; v2.0 `fetch_tracks` 39-error baseline). Reconciling the regen
requires either:

1. **Close the `whose`-filter grammar gap first** (the v2.0 BNF work) so neither fixture errors
   under either toolchain, THEN regenerate — the cleanest path, but it pulls v2.0 grammar work
   forward into v1.4; or
2. **Re-baseline the regression fixtures**: accept the newer toolchain's error-recovery as the new
   truth and regenerate `music_script.scpt`'s expected behavior with explicit visual sign-off
   (analogous to the 5 deferred Phase-2 parsing-fixture regenerations) — a deliberate decision,
   not a silent commit; or
3. **Defer PSI-04 entirely to v2.0**, keeping `verifyGeneratedSourcesMatch` installed-but-not-wired
   (its current Phase-4 state), since D-04 keeps GROUP B grammar interfaces OPEN this phase and no
   in-phase BNF `{methods=...}` change depends on the regen.

This is an architectural sequencing decision (which milestone owns the `whose`-grammar +
regen-baseline coupling), not an auto-fixable executor deviation.

## Re-apply checklist (when this is unblocked)

The two validated generation-procedure fixes to re-apply to `build.gradle.kts`:
- `generateParser`: `dependsOn("compileKotlin","compileJava")` + add
  `build/classes/kotlin/main` and `build/classes/java/main` to `classpath`.
- `generateLexer`/`generateParser`: separate `tmp-gen-lexer` / `tmp-gen-parser` output roots;
  `verifyGeneratedSourcesMatch` resolves each committed file against whichever root produced it.
