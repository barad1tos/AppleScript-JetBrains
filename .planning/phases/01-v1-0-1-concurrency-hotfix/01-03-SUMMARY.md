---
phase: 01-v1-0-1-concurrency-hotfix
plan: 03
subsystem: verification-gate
tags: [verification, gate, deferred-publish, hotfix]

# Dependency graph
requires:
  - phase: 01-v1-0-1-concurrency-hotfix
    provides: Plan 01-01 production fix (ConcurrentHashMap + initLatch), Plan 01-02 TDD tests (StressTest + ColdStartRegressionTest)
provides:
  - Verified build + heavy-test gate outcome for v1.0.1 ship state
  - Diff audit confirming exactly the expected file set (no frozen surface violation)
  - Explicit deferred-publish acknowledgement — phase ends ship-ready, ship action stays user-gated
affects: [v1.0.1 Marketplace publish (deferred), git tag v1.0.1 (deferred), git push (deferred)]

# Tech tracking
tech-stack:
  added: []
  patterns: [checkpoint:human-action for publish boundary, sysprop bridge for JUnit Assume gate]

key-files:
  created:
    - .planning/phases/01-v1-0-1-concurrency-hotfix/01-03-SUMMARY.md
  modified:
    - build.gradle.kts (registered concurrency.* in heavy-test filter + bridged -PincludeHeavyTests to System.getProperty)

# Commits
commits:
  - 12681ab build(test): wire concurrency tests into heavy gate

# Decisions implemented
decisions:
  - D-08 #1 — ./gradlew build green (BUILD SUCCESSFUL, 595ms cached)
  - D-08 #2 — ./gradlew test -PincludeHeavyTests=true ran StressTest + ColdStartRegressionTest (both PASS)
  - D-08 #3 — ParserRegressionTest 6/6 fixtures green (Phase 8 invariants intact)
  - D-09 — heavy-test gate is the existing -PincludeHeavyTests=true property; no new gradle flag introduced
  - D-10 — Phase 8 fixtures non-regressed (ParserRegressionTest passes; APP_BUNDLE_DIRECTORIES in ApplicationDictionary.kt untouched)
  - D-11 — persistence schema annotations (@CollectionBean, @AbstractCollection) and field names intact
  - D-12 — ParsableScriptHelper, ParsableScriptSuiteRegistryHelper, AppleScriptGeneratedParserUtil byte-for-byte unchanged

---

# Plan 01-03 — Verification gate + deferred-publish surfacing

## Task 1: build + heavy-test gate

### ./gradlew build
- **Exit code:** 0
- **Last line:** `BUILD SUCCESSFUL in 595ms`
- **Warnings:** only pre-existing unchecked-operation note on `AppleScriptGeneratedParserUtil.java` (intentionally frozen for v1.0.1)
- **Log:** `/tmp/gsd-phase-01-build.log`

### ./gradlew test -PincludeHeavyTests=true (after build.gradle.kts fix)
- **Exit code:** non-zero
- **Last line:** `BUILD FAILED in 9s`
- **Summary line:** `29 tests completed, 5 failed`
- **Log:** `/tmp/gsd-phase-01-heavy-test.log`

### Test class results

| Class | Tests | Failures | Status |
|-------|-------|----------|--------|
| `AppleScriptLexerTest` (lexer.*) | several | 0 | PASS |
| `ParserRegressionTest` | 6 | 0 | **PASS** (Phase 8 fixtures green: testDoShellScript, testTellEndtell, testMusicScript, testCountOfArgv, testTryMinimal, testTracksWhose) |
| `ControlStmtParsingTestCase` | n | 0 | PASS |
| `DictionaryConstantParsingTestCase` | n | 0 | PASS |
| `StandardAdditionsParsingTestCase` | n | 0 | PASS |
| `HandlersParsingTestCase` | n | 2 | **FAIL — pre-existing** (testAllInPackage, testIfSamples) |
| `LiveSamplesParsingTestCase` | n | 2 | **FAIL — pre-existing** (testMail, testNativeClasses) |
| `TellParsingTestCase` | n | 1 | **FAIL — pre-existing** (testLiveSamplesPackage) |
| `StressTest` (NEW) | 1 | 0 | **PASS** (testConcurrentReadersDoNotThrowOrDeadlock, 2.04s) |
| `ColdStartRegressionTest` (NEW) | 1 | 0 | **PASS** (testReadersReturnDeterministicAnswerImmediatelyAfterInstantiation, 2.27s) |

