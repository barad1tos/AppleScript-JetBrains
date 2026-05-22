---
phase: 01-v1-0-1-concurrency-hotfix
plan: 01
subsystem: sdef-registry
tags: [concurrency, intellij-platform, kotlin, hotfix, countdownlatch, concurrenthashmap]

# Dependency graph
requires:
  - phase: 00-v1-0-0
    provides: AppleScriptSystemDictionaryRegistryService skeleton, ParsableScriptHelper interface, PersistedState schema, Phase 8 parser invariants
provides:
  - ConcurrentHashMap-backed 14 outer index maps + 4 inner concurrent sets in AppleScriptSystemDictionaryRegistryService
  - Single CountDownLatch(1) released from init {} finally — survives failed init
  - Asymmetric reader gates: 22 non-blocking Boolean predicate gates + 2 bounded 2s resolver awaits
  - Defensive-snapshot HashSet<String> getters preserving the frozen ParsableScriptHelper interface
  - CHANGELOG.md v1.0.1 entry (date placeholder) naming the user-visible symptom
affects: [01-02 (TDD tests), 01-03 (publish/ship gate), v1.1 SDEF-05 (ApplicationDictionaryImpl deferred work)]

# Tech tracking
tech-stack:
  added: [java.util.concurrent.ConcurrentHashMap, java.util.concurrent.CountDownLatch]
  patterns: [ConcurrentHashMap.compute atomic writer, asymmetric CountDownLatch policy (parser non-blocking / resolver bounded await), defensive-snapshot boundary copy for frozen HashSet<String> interface returns]

key-files:
  created: []
  modified:
    - src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/AppleScriptSystemDictionaryRegistryService.kt
    - CHANGELOG.md

key-decisions:
  - "D-01 honored: ApplicationDictionaryImpl.kt NOT touched — deferred to v1.1 SDEF-05"
  - "D-02 honored: ConcurrentHashMap.newKeySet() chosen over Collections.synchronizedSet / CopyOnWriteArraySet"
  - "D-03 honored: writer pattern uses ConcurrentHashMap.compute(key) { _, existing -> ... } for atomic get-or-put-and-mutate"
  - "D-04 + D-05 honored: single CountDownLatch(1) released in finally so failed init still wakes readers"
  - "D-06 honored: 22 Boolean predicates use non-blocking initLatch.count > 0L check; 2 resolver methods use initLatch.await(2, TimeUnit.SECONDS)"
  - "D-07 honored: writer helpers inside the init chain (updateApplicationNameSetFor / updateObjectNameSetForApplication) are NOT latch-gated"
  - "D-10, D-11, D-12 honored: APP_BUNDLE_DIRECTORIES list, PersistedState annotation set, ParsableScriptHelper signatures byte-for-byte preserved"
  - "CD-02 declined: did not extract initNotReady() helper — the inline gate is short and the line economy at 22 call sites is acceptable"
  - "CD-04 resolved: CHANGELOG names user-visible symptom (NPE / IDE hangs) per HOTFIX-04; date kept as YYYY-MM-DD placeholder for Plan 03"

patterns-established:
  - "Pattern: ConcurrentHashMap.compute(key) { _, existing -> (existing ?: newKeySet()).also { add } } — the canonical atomic-writer pattern for v1.0.1 (will migrate into DictionaryIndexes in v1.1 SDEF-05)"
  - "Pattern: CountDownLatch(1) gating with two-tier policy — non-blocking for parser hot path, await(2s) for resolver"
  - "Pattern: defensive HashSet(concurrentSource) snapshot at the public boundary when the interface return type is a frozen mutable concrete (preserves D-12 contract without exposing concurrent storage)"

requirements-completed: [HOTFIX-01, HOTFIX-02, HOTFIX-04]

# Metrics
duration: ~25min
completed: 2026-05-22
---

# Phase 01 Plan 01: v1.0.1 Concurrency Hotfix Summary

**Production data-race window in `AppleScriptSystemDictionaryRegistryService` closed via `ConcurrentHashMap` + `CountDownLatch(1)`; reader hot paths now either fall through non-blocking or wait at most 2 s for the index to warm up.**

## Performance

- **Duration:** ~25 min wall-clock
- **Started:** 2026-05-22T20:00:00Z (approx; worktree branched from base 505f8e2)
- **Completed:** 2026-05-22T20:25:12Z
- **Tasks:** 3 of 3
- **Files modified:** 2 (1 production source + 1 changelog)
- **Commits:** 3 atomic + this SUMMARY

## Accomplishments

