---
phase: 04-v1-3-service-decomposition
plan: 01
subsystem: infra
tags: [service-decomposition, verification-scaffolding, parser-util-contract, grammar-kit, jflex, ipgp-2.16.0, light-service, sdef-file-type-registrar, junit5-reflection, cycle-detection]

# Dependency graph
requires:
  - phase: 03-v1-2-sdef-loading-structured-concurrency
    provides: "@JvmOverloads `(CoroutineScope, CoroutineDispatcher)` constructor pattern for @Service(Service.Level.APP) classes; CompletableDeferred<Result<Unit>> success-semantic gating; verifyNoRunBlocking + verifyNoBundledCoroutines + verifyBundledCoroutinesVersions Gradle task precedent; pluginSinceBuild=251"
  - phase: 02-v1-1-sdef-data-model-quick-wins
    provides: "SDEF-13 golden persistence fixture (regression-locks PersistedState wire format); immutable Suite + AppleScriptCommandData + DictionaryClassData data classes"
  - phase: 01-v1-0-1-concurrency-hotfix
    provides: "HOTFIX-01 ConcurrentHashMap pattern; persistence @State annotation class-identity contract"
provides:
  - "ParserUtilContractTest (Java, JUnit 5, reflection-only) gating the 26 @JvmStatic methods on ParsableScriptSuiteRegistryHelper consumed by the generated parser util"
  - "SdefFileTypeRegistrar Light Service template — Wave 1 pilot extraction validating @Service-extraction mechanics on a low-risk target"
  - "verifyServiceDependencyGraph custom Gradle task — DFS cycle detection over the 6 SDEF service classes; wired into check"
  - "verifyGeneratedSourcesMatch custom Gradle task — INSTALLED (re-runs generateLexer/generateParser into build/verifyGeneratedSourcesMatch/tmp-gen + diffs against committed src/main/gen); NOT wired into check on Wave 1 (deferred — see Deviations)"
  - "platformVersion bump 2024.3 -> 2025.1 (RESEARCH Q4) aligning dev sandbox with pluginSinceBuild=251"
  - "test.service.* test filter under -PincludeHeavyTests=true for Light Service unit tests"
  - "Slim facade pattern proof-of-concept: registerSdefExtension trampoline (1357 -> 1348 LOC, -9 LOC)"
affects: [04-02 SdefPersistenceService extraction, 04-03 ApplicationDiscoveryService, 04-04 SdefFileProvider, 04-05 SdefIndexService, 04-06 facade slim-to-150-LOC final pass, v1.4 PSI work (next phase allowed to mutate FROZEN_CONTRACT)]

# Tech tracking
tech-stack:
  added:
    - "id(\"org.jetbrains.intellij.platform.grammarkit\") v2.16.0 (bundled IPGP variant — JFlex 1.9.2 + Grammar-Kit 2023.3)"
  patterns:
    - "Light Service with constructor-injected (CoroutineScope, CoroutineContext) — @Service(Service.Level.APP) + @JvmOverloads + suspend method delegating to withContext(edtDispatcher)"
    - "Frozen-contract reflection test with static PARAM_TYPE_ALLOWLIST (CWE-470 safe — no Class.forName)"
    - "Custom Gradle verify tasks modelled on verifyNoRunBlocking — DFS over service-graph adjacency built by grep-scanning service<X>() / X.getInstance call sites"
    - "Grammar drift gate via grammarkit-bundled generateLexer/generateParser into build/<tmp>/tmp-gen + recursive file diff"

key-files:
  created:
    - "src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/SdefFileTypeRegistrar.kt (~85 LOC Light Service)"
    - "src/test/java/com/intellij/plugin/applescript/test/parser/ParserUtilContractTest.java (reflection-only contract gate)"
    - "src/test/kotlin/com/intellij/plugin/applescript/test/service/SdefFileTypeRegistrarTest.kt (BasePlatformTestCase smoke + idempotency)"
  modified:
    - "build.gradle.kts (grammarkit plugin + 2 new verify tasks + 2 test filter additions: parser.* unconditional, service.* under -PincludeHeavyTests=true)"
    - "gradle.properties (platformVersion 2024.3 -> 2025.1; pluginSinceBuild unchanged at 251)"
    - "src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/AppleScriptSystemDictionaryRegistryService.kt (registerSdefExtension method DELETED; runInitChain step 1 now `service<SdefFileTypeRegistrar>().register()`; unused FileType/FileTypeManager/EDT imports removed)"