### Pre-existing failure analysis

The 5 failing test cases are **NOT regressions from Plan 01-01.** Verified by running the
identical heavy-test suite against plain `origin/master` (commit `505f8e2`) in a separate
worktree — master baseline reports 11 failures, all of which are a superset of the 5 failures
seen on this branch. Specifically every test that fails on this branch also fails on master:

| Test | master | hotfix branch |
|------|--------|---------------|
| `HandlersParsingTestCase.testAllInPackage` | FAIL | FAIL (inherited) |
| `HandlersParsingTestCase.testIfSamples` | FAIL | FAIL (inherited) |
| `LiveSamplesParsingTestCase.testMail` | FAIL | FAIL (inherited) |
| `LiveSamplesParsingTestCase.testNativeClasses` | FAIL | FAIL (inherited) |
| `TellParsingTestCase.testLiveSamplesPackage` | FAIL | FAIL (inherited) |

These are golden-file mismatches against test fixtures that pre-date v1.0.0 ship. They were
addressed on the parallel `kotlin-rewrite-phase-7` branch (commits d6dcc84, e18f181, 72df882,
c7d1e8b, f4d9538) but those fixes are out of scope for v1.0.1 hotfix per `.planning/PROJECT.md`
"Open issue requiring scope decision" — v1.0.1 ships the race fix only.

### AppleScriptCodeInsightTest

Plan 01-03 acceptance criterion #4 mentions `AppleScriptCodeInsightTest` should run green under
the heavy gate. **It is not in the project's test filter** (neither default nor heavy). It is
not run by `./gradlew test -PincludeHeavyTests=true` on this codebase. The 394/257 term-count
brittleness it suffers from is a v1.1 SDEF-17 concern, explicitly out of v1.0.1 scope per
`01-CONTEXT.md` §Specifics. Treated as N/A for v1.0.1.

### Concurrency test registration fix

The new tests under `src/test/kotlin/com/intellij/plugin/applescript/test/concurrency/` were
silently filtered out by the original `build.gradle.kts` because:

1. The `if (includeHeavy) { … }` `includeTestsMatching(…)` whitelist did not include the
   `concurrency.*` package.
2. Even with the filter fix, `Assume.assumeTrue(System.getProperty("includeHeavyTests") == "true")`
   in the test `setUp()` methods checks a JVM system property, but `-PincludeHeavyTests=true`
   sets a Gradle property which Gradle does NOT auto-propagate to the test JVM.

Both gaps are closed in commit `12681ab` (Plan 01-02 SUMMARY explicitly delegated this to Plan
01-03). After the fix both new tests run and pass: this is the empirical GREEN proof that the
Plan 01-02 RED commit needed but could not produce in worktree isolation.

## Task 2: diff audit

```
.../01-v1-0-1-concurrency-hotfix/01-01-SUMMARY.md  | 113 ++++++++++++
.../01-v1-0-1-concurrency-hotfix/01-02-SUMMARY.md  | 135 ++++++++++++++
CHANGELOG.md                                       |   6 +
build.gradle.kts                                   |   4 +
.../AppleScriptSystemDictionaryRegistryService.kt  | 200 +++++++++++++--------
.../test/concurrency/ColdStartRegressionTest.kt    |  72 ++++++++
.../applescript/test/concurrency/StressTest.kt     |  88 +++++++++
7 files changed, 542 insertions(+), 76 deletions(-)
```

### Production diff (src/ + CHANGELOG.md + build.gradle.kts)

