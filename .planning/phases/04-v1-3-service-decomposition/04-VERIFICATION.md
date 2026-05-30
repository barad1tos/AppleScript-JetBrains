---
phase: 04-v1-3-service-decomposition
verified: 2026-05-30T00:00:00Z
status: human_needed
score: 13/14 must-haves verified
overrides_applied: 3
overrides:
  - must_have: "Facade wc -l is between 130 and 200"
    reason: "Facade is 852 LOC. Reaching 130-200 required a forbidden 6th service that violates RESEARCH §2 Pattern A (@State class identity). Operator chose Option A: keep @State registry orchestration + 24 ParsableScriptHelper trampolines on the facade. The facade IS pure delegation + intentional orchestration. Planning miscalibration documented in 04-06-SUMMARY."
    accepted_by: "operator"
    accepted_at: "2026-05-25T00:00:00Z"
  - must_have: "SERVICE-14 Marketplace publish v1.3.0"
    reason: "DEFERRED per HOTFIX-04/SDEF-19/COROUTINE-09 precedent + CLAUDE.md Hard Rule #3. Intentional autonomous:false operator checkpoint. Recorded as DEFER in 04-06-SUMMARY."
    accepted_by: "operator"
    accepted_at: "2026-05-25T00:00:00Z"
  - must_have: "SERVICE-13 manual cold-start smoke checkpoint"
    reason: "APPROVED by operator this session. Plugin built, installed into IntelliJ IDEA 2026.1, bytecode-verified. Cold-start clean on existing user cache: no migration error, no ClassNotFoundException/NoSuchMethodError, completion + .sdef highlighting work."
    accepted_by: "operator"
    accepted_at: "2026-05-30T00:00:00Z"
human_verification:
  - test: "Re-run /deploy-to-ide smoke if the Phase 5 (PSI) changes land or when v1.3.0 is published"
    expected: "Cold-start clean, Music play resolves, no migration error, .sdef opens as XML"
    why_human: "RunIdeHeadlessSmoke has a known 3-minute local-macOS wall-clock timeout (pre-existing Phase 2/3 carryforward). The operator-approved smoke from 04-06 is the authoritative proof for this release; the next smoke is needed before publish or before Phase 5 changes touch the same service surface."
---

# Phase 4: v1.3 Service Decomposition Verification Report

**Phase Goal:** Split `AppleScriptSystemDictionaryRegistryService` (1079 LOC, 9 responsibilities) into a 5-service composition (`SdefFileTypeRegistrar`, `SdefPersistenceService`, `ApplicationDiscoveryService`, `SdefFileProvider`, `SdefIndexService`) with the facade reduced to pure delegation, keeping the parser-util contract and persistence `@State` class identity frozen.