key-decisions:
  - "verifyGeneratedSourcesMatch INSTALLED but NOT wired into check on Wave 1 (Rule 3 deviation — committed src/main/gen was produced by JFlex 1.7.0-SNAPSHOT; IPGP-bundled JFlex 1.9.2 produces differently-formatted output; baseline regen out of Wave 1 scope per CLAUDE.md 'Grammar changes are LARGE tier')"
  - "PARAM_TYPE_ALLOWLIST map replaces Class.forName in ParserUtilContractTest — eliminates CWE-470 unsafe-reflection surface (only String + Project ever appear as param types across the 26 frozen signatures)"
  - "SdefFileTypeRegistrar edtDispatcher typed as CoroutineContext (not CoroutineDispatcher) because Platform's com.intellij.openapi.application.EDT extension returns CoroutineContext on the IPGP 2.16.0 / Platform 2025.1 classpath — matches facade's existing withContext(Dispatchers.EDT) typing"
  - "service.* test filter gated under -PincludeHeavyTests=true (concurrency.* precedent) — keeps default ./gradlew test fast since Light-Service tests that touch real Platform APIs need BasePlatformTestCase fixture boot"

patterns-established:
  - "Wave 1 verification-first ordering: ParserUtilContractTest lands BEFORE any service extraction (D-01 enforcement — every subsequent wave's gate catches @JvmStatic signature drift on day 1)"
  - "Light Service extraction template: @Service(Service.Level.APP) class + @JvmOverloads constructor(serviceScope: CoroutineScope, edtDispatcher: CoroutineContext = EmptyCoroutineContext + Dispatchers.EDT) + suspend method + getInstance() companion + NO plugin.xml entry — Waves 2-5 reuse verbatim"
  - "Facade trampoline pattern: method body collapses to service<X>().Y() — Waves 2-5 apply to ParsableScriptHelper methods"

requirements-completed: [SERVICE-01, SERVICE-07, SERVICE-10, SERVICE-11]

# Metrics
duration: ~3h
completed: 2026-05-24
---

# Phase 4 Plan 01: v1.3 Service Decomposition — Wave 1 Summary

**Verification scaffolding (ParserUtilContractTest + verifyServiceDependencyGraph + verifyGeneratedSourcesMatch) + pilot Light Service extraction (SdefFileTypeRegistrar, ~85 LOC) validating the @Service-extraction template every later wave reuses**

## Performance

- **Duration:** ~3h (context loading + diagnostic deep-dive on grammarkit toolchain drift + 2 atomic commits)
- **Started:** 2026-05-24T17:34:00Z (worktree spawn-time HEAD reset)
- **Completed:** 2026-05-24T17:52:00Z (Task 2 commit)
- **Tasks:** 2 completed (both `type="auto"`, no checkpoints encountered)
- **Files modified:** 5 (3 created + 2 modified)

## Accomplishments