- **Storage layer hardened.** 15 outer maps (`dictionaryInfoMap` + 14 index maps) now use `ConcurrentHashMap`; 4 bare set sentinels (`notScriptableApplicationList`, `scriptingAdditions`, `notFoundApplicationList`, `discoveredApplicationNames`) use `ConcurrentHashMap.newKeySet()`. Final count: 23 `ConcurrentHashMap` occurrences, 6 `newKeySet` occurrences in the registry file.
- **Writer atomicity guaranteed.** The two writer helpers in the companion (`updateApplicationNameSetFor`, `updateObjectNameSetForApplication`) now use `ConcurrentHashMap.compute(key) { _, existing -> (existing ?: ConcurrentHashMap.newKeySet<String>()).also { it.add(...) } }`. The dead `existingSet` parameter on the old `updateSetForMappedObjectName` helper was folded out.
- **Latch + reader gates landed.** `private val initLatch: CountDownLatch = CountDownLatch(1)` declared above the `init {}` block; released in a new `finally` branch so even a thrown init exception wakes all blocked readers. 22 Boolean predicates (`ensureKnownApplicationDictionaryInitialized` plus the 20 `isXxx` checks plus the `?:` chains inside `findStdCommands`) start with `if (initLatch.count > 0L) return false`; the 2 resolver methods (`findStdCommands`, `findApplicationCommands`) start with `if (!initLatch.await(2, TimeUnit.SECONDS)) return emptyList()`.
- **Interface contract preserved byte-for-byte.** `ParsableScriptHelper.kt`, `ParsableScriptSuiteRegistryHelper`, `getInstance()`, `PersistedState`, the `@State`/`@Tag`/`@AbstractCollection`/`@CollectionBean` annotation set, `COMPONENT_NAME`, and the `APP_BUNDLE_DIRECTORIES` list (including `/System/Applications`) are unchanged. Public `HashSet<String>`-returning getters (`getNotScriptableApplicationList`, `getScriptingAdditions`, `getDiscoveredApplicationNames`) take a defensive `HashSet(...)` snapshot at the boundary so callers can't mutate the shared concurrent storage.
- **CHANGELOG drafted.** New `## [1.0.1] - YYYY-MM-DD` section sits above `## [1.0.0]` and names the user-visible symptom (sporadic `NullPointerException` / brief IDE hangs at startup or after project switch) without exposing internal mechanics. Date placeholder is intentional — Plan 03 fills the real date on the publish day.

## Task Commits

Each task was committed atomically per the execute-plan workflow:

1. **Task 1: Swap 14 HashMap fields to ConcurrentHashMap + atomic writers** — `8524ef6` (fix)
2. **Task 2: Add CountDownLatch + gate ParsableScriptHelper reader bodies** — `1c40bf1` (fix)
3. **Task 3: Draft CHANGELOG v1.0.1 entry** — `c74b772` (docs)

This SUMMARY is committed separately by the execute-plan git_commit_metadata step.

## Files Created/Modified

- `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/AppleScriptSystemDictionaryRegistryService.kt` — index field types swapped, `initLatch` introduced, 22 reader bodies gated, 2 writer helpers rewritten to `compute`, 3 boundary-snapshot copies added.
- `CHANGELOG.md` — v1.0.1 entry inserted above v1.0.0.

## Verification (static)

- `./gradlew compileKotlin` exits 0 (only pre-existing deprecation warnings on `AbstractCollection`, `SAXBuilder`, `XMLOutputter` — out of scope for this hotfix).
- `grep -c "ConcurrentHashMap"` on the registry file: **23** (target ≥ 16).
- `grep -c "newKeySet"`: **6** (target ≥ 1).
- `grep -c "if (initLatch.count > 0L) return false"`: **22** (target ≥ 20).
- `grep -c "initLatch.await(2, TimeUnit.SECONDS)"` total: **3** — one inside the KDoc on `initLatch` (line 86, doc-only), two inside actual reader bodies (lines 302 and 316). Code-only count: **2** (target == 2).
- `grep -nE "= HashMap\(\)"` on the registry file: empty.
- `grep -nE ": HashSet<String> = HashSet\(\)"` on the registry file: empty.
- `grep -n "/System/Applications"` on the registry file: still present (Phase 8 invariant intact).
- `grep -n "COMPONENT_NAME\|@State\|@CollectionBean\|@AbstractCollection"` on the registry file: all four annotation/identifier hits preserved.
- `git diff --stat 505f8e2..HEAD -- src/main/kotlin/com/intellij/plugin/applescript/lang/parser/ParsableScriptHelper.kt`: empty (interface untouched).
- `git diff --stat 505f8e2..HEAD -- src/main/kotlin/com/intellij/plugin/applescript/psi/sdef/impl/ApplicationDictionaryImpl.kt`: empty (deferred to v1.1 SDEF-05 per D-01).

Runtime stress / cold-start regression verification is Plan 02's responsibility (TDD tests) and Plan 03's responsibility (publish gate). This plan stops at the static layer per the plan's `<verification>` section.

## Decision IDs Implemented

D-01, D-02, D-03, D-04, D-05, D-06 (parser branch + resolver branch), D-07, D-10, D-11, D-12.

## Open / Deferred

- **CD-04 placeholder:** CHANGELOG date kept as `YYYY-MM-DD`; Plan 03 fills the concrete date once the user confirms the Marketplace publish day.
- **ApplicationDictionaryImpl.kt 9 maps:** intentionally NOT touched per D-01; carried into v1.1 SDEF-05 (DictionaryIndexes encapsulation).
- **CD-02 helper not extracted:** kept the inline `if (initLatch.count > 0L) return false` form at 22 call sites. Readable enough; introducing `initNotReady()` only saves ~6 chars per site and adds an indirection. Re-evaluate if v1.2 structured-concurrency rewrite touches these sites.