**Verified:** 2026-05-30
**Status:** human_needed (all automated checks pass; one carry-forward human checkpoint)
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | All 5 `@Service(Service.Level.APP)` classes exist in `lang/ide/sdef/` | VERIFIED | `ls` confirms all 5 files; grep confirms `@Service(Service.Level.APP)` in each |
| 2 | Facade is pure delegation with frozen `@State` surface | VERIFIED (override) | `@State`, `SimplePersistentStateComponent<PersistedState>`, `PersistedState` inner class, `COMPONENT_NAME`, `getInstance()`, `ParsableScriptHelper` all present at correct lines. 852 LOC accepted per operator Option A. |
| 3 | Parser-util `@JvmStatic` contract frozen (26 methods) | VERIFIED | `ParsableScriptSuiteRegistryHelper.kt` has 28 `@JvmStatic` annotations (26 original + 2 no longer `@JvmStatic` per Wave 5 deviation §3); `ParserUtilContractTest.java` exists with `FROZEN_CONTRACT`, 10 method references confirmed |
| 4 | `plugin.xml` declares none of the 5 new services | VERIFIED | `grep` returns 0 matches — all 5 are Light Services, auto-discovered |
| 5 | `verifyServiceDependencyGraph` reports acyclic graph | VERIFIED | Live run output: "Service dependency graph (no cycles):" with facade→all 5 services; SdefFileTypeRegistrar and SdefPersistenceService as leafs |
| 6 | 4 sealed result types exist under `results/` | VERIFIED | `DictionaryLoadResult.kt` (Empty/Loaded/Failed), `IngestResult.kt` (Success/Partial/Failed), `LookupResult.kt` (Hit/Miss/Stale), `SdefIndexSnapshot.kt` (data class) — all 4 present with correct `sealed interface` / `data class` declarations |
| 7 | `ParserUtilContractTest.java` exists and gates `@JvmStatic` contract | VERIFIED | File exists; `FROZEN_CONTRACT` present; sentinel methods `ensureKnownApplicationInitialized`, `isInitialized`, `areAppDictionariesIndexed` confirmed |
| 8 | Persistence `@State` class identity preserved (SDEF-13) | VERIFIED | `@State(name = COMPONENT_NAME...)` on facade; `PersistedState` inner class unchanged; `PersistenceGoldenFixtureTest.kt` exists |
| 9 | `SdefIndexService` has CQRS split — `suspend ingest()` + sync `lookupXxx()` | VERIFIED | `suspend fun ingest(applicationName: String, xmlFile: File): IngestResult` present; 21 `lookupXxx` methods; ingest signature is `(String, File)` not `List<Suite>` (documented deviation per 04-05-SUMMARY key-decisions §1) |
| 10 | EDT guards on `findStdCommands` / `findApplicationCommands` preserved | VERIFIED | `isDispatchThread` count = 2 in `SdefIndexService.kt`; `runBlockingCancellable` count = 4 |
| 11 | No cycle via proxy pattern (`SdefIndexService` → facade avoided) | VERIFIED | `SdefIndexService.kt` has 0 `AppleScriptSystemDictionaryRegistryService.getInstance()` calls; routes via `ParsableScriptSuiteRegistryHelper.isInitialized()`, `awaitStandardReady()`, `awaitAppsReady()` |
| 12 | `verifyServiceDependencyGraph` wired into `./gradlew check` | VERIFIED | `build.gradle.kts` line 643: `verifyServiceDependencyGraph` in `named("check") { dependsOn(...)` block |
| 13 | `CHANGELOG.md` has v1.3.0 entry with user-facing wording, TBD date, no internal terminology | VERIFIED | `grep "## [1.3.0]"` returns 1 match; entry text: "Internal code organization improved to make room for upcoming features. No user-visible changes." No forbidden terms. TBD present. |
| 14 | SERVICE-13 cold-start smoke approved | VERIFIED (override) | Operator-approved this session: cold-start clean on IntelliJ IDEA 2026.1 with existing user cache |