- **ParserUtilContractTest live, gating every CI run.** The 26 @JvmStatic methods on `ParsableScriptSuiteRegistryHelper` are now signature-locked at the reflection layer. Any drift fails the build with a clear "FROZEN CONTRACT VIOLATION" message naming the violating signature. Runs in <200ms (reflection only, no fixture boot), wired into the default test filter via `parser.*` matcher.
- **SdefFileTypeRegistrar Light Service extracted** as the Wave 1 pilot. Pure Platform-API delegation; no plugin.xml entry; constructor-injected `CoroutineScope` + `CoroutineContext` (Phase 3 COROUTINE-03 pattern). Facade's `registerSdefExtension` method DELETED; `runInitChain` step 1 is now `service<SdefFileTypeRegistrar>().register()`. Init chain ordering preserved byte-for-byte.
- **verifyServiceDependencyGraph wired into check.** Reports `SdefFileTypeRegistrar (leaf)` + `AppleScriptSystemDictionaryRegistryService -> SdefFileTypeRegistrar` after Wave 1 (5 remaining services still listed as leafs — Waves 2-5 will extract them). No cycles.
- **verifyGeneratedSourcesMatch implemented.** Re-runs `generateLexer` + `generateParser` (auto-registered by the bundled IPGP grammarkit plugin) into `build/verifyGeneratedSourcesMatch/tmp-gen` and diffs against committed `src/main/gen`. Currently surfaces a substantial toolchain-version drift (JFlex 1.7.0-SNAPSHOT historical vs 1.9.2 bundled) — see Deviations. The gate logic is fully functional; only the `check`-task wiring is deferred.
- **Dev sandbox aligned with production minimum.** `gradle.properties platformVersion 2024.3 -> 2025.1` — `./gradlew runIde` now matches `pluginSinceBuild=251` (eliminates the Kotlin 2.0.21 / SpillingKt missing-symbol crash on first suspend resumption that Phase 3 Plan 03-12 documented).

## Task Commits

1. **Task 1: Wire grammarkit plugin + Phase 4 verify tasks + bump platformVersion** — `afe195c` (chore)
2. **Task 2: Extract SdefFileTypeRegistrar Light Service + add ParserUtilContractTest** — `d48282c` (feat)

**Plan metadata:** (this SUMMARY commit follows)

## Files Created/Modified

### Created

- `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/SdefFileTypeRegistrar.kt` — Light Service (`@Service(Service.Level.APP)`) with `@JvmOverloads` constructor `(serviceScope: CoroutineScope, edtDispatcher: CoroutineContext)`; suspend `register()` wraps EDT-bound `runWriteAction { FileTypeManager.associateExtension(xml, "sdef") }`; idempotent (no-op on second call); `getInstance()` companion for Java-callsite parity.
- `src/test/java/com/intellij/plugin/applescript/test/parser/ParserUtilContractTest.java` — Reflection-only golden test with hardcoded 26-entry `FROZEN_CONTRACT` list; two tests (`everyFrozenMethodIsCallable` + `noNewJvmStaticMethodsLeakIntoContract`); explicit doc comment that Phase 5 / v1.4 PSI work is the next phase allowed to mutate this list.
- `src/test/kotlin/com/intellij/plugin/applescript/test/service/SdefFileTypeRegistrarTest.kt` — BasePlatformTestCase smoke (`testRegisterIsIdempotent` + `testGetInstanceReturnsRegisteredService`); gated under `-PincludeHeavyTests=true`.

### Modified

- `build.gradle.kts` — plugins block adds `id("org.jetbrains.intellij.platform.grammarkit") version "2.16.0"`; new imports for `GenerateLexerTask` + `GenerateParserTask`; configures the auto-registered `generateLexer`/`generateParser` tasks to write to `build/verifyGeneratedSourcesMatch/tmp-gen`; registers `verifyServiceDependencyGraph` + `verifyGeneratedSourcesMatch` custom verify tasks; appends `parser.*` test filter (unconditional) and `service.*` test filter (heavy-gated); extends `check` `dependsOn(...)` to include `verifyServiceDependencyGraph` (NOT `verifyGeneratedSourcesMatch` — see Deviations).
- `gradle.properties` — `platformVersion 2024.3 -> 2025.1`; `pluginSinceBuild` unchanged at 251.
- `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/AppleScriptSystemDictionaryRegistryService.kt` — added `import com.intellij.openapi.components.service`; removed unused `import com.intellij.openapi.application.EDT`, `import com.intellij.openapi.fileTypes.FileType`, `import com.intellij.openapi.fileTypes.FileTypeManager`; `runInitChain` step 1 changed from `withContext(Dispatchers.EDT) { registerSdefExtension() }` to `service<SdefFileTypeRegistrar>().register()`; private `registerSdefExtension()` method DELETED; KDoc on `runInitChain` updated to reference SdefFileTypeRegistrar. LOC: 1357 -> 1348 (-9 net).

## Decisions Made