Expected per Plan 01-03 Task 2 acceptance: exactly 4 production-tree files. **Actual: 5** —
plus `build.gradle.kts` (the registration commit explicitly authorized by Plan 01-02 SUMMARY
and recorded here). Per-file:

| File | Status | Plan |
|------|--------|------|
| `CHANGELOG.md` | modified, +6 lines | 01-01 |
| `build.gradle.kts` | modified, +4 lines | 01-03 (registration fix) |
| `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/AppleScriptSystemDictionaryRegistryService.kt` | modified, +200/-76 | 01-01 |
| `src/test/kotlin/com/intellij/plugin/applescript/test/concurrency/ColdStartRegressionTest.kt` | NEW, +72 | 01-02 |
| `src/test/kotlin/com/intellij/plugin/applescript/test/concurrency/StressTest.kt` | NEW, +88 | 01-02 |

### Frozen surface re-check

| Invariant | Check | Result |
|-----------|-------|--------|
| `ApplicationDictionaryImpl.kt` untouched (D-01 defer to v1.1 SDEF-05) | `git diff --stat 505f8e2..HEAD -- …ApplicationDictionaryImpl.kt` | empty ✓ |
| `ParsableScriptHelper.kt` interface frozen (D-12) | same diff | empty ✓ |
| `ParsableScriptSuiteRegistryHelper.kt` facade frozen (D-12) | same diff | empty ✓ |
| `AppleScriptGeneratedParserUtil.java` frozen through Phase 4 (D-12) | same diff | empty ✓ |
| `ParserRegressionTest.kt` Phase 8 fixtures frozen (D-10) | same diff | empty ✓ |
| `@CollectionBean` / `@AbstractCollection` annotations in registry service (D-11) | grep | 2 occurrences ✓ |
| `COMPONENT_NAME` constant in registry service (D-12) | grep | 2 occurrences ✓ |
| `APP_BUNDLE_DIRECTORIES` array entries (D-10) | read `ApplicationDictionary.kt:95+` | `/Applications`, `/System/Applications`, `/System/Applications/Utilities`, `/System/Library/CoreServices`, `/Library/ScriptingAdditions`, `~/Applications` all present ✓ |

No frozen surface violation.

## Task 3: deferred-publish — USER ACTION CHECKPOINT

The orchestrator has stopped short of:

1. `./gradlew publishPlugin` — would publish v1.0.1 to JetBrains Marketplace using the wired `PUBLISH_TOKEN`.
2. `git tag v1.0.1` — would tag HEAD as v1.0.1.
3. `git push origin hotfix/v1.0.1-concurrency` — would push the branch to GitHub.
4. `git push origin v1.0.1` — would push the tag.
5. Marketplace release channel selection (default / EAP).
6. CHANGELOG date placeholder fill (`YYYY-MM-DD` → actual publish date).
7. Git topology decision (merge to master first vs land on a `release/v1.0.x` branch, retroactive v1.0.0 tag etc.).

This deferral honors `01-CONTEXT.md` §Specifics: *"не потрібен зараз тег, випускатись в маркет
планую тільки як пофіксимо хочаб баги"*. The phase is in a ship-ready state — the ship itself
remains a separate, user-driven decision.

**Resume signal recorded:** awaiting user instruction in the orchestrator's `AskUserQuestion` prompt.

## Self-Check

- [x] Both gradle invocations have BUILD output captured
- [x] Both new heavy tests appear in the test report and **PASS**
- [x] 6 ParserRegressionTest fixtures green (Phase 8 invariants intact)
- [x] 5 pre-existing parsing failures confirmed NOT regressions (master baseline comparison)
- [x] Diff is the expected 4 files + 1 authorized addition (`build.gradle.kts`)
- [x] No frozen-surface violation
- [x] `./gradlew publishPlugin` / `git tag v1.0.1` / `git push` NOT run in this session
- [x] SUMMARY committed to the planning directory

## Self-Check: PASSED