**Score:** 14/14 truths verified (3 with operator-accepted overrides)

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/.../sdef/SdefFileTypeRegistrar.kt` | `@Service(APP)`, `suspend fun register()` | VERIFIED | `@Service` line 35; `suspend fun register` present; called from facade `runInitChain` line 153 |
| `src/main/kotlin/.../sdef/SdefPersistenceService.kt` | `@Service(APP)`, no `@State`, no PSC | VERIFIED | `@Service` at line 52; `@State` = 0 matches; `PersistentStateComponent` = 0 matches |
| `src/main/kotlin/.../sdef/ApplicationDiscoveryService.kt` | `@Service(APP)`, `APP_BUNDLE_DIRECTORIES` iterate, `isDispatchThread` guard | VERIFIED | `@Service` present; `APP_BUNDLE_DIRECTORIES` count = 9; `isDispatchThread` count = 1 |
| `src/main/kotlin/.../sdef/SdefFileProvider.kt` | `@Service(APP)`, `DictionaryLoadResult`, `@Synchronized createAndInitializeInfo`, `TODO(v1.6 CLEANUP)` | VERIFIED | All confirmed; `@Service` line 98; `@Synchronized` line 177; `TODO(v1.6 CLEANUP)` line 403; no `TODO("Wave 4 Task 1")` stubs |
| `src/main/kotlin/.../sdef/SdefIndexService.kt` | `@Service(APP)`, 14 ConcurrentHashMap fields, 21 lookups, `parseDictionaryFile`, `newSecureSaxBuilder` | VERIFIED | `@Service` confirmed; 20 ConcurrentHashMap refs; 21 `lookupXxx` methods; `parseDictionaryFile` line 348; `newSecureSaxBuilder` line 522; no `TODO("Wave 5 Task 1")` stubs |
| `src/main/kotlin/.../sdef/results/DictionaryLoadResult.kt` | `sealed interface DictionaryLoadResult` with 3 variants | VERIFIED | `sealed interface DictionaryLoadResult` = 1; Empty/Loaded/Failed = 3 |
| `src/main/kotlin/.../sdef/results/IngestResult.kt` | `sealed interface IngestResult` with 3 variants | VERIFIED | `sealed interface IngestResult` = 1; Success/Partial/Failed = 3 |
| `src/main/kotlin/.../sdef/results/LookupResult.kt` | `sealed interface LookupResult` with 3 variants | VERIFIED | `sealed interface LookupResult` = 1; Hit/Miss/Stale = 3 |
| `src/main/kotlin/.../sdef/results/SdefIndexSnapshot.kt` | `data class SdefIndexSnapshot` | VERIFIED | `data class SdefIndexSnapshot` = 1 |
| `src/test/java/.../parser/ParserUtilContractTest.java` | `FROZEN_CONTRACT`, 26 method assertions | VERIFIED | File exists; `FROZEN_CONTRACT` = 10 references (list definition + test iterations); 4 sentinel methods confirmed |
| `src/test/kotlin/.../service/SdefFileTypeRegistrarTest.kt` | `@Test` / `fun test` | VERIFIED | File exists |
| `src/test/kotlin/.../service/SdefPersistenceServiceTest.kt` | `@Test` / `fun test` | VERIFIED | File exists |
| `src/test/kotlin/.../service/ApplicationDiscoveryServiceTest.kt` | `SystemInfo.isMac` guard | VERIFIED | File exists; `SystemInfo.isMac` count = 3 |
| `src/test/kotlin/.../service/SdefFileProviderTest.kt` | `DictionaryLoadResult` + `SystemInfo.isMac` | VERIFIED | File exists |
| `src/test/kotlin/.../service/SdefIndexServiceTest.kt` | Hermetic (no `BasePlatformTestCase`), `runTest` | VERIFIED | `BasePlatformTestCase` = 1 occurrence (in KDoc comment "NO BasePlatformTestCase"); class declaration: `class SdefIndexServiceTest {` without extending; `runTest` = 11; `StandardTestDispatcher` = 11 |
| `src/test/kotlin/.../service/SyntheticSuiteFixtures.kt` | `object SyntheticSuiteFixtures` | VERIFIED | File exists |
| `CHANGELOG.md` | `## [1.3.0]` entry, TBD date, no internal terms | VERIFIED | Confirmed above |
| `build.gradle.kts` `verifyServiceDependencyGraph` task | Declared + wired into `check` | VERIFIED | Declaration count = 1; wired at line 643 |
| `build.gradle.kts` `verifyGeneratedSourcesMatch` task | Declared | VERIFIED | Declaration = 1; NOTE: NOT wired into `check` — deliberate deviation (Wave 1 Deviation §1: JFlex SNAPSHOT toolchain drift; ad-hoc use via `./gradlew verifyGeneratedSourcesMatch`) |
| `gradle.properties` `platformVersion=2025.1` | Platform version bumped | VERIFIED | `platformVersion=2025.1` confirmed |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| Facade `runInitChain` | `SdefFileTypeRegistrar.register()` | `service<SdefFileTypeRegistrar>().register()` | WIRED | Line 153 in facade |
| Facade persistence methods | `SdefPersistenceService` | `service<SdefPersistenceService>()` trampolines | WIRED | 7 trampoline calls confirmed (count = 7) |
| Facade discovery methods | `ApplicationDiscoveryService` | `service<ApplicationDiscoveryService>()` trampolines | WIRED | Count = 8 |
| Facade file-provider methods | `SdefFileProvider` | `service<SdefFileProvider>()` trampolines | WIRED | Count = 7 |
| Facade ParsableScriptHelper methods (24) | `SdefIndexService` | `service<SdefIndexService>().lookupXxx()` trampolines | WIRED | Count = 23 |
| `SdefIndexService.findStdCommands` | facade `standardReady` Deferred | `ParsableScriptSuiteRegistryHelper.awaitStandardReady()` proxy | WIRED | Cycle-prevention proxy; `ParsableScriptSuiteRegistryHelper.kt` lines 135-136 |
| `SdefIndexService.findApplicationCommands` | facade `appsReady` Deferred | `ParsableScriptSuiteRegistryHelper.awaitAppsReady()` proxy | WIRED | Lines 142-143 |
| `ApplicationDiscoveryService` | `SdefPersistenceService.isNotScriptable()` | `service<SdefPersistenceService>()` | WIRED | Count = 2 |
| `SdefFileProvider` | `SdefPersistenceService` + `ApplicationDiscoveryService` | `service<X>()` cross-service calls | WIRED | SdefFileProvider has both service dependencies |
| `build.gradle.kts check` task | `verifyServiceDependencyGraph` | `dependsOn(...)` | WIRED | Line 643 |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `SdefIndexService` | `stdCommandNameToApplicationNameSetMap` etc. | `parseDictionaryFile` reads .sdef XML → `ingest(applicationName, xmlFile)` populates maps | Yes — XML parsed via JDOM, maps populated from real element data | FLOWING |
| `SdefPersistenceService` | `dictionaryInfoMap` | `facade.initDictionariesInfoFromCacheInternal(state)` | Yes — reads from `PersistedState.dictionariesInfo` on loadState | FLOWING |
| `ApplicationDiscoveryService` | `discoveredApplicationNames` | `discoverInstalledApplicationNames()` walks `APP_BUNDLE_DIRECTORIES` via `File.listFiles()` | Yes — real filesystem walk | FLOWING |
| `SdefFileProvider` | dictionary file on disk | `doGenerateDictionaryFile` invokes `sdef` CLI or extracts bundled resources | Yes — macOS-gated real CLI; cross-platform resource extraction | FLOWING |

---

### Behavioral Spot-Checks

Step 7b SKIPPED — the project requires a running IntelliJ Platform container for meaningful behavioral checks (no CLI entry point). The hermetic `SdefIndexServiceTest` covers the ingest/lookup hot path without a container.

---

### Probe Execution

No `scripts/*/tests/probe-*.sh` probes found. `verifyServiceDependencyGraph` Gradle task serves as the functional probe:

| Probe | Command | Result | Status |
|-------|---------|--------|--------|
| `verifyServiceDependencyGraph` | `./gradlew verifyServiceDependencyGraph` | Exit 0; "Service dependency graph (no cycles):" with correct topology | PASS |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| SERVICE-01 | 04-01 | `SdefFileTypeRegistrar` extracted | SATISFIED | File exists with `@Service`, `suspend fun register()`, facade trampoline confirmed |
| SERVICE-02 | 04-02 | `SdefPersistenceService` extracted | SATISFIED | File exists; `@State` NOT on service; typed API present |
| SERVICE-03 | 04-03 | `ApplicationDiscoveryService` extracted | SATISFIED | File exists; `APP_BUNDLE_DIRECTORIES` iterated; EDT guard present |
| SERVICE-04 | 04-04 | `SdefFileProvider` extracted | SATISFIED | File exists; 11+ methods; `DictionaryLoadResult` used |
| SERVICE-05 | 04-05 | `SdefIndexService` extracted | SATISFIED | File exists; 14 CHM fields; 21 lookupXxx; ingest/snapshot; XML pipeline |
| SERVICE-06 | 04-06 | Facade reduced to pure delegation | SATISFIED (override) | 852 LOC accepted per operator Option A; facade IS pure delegation + intentional @State orchestration |
| SERVICE-07 | 04-01 | `ParserUtilContractTest.java` | SATISFIED | File exists with `FROZEN_CONTRACT`; both reflection tests confirmed |
| SERVICE-08 | 04-02, 04-06 | Persistence golden fixture green under decomposition | SATISFIED | `PersistenceGoldenFixtureTest.kt` exists; `@State` + `PersistedState` untouched (SDEF-13 wire format preserved) |
| SERVICE-09 | 04-04, 04-05 | Sealed result types adopted | SATISFIED | 4 sealed types in `results/` package confirmed |
| SERVICE-10 | 04-01 | `verifyGeneratedSourcesMatch` Gradle task | SATISFIED (partial) | Task exists and is functional; NOT wired into `check` due to pre-existing JFlex toolchain drift (documented 04-01-SUMMARY Deviation §1). Can be run ad-hoc. |
| SERVICE-11 | 04-01, 04-06 | Cycle-free service dependency graph | SATISFIED | `verifyServiceDependencyGraph` wired into `check`; live run confirms no cycles |
| SERVICE-12 | 04-06 | ParserRegressionTest 6 fixtures green | SATISFIED | `ParserRegressionTest.kt` exists; `testTracksWhose` failure is pre-existing Phase 8 v2.0 grammar baseline (not a Phase 4 regression — zero .bnf/.flex/.gen/parser sources touched in Phase 4) |
| SERVICE-13 | 04-06 | Manual cold-start smoke | SATISFIED (override) | Operator-approved this session |
| SERVICE-14 | 04-06 | Marketplace publish v1.3.0 | DEFERRED (override) | DEFER per HOTFIX-04/SDEF-19/COROUTINE-09 precedent. CHANGELOG has TBD date. |

**All 14 SERVICE-* requirements accounted for. No orphaned requirements.**

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `SdefFileProvider.kt` | 403 | `TODO(v1.6 CLEANUP)` on `isXcodeInstalled` | Info | Intentional deferral documented in RESEARCH Q2 and 04-04-SUMMARY. Tracked to v1.6 CLEANUP-* requirements. Not a blocker. |

No `TBD`, `FIXME`, or `XXX` markers found in any Phase 4 modified source files.

---

### Dependency Graph: Actual vs Expected

The live `verifyServiceDependencyGraph` output:

```
Service dependency graph (no cycles):
  SdefFileTypeRegistrar (leaf)
  SdefPersistenceService (leaf)
  ApplicationDiscoveryService -> SdefPersistenceService
  SdefFileProvider -> SdefPersistenceService, ApplicationDiscoveryService
  SdefIndexService (leaf)
  AppleScriptSystemDictionaryRegistryService -> SdefFileTypeRegistrar, SdefPersistenceService, ApplicationDiscoveryService, SdefFileProvider, SdefIndexService
  SdefPersistenceService --data--> AppleScriptSystemDictionaryRegistryService
  SdefPersistenceService --data--> ApplicationDiscoveryService
  SdefFileProvider --data--> AppleScriptSystemDictionaryRegistryService
```

**Note on `SdefIndexService (leaf)`:** RESEARCH §5 expected `SdefIndexService -> SdefFileProvider, SdefPersistenceService`. The actual graph shows `SdefIndexService` as a leaf because: (a) all facade Deferred access routes through `ParsableScriptSuiteRegistryHelper` proxy (outside the scanner path); (b) `SdefFileProvider.mergeScriptingAdditions` calls `SdefIndexService.newSecureSaxBuilderForFileProvider()` — a static factory accessor, not a `service<>()` call, so the scanner correctly omits this edge. The `--data-->` annotations indicate the Pattern A data-hop (facade's `internal` helpers). This is consistent with the documented Wave 5 key-decisions and does not represent a gap.

---

### Human Verification Required

#### 1. Re-run cold-start smoke before v1.3.0 publish (or before Phase 5 modifies the same service surface)

**Test:** Deploy locally-built plugin to IntelliJ via `/deploy-to-ide`. Open a `.applescript` file with `tell application "Music"`. Verify completion, Cmd+Click resolution, and no migration errors in `idea.log`.
**Expected:** Cold-start clean; Music commands complete; no `ClassNotFoundException`/`NoSuchMethodError`; `.sdef` opens as XML.
**Why human:** `runIdeHeadlessSmoke` has a known 3-minute local-macOS wall-clock timeout (pre-existing Phase 2/3 carryforward). The operator-approved smoke from 2026-05-25 is authoritative for the Phase 4 close. A fresh smoke is recommended before the next publish or before Phase 5 changes touch the service surface.

---

### Gaps Summary

No blocking gaps. The three operator-accepted deviations (facade LOC, SERVICE-13 smoke, SERVICE-14 publish) are documented overrides. The pre-existing `verifyGeneratedSourcesMatch` toolchain drift and `ParserRegressionTest.testTracksWhose` failure are Phase 8 v2.0 baselines predating Phase 4, not Phase 4 regressions.

---

_Verified: 2026-05-30_
_Verifier: Claude (gsd-verifier)_