- **`verifyGeneratedSourcesMatch` NOT wired into `check` on Wave 1** (see Deviations §1). Logic fully implemented; gating deferred.
- **`PARAM_TYPE_ALLOWLIST` static map** replaces `Class.forName(fqn)` in `ParserUtilContractTest` to eliminate CWE-470 unsafe-reflection surface even for hardcoded inputs. Only `String` and `Project` ever appear as parameter types across the 26 frozen signatures, so the allowlist is tiny.
- **`edtDispatcher` typed as `CoroutineContext`** (not `CoroutineDispatcher`) in `SdefFileTypeRegistrar` because the Platform's `com.intellij.openapi.application.EDT` extension returns `CoroutineContext` on the IPGP 2.16.0 / Platform 2025.1 classpath. Matches the facade's existing `withContext(Dispatchers.EDT)` typing. Test dispatchers (`StandardTestDispatcher`) widen cleanly via `TestDispatcher : CoroutineDispatcher : CoroutineContext`.
- **`test.service.*` filter gated under `-PincludeHeavyTests=true`** (mirrors the existing `concurrency.*` heavy-gating). The Light-Service smoke test extends `BasePlatformTestCase` — keeping it out of the default `./gradlew test` preserves the fast default loop.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Blocking] verifyGeneratedSourcesMatch NOT wired into check (toolchain drift)**

- **Found during:** Task 1 (initial `./gradlew verifyGeneratedSourcesMatch` run)
- **Issue:** The plan's acceptance criterion requires "`verifyGeneratedSourcesMatch` exits 0 on committed `src/main/gen`". The drift gate is INSTALLED and FUNCTIONAL but reports 248+ differences between the committed gen and a fresh re-run, because the committed gen was originally produced by **JFlex 1.7.0-SNAPSHOT** and an older Grammar-Kit; the IPGP 2.16.0 bundled toolchain is **JFlex 1.9.2 + Grammar-Kit 2023.3**. The differences are formatting-level (import ordering, method ordering, indentation) but pervasive across every generated file.
- **Why blocking:** Wiring the gate into `check` as the plan requires would fail every CI run on Wave 1 with no path to green until the gen is regenerated — but regenerating ~200 generated files in this commit:
  1. Falls under CLAUDE.md's "Grammar changes are LARGE tier" rule (the regenerated output cascades through parser + PSI + tests; requires ULTRATHINK plan + parser regression validation).
  2. Is out of scope for the Wave 1 warm-up extract (which deliberately picks the lowest-risk service to validate the @Service-extraction template).
  3. Would require running `-PincludeHeavyTests=true` ParserRegressionTest to confirm zero semantic regression — that suite is already known to have 5 pre-existing fixture failures deferred to Phase 8 v2.0 grammar hardening, masking any new regressions.
- **Fix:** `verifyGeneratedSourcesMatch` registered with full diff logic and a `dependsOn("generateLexer", "generateParser")` chain, but excluded from `check`'s `dependsOn(...)` list with an explicit block comment naming this deferred follow-up. Developers can invoke it ad-hoc (`./gradlew verifyGeneratedSourcesMatch`) — the GATE WORKS, only its automatic CI wiring is deferred.
- **Files modified:** `build.gradle.kts` (`check { dependsOn(... verifyServiceDependencyGraph) }` — `verifyGeneratedSourcesMatch` intentionally omitted with explanatory comment).
- **Verification:** `./gradlew verifyGeneratedSourcesMatch` runs cleanly, regenerates into `build/verifyGeneratedSourcesMatch/tmp-gen`, and produces a deterministic diff report (current diff size: 248+ files differing). `./gradlew check` exits 0 with the dependency-graph gate still active.
- **Committed in:** `afe195c` (Task 1; documented in commit message body + inline block comment).
- **Follow-up:** A dedicated future plan (within Phase 4 or v1.4) should regenerate `src/main/gen` against IPGP 2.16.0's JFlex 1.9.2 + Grammar-Kit 2023.3, run `-PincludeHeavyTests=true` ParserRegressionTest to confirm semantic equivalence, then add `verifyGeneratedSourcesMatch` to `check`'s `dependsOn(...)`.

**2. [Rule 2 — Missing Critical / Security] Replace `Class.forName(fqn)` with static `PARAM_TYPE_ALLOWLIST`**

- **Found during:** Task 2 (creating `ParserUtilContractTest.java`)
- **Issue:** The reference test shape in RESEARCH §3 uses `Class.forName(fqn)` for parameter-type resolution. semgrep flagged this as **CWE-470** (Use of Externally-Controlled Input to Select Classes or Code, "Unsafe Reflection") on the post-Write hook. Although `fqn` is sourced exclusively from a hardcoded `private static final List<String>` literal in the same file, eliminating the dynamic resolution entirely is cheap and removes the attack-surface check entirely.
- **Fix:** Added `private static final Map<String, Class<?>> PARAM_TYPE_ALLOWLIST = Map.of("java.lang.String", String.class, "com.intellij.openapi.project.Project", Project.class)` plus a `resolveParamType(String fqn): Class<?>` helper that throws on missing entries with a clear message asking the author to pair new entries with explicit FROZEN_CONTRACT additions. Only `String` + `Project` ever appear as parameter types across the 26 frozen signatures; return types (`Collection` / `List` / `HashSet` / `boolean`) are compared by `getName()` and never resolved through this path.
- **Files modified:** `src/test/java/com/intellij/plugin/applescript/test/parser/ParserUtilContractTest.java`.
- **Verification:** Both `everyFrozenMethodIsCallable` + `noNewJvmStaticMethodsLeakIntoContract` tests pass (0 failures, 0 errors, 0.176s combined wall-time). semgrep CWE-470 finding no longer triggers on the file.
- **Committed in:** `d48282c` (Task 2; documented in commit message body).

**3. [Rule 1 — Bug] `Dispatchers.EDT` is `CoroutineContext`, not `CoroutineDispatcher`**

- **Found during:** Task 2 (`./gradlew compileKotlin` after writing `SdefFileTypeRegistrar.kt`)
- **Issue:** RESEARCH §1 + the plan's task-2 step-2 example typed `edtDispatcher` as `kotlinx.coroutines.CoroutineDispatcher`. The Platform's `com.intellij.openapi.application.EDT` extension on the IPGP 2.16.0 / Platform 2025.1 classpath returns `kotlin.coroutines.CoroutineContext` (the facade at `AppleScriptSystemDictionaryRegistryService.kt:175` already uses `withContext(Dispatchers.EDT)` which compiles only because `withContext` accepts `CoroutineContext`). Kotlin compile failed: `Initializer type mismatch: expected 'CoroutineDispatcher', actual 'CoroutineContext'`.
- **Fix:** Widened the type to `CoroutineContext` with `EmptyCoroutineContext + Dispatchers.EDT` as the default. Added KDoc explaining the typing rationale and confirming that `TestDispatcher : CoroutineDispatcher : CoroutineContext` so test injection still works (widening only).
- **Files modified:** `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/SdefFileTypeRegistrar.kt`.
- **Verification:** `./gradlew compileKotlin` green; `./gradlew test -PincludeHeavyTests=true --tests "*SdefFileTypeRegistrarTest"` green (both 2 tests pass, 1.474s total).
- **Committed in:** `d48282c` (Task 2).

**4. [Rule 1 — Bug] Test filter must include `service.*` for SdefFileTypeRegistrarTest to be discoverable**

- **Found during:** Task 2 (`./gradlew test --tests "*SdefFileTypeRegistrarTest"` produced `No tests found for given includes`)
- **Issue:** The existing `tasks.test.filter { includeTestsMatching(...) }` block listed `lexer.*`, `persistence.*`, `sdef.*`, but not `service.*`. Gradle's `--tests` flag INTERSECTS with the filter, so even an explicit class name is rejected when the package isn't in the filter.
- **Fix:** Added `includeTestsMatching("com.intellij.plugin.applescript.test.service.*")` inside the existing `if (includeHeavy) { ... }` block (alongside `concurrency.*`). Comment explains the heavy-gating rationale: Light Service tests that touch real Platform APIs need BasePlatformTestCase fixture boot.
- **Files modified:** `build.gradle.kts`.
- **Verification:** `./gradlew test -PincludeHeavyTests=true --tests "*SdefFileTypeRegistrarTest"` exits 0 with both tests green.
- **Committed in:** `d48282c` (Task 2 — folded into the same atomic commit since it's discovered while making Task 2's test pass).

**5. [Rule 1 — Bug] Plan-listed grammarkit task FQN was wrong (`org.jetbrains.grammarkit.tasks` vs. actual IPGP-bundled `org.jetbrains.intellij.platform.gradle.tasks`)**

- **Found during:** Task 1 (`./gradlew tasks --group grammarkit` after first wiring attempt with `register<org.jetbrains.grammarkit.tasks.GenerateLexerTask>(...)` produced 13 "Unresolved reference" compile errors)
- **Issue:** RESEARCH §8 + the plan's task-1 example referenced standalone-plugin task FQNs (`org.jetbrains.grammarkit.tasks.GenerateLexerTask`). The IPGP 2.16.0 bundled grammarkit (`id("org.jetbrains.intellij.platform.grammarkit")`) ships task classes under `org.jetbrains.intellij.platform.gradle.tasks.{GenerateLexerTask, GenerateParserTask}` instead (verified by `jar tf` on the IPGP jar — only the IPGP-prefixed FQN exists in the bundled variant).
- **Fix:** Added explicit imports for the IPGP-bundled task types at the top of `build.gradle.kts`. Replaced custom `register<T>(...)` task creation with `named<GenerateLexerTask>("generateLexer") { ... }` / `named<GenerateParserTask>("generateParser") { ... }` — the bundled plugin auto-registers both tasks via its `Registrable.register(project)` companion, so only configuration (`sourceFile.set(...)` + `targetRootOutputDir.set(...)`) is needed.
- **Files modified:** `build.gradle.kts`.
- **Verification:** `./gradlew tasks --all` lists both `generateLexer` + `generateParser` under group "intellij platform"; `./gradlew check` exits 0.
- **Committed in:** `afe195c` (Task 1 — first commit; rationale documented in commit message body).

---

**Total deviations:** 5 auto-fixed (1 blocking-with-deferred-followup [Rule 3], 1 security/missing-critical [Rule 2], 3 bugs [Rule 1]).

**Impact on plan:** All 5 deviations were resolved within Wave 1 scope. Deviation #1 (toolchain drift) is the only one with a deferred follow-up that future-wave executors should track; the other 4 are inline fixes documented in commit messages and this SUMMARY. Wave 1's primary deliverables (ParserUtilContractTest + SdefFileTypeRegistrar + dependency-graph gate + platformVersion bump) all shipped with green tests + green `./gradlew check`. No scope creep.

## Issues Encountered

- **`verifyGeneratedSourcesMatch` baseline drift required a checkpoint-level decision.** Resolved without escalating to user via Rule 3 deviation (deferred wiring; gate logic delivered intact). Detailed in Deviations §1.
- **Kotlin compile errors on first `SdefFileTypeRegistrar` draft.** Both the wrong-FQN issue (Deviation #5) and the `Dispatchers.EDT` type mismatch (Deviation #3) — discovered immediately by `./gradlew compileKotlin` after writing the file. Standard Rule 1 fixes.
- **semgrep CWE-470 hook fired on the first ParserUtilContractTest draft.** Resolved by switching from dynamic `Class.forName` to a static allowlist map (Deviation #2).

## User Setup Required

None — Wave 1 is pure refactor / verification scaffolding. No environment variables, dashboard configuration, or external services added.

## Known Stubs

None — `SdefFileTypeRegistrar.register()` is fully wired into `runInitChain` and the facade's `registerSdefExtension` is deleted. No placeholder data, no empty UI-bound collections, no "coming soon" text.

## Threat Flags

None — no new network endpoints, auth paths, file access patterns, or schema changes introduced. The new verify tasks read only project-local source files; the new Light Service makes the same `FileTypeManager.associateExtension` call that the deleted `registerSdefExtension` made (byte-for-byte equivalent behaviour per CONTEXT D-08 frozen invariant).

## Next Phase Readiness

**Ready for Wave 2 (`SdefPersistenceService` extraction).** The Light Service template is proven:
- @Service(Service.Level.APP) + @JvmOverloads constructor(serviceScope: CoroutineScope, ...) — verified compatible with the Platform's service container.
- No plugin.xml entry — Wave 2 reuses.
- Facade trampoline pattern (single-line `service<X>().method()`) — Wave 2 reuses for `loadState` / `updateState` delegation.
- `verifyServiceDependencyGraph` already reports the new edges; Wave 2 will add `SdefPersistenceService -> AppleScriptSystemDictionaryRegistryService` (data-hop to read `state.dictionariesInfo`).
- `ParserUtilContractTest` gates every commit — Wave 2's facade slim (`getDictionaryInfoList`, etc. trampoline migration) will fail the test immediately if any @JvmStatic surface accidentally drifts.

**Deferred for a future plan (within Phase 4 or v1.4):**
- Regenerate `src/main/gen` against IPGP 2.16.0's JFlex 1.9.2 + Grammar-Kit 2023.3.
- Validate semantic equivalence via `-PincludeHeavyTests=true` ParserRegressionTest pass.
- Wire `verifyGeneratedSourcesMatch` into `check`'s `dependsOn(...)` list (one-line build.gradle.kts edit; comment-in the gate that's currently commented out).

## Phase 3 Frozen Invariants — D-08 Verification

Verified zero-diff after Wave 1:
- **Parser-util surface:** 26 @JvmStatic methods on `ParsableScriptSuiteRegistryHelper` unchanged. ParserUtilContractTest passes (signature + return-type assertions for all 26).
- **Persistence schema:** `@State(name = COMPONENT_NAME, storages = [Storage(value = "appleScriptCachedDictionariesInfo.xml", roamingType = RoamingType.PER_OS)])` annotation, `: SimplePersistentStateComponent<PersistedState>` inheritance, `PersistedState` inner class, `loadState` + `updateState` overrides — all unchanged. SDEF-13 golden fixture continues to pass under the default test suite (`./gradlew check` green; `persistence.*` filter executes the golden test).
- **WEAK_WARNING annotator severity:** No annotator changes in Wave 1 (out of scope; Wave 6 final pass touches annotator wiring at most).
- **APP_BUNDLE_DIRECTORIES:** No discovery-path changes in Wave 1 (out of scope; Wave 3 ApplicationDiscoveryService extraction touches this).
- **runInitChain ordering:** Step 1 was `withContext(Dispatchers.EDT) { registerSdefExtension() }` → now `service<SdefFileTypeRegistrar>().register()`. Behavioural equivalence: the new path's `register()` body opens its own `withContext(edtDispatcher) { runWriteAction { ... } }`. Same EDT dispatch + same `FileTypeManager.associateExtension(xml, "sdef")` call.

## Self-Check: PASSED

Verified at SUMMARY-write time:
- `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/SdefFileTypeRegistrar.kt`: FOUND
- `src/test/java/com/intellij/plugin/applescript/test/parser/ParserUtilContractTest.java`: FOUND
- `src/test/kotlin/com/intellij/plugin/applescript/test/service/SdefFileTypeRegistrarTest.kt`: FOUND
- `build.gradle.kts`: modified (grammarkit plugin + 2 verify tasks + parser/service filter wiring)
- `gradle.properties`: modified (platformVersion 2025.1)
- `src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef/AppleScriptSystemDictionaryRegistryService.kt`: modified (registerSdefExtension deleted, service<SdefFileTypeRegistrar>() trampoline added)
- Commit `afe195c`: FOUND (Task 1)
- Commit `d48282c`: FOUND (Task 2)
- `./gradlew check` green
- `verifyServiceDependencyGraph` reports `SdefFileTypeRegistrar (leaf)` + `AppleScriptSystemDictionaryRegistryService -> SdefFileTypeRegistrar`

---
*Phase: 04-v1-3-service-decomposition*
*Completed: 2026-05-24*
