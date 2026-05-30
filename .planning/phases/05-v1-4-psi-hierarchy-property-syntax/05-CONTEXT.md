# Phase 5: v1.4 PSI Hierarchy + Property Syntax - Context

**Gathered:** 2026-05-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Convert the ~243 Java-style getter methods (`fun getX()` / `fun isX()` / `fun findX()` — baseline was 222 at 2026-05-22; drifted up ~21 from Phase 2-4 work; planner re-counts) on PSI-adjacent and SDEF-domain Kotlin interfaces to Kotlin `val`/`var` properties, with `@get:JvmName("getX")` preserving every Java-consumed accessor name. Audit which PSI interfaces are seal-safe (no implementers in `src/main/gen/`) and seal those that are — coordinated with BNF `{methods=...}` updates and `src/main/gen/` regeneration. Pilot on one small interface before broad adoption.

**Highest-blast-radius milestone of the v1.x cycle** — touches generated code. This is the first phase where Grammar-Kit `{methods=...}` changes are unlocked (frozen v1.1-v1.3 so parser-util signatures stayed stable).

Scope: property conversion + `@get:JvmName` bridge + sealing audit + seal seal-safe interfaces + BNF/regen coordination + reflective JVM-signature test (ROADMAP criterion 3). NOT in scope: `AppleScriptGeneratedParserUtil.java` Kotlin migration (frozen Java per REQUIREMENTS Out-of-Scope), test Java→Kotlin port (Phase 6/v1.5), direct `src/main/gen/` edits (regenerate from BNF only).
</domain>

<decisions>
## Implementation Decisions

### Pilot strategy (PSI-02)
- **D-01: Two separate pilots, not one combined.** (a) Property-conversion pilot on a 0-gen-implementer leaf interface (e.g. `AppleScriptPropertyDefinition`, 5 getters — verified 0 `src/main/gen/` implementers) to validate the `val` + `@get:JvmName` Java-bridge mechanics cleanly. (b) A SEPARATE sealing-viability pilot on ONE gen-implemented interface, because PSI-02's question ("does build break, or only runtime exhaustiveness fail, when generated Java implements a sealed Kotlin interface?") is only meaningful when generated Java actually implements the interface. Rationale: isolates variables — if a pilot breaks, you know whether the property-bridge or the sealing mechanism caused it (Phase 3/4 lesson: incremental gates catch layered defects; combined pilots conflate 4 mechanisms BNF+regen+@get:JvmName+sealing). Cost: +1 PR; worth it on the riskiest phase.

### Conversion sequencing (PSI-03/04)
- **D-02: Leaf-domain interfaces first, gen-grammar interfaces second.** Convert the 0-gen-implementer SDEF-domain interfaces (`AppleScriptCommand`, `DictionaryComponent`, `AppleScriptPropertyDefinition`, `AppleScriptHandler`, etc.) first — no BNF/regen needed, low blast radius — then tackle the gen-implemented PSI grammar interfaces with their BNF `{methods=...}` + `src/main/gen/` regeneration coordination. The 0-gen vs gen-consumed split IS the natural risk axis. Mirrors Phase 4's leaf-up D-02 extraction order + incremental-waves D-01. Risk is concentrated in the back half — but by then both pilots have proven the path. Each batch is a wave with its own gate (ParserUtilContractTest + heavy tests green before next).

### `@get:JvmName` policy (PSI-03)
- **D-03: Blanket `@get:JvmName("getX")` on every interface getter (Java-reachable surface).** Apply uniformly to every getter on any PSI/SDEF interface that could be reached from Java (the 1404-LOC `AppleScriptGeneratedParserUtil.java` + 125 `src/main/gen/` Java files are all Java consumers). Pure-internal/private Kotlin-only helpers skip it. Rationale: PITFALLS 2.1 (property rename breaks Java callers) is a BLOCKER; blanket-on-interface-getters eliminates the per-getter "is this consumed from Java?" analysis and its miss-risk (one missed consumer → runtime `NoSuchMethodError`, caught only by heavy tests). Verbosity is mechanical; the new reflective JVM-signature test (ROADMAP criterion 3) locks every name. `is`-prefixed Boolean getters keep the `is` prefix (PITFALLS 2.3).

### Sealing aggressiveness (PSI-01/05)
- **D-04: Conservative — seal only interfaces with zero `src/main/gen/` implementers.** Seal exactly the interfaces the PSI-01 audit (`rg "implements .*<Iface>" src/main/gen/`) proves have zero generated-Java implementers — guaranteed safe (no sealed-Kotlin + generated-Java conflict per PITFALLS 5.1). The gen-implemented PSI grammar interfaces (`AppleScriptExpression` and the bulk of the 125 gen-implementing files) stay OPEN this phase. Rationale: on the highest-blast-radius phase, the type-safety win on SDEF-domain interfaces is real, while sealing the grammar-PSI tree is marginal value at high risk (PITFALLS 5.2 over-sealed). Service-internal sealed types from v1.3 (DictionaryLoadResult, IngestResult, LookupResult, SdefIndexSnapshot) are retained. Aggressive gen-interface sealing is a deferred post-v1.6 question if the PSI-02 sealing pilot proves encouraging.

### Claude's Discretion (researcher/planner decide)
- Exact pilot interface for the property-conversion pilot (`AppleScriptPropertyDefinition` is the recommended candidate; researcher confirms it is the smallest representative 0-gen leaf).
- Exact gen-implemented interface for the sealing-viability pilot.
- Reflective JVM-signature test shape, location, framework (Java for now per Phase 6 deferral; mirrors ParserUtilContractTest pattern).
- `data class + FakePsiElement` split (PSI-06) — WHERE it is actually needed is an audit-driven researcher call (no PSI-extending class converted to `data class`).
- BNF `{methods=...}` edit + `src/main/gen/` regen mechanics and the exact per-interface coordination order within the gen-grammar back half.
- Whether the `gradle.properties platformVersion` bump / any build-config chores ride a Phase 5 wave or stay deferred.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements + roadmap
- `.planning/REQUIREMENTS.md` PSI-01..09 (lines 94-106) — 9 locked requirements for this phase
- `.planning/ROADMAP.md` Phase 5 entry (lines 165-185) — Goal, Success Criteria (note criterion 3 = reflective JVM-signature test; criterion 6 = v1.4.0 publish), Canonical refs

### Research (the v1.4 pitfalls + build-order rationale)
- `.planning/research/PITFALLS.md` 2.1 (property rename breaks Java callers — BLOCKER), 2.2 (Grammar-Kit method-mixin), 2.3 (`is`-prefixed Booleans keep prefix), 5.1 (sealed Kotlin interface cannot be implemented by generated Java), 5.2 (over-sealed service)
- `.planning/research/ARCHITECTURE.md` §4 (v1.4 build-order rationale: PSI changes touch `src/main/gen/`; doing this AFTER v1.3 means regenerating after the service split too)
- `.planning/research/FEATURES.md` A-1 (`data class + FakePsiElement` split pattern — PSI-06), STACK.md (value-class name-mangling, `@get:JvmName` semantics)

### Codebase maps
- `.planning/codebase/CONCERNS.md` "222 Java-style accessor methods" (lines 114-132, per-interface getter counts: AppleScriptPsiImplUtil 21 gen-consumed, ApplicationDictionary 12, DictionaryComponent 11, AppleScriptCommand 6, AppleScriptPropertyDefinition 5), "0 sealed hierarchies" (lines 150-158), "Generated Code Constraints" (lines 266-308: `src/main/gen/` 250 files, AppleScriptGeneratedParserUtil.java 1404 LOC, generated PSI casts to hand-written interfaces by name)

### Project policy
- `.planning/PROJECT.md` "Out of Scope: AppleScriptGeneratedParserUtil.java migration" — stays Java; "Constraints: Persistence frozen / Parser util signatures"
- `.planning/REQUIREMENTS.md` Out of Scope (lines 155-165): Grammar-Kit `{methods=...}` unlocked at v1.4; never edit `src/main/gen/` directly; persistence + run-config schema frozen
- `./CLAUDE.md` — Generated-code-untouchable rule; Grammar changes are LARGE tier (ULTRATHINK before BNF edit); commit-style; .planning/ local-only
- `~/.claude/skills/intellij-plugin-dev/` — PSI, Grammar-Kit, `@get:JvmName`, generated-code regen, RECURRING_PITFALLS.md

### Frozen invariants (carryforward — DO NOT regress)
- `.planning/phases/04-v1-3-service-decomposition/04-CONTEXT.md` D-08 frozen invariants (parser-util signatures, persistence @State schema, WEAK_WARNING, APP_BUNDLE_DIRECTORIES); ParserUtilContractTest (SERVICE-07) is the parser-util signature net to keep green
- `.planning/phases/04-v1-3-service-decomposition/04-VERIFICATION.md` — v1.3 service surface this phase builds on (stable index-service API)
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`ParserUtilContractTest`** (Java, from Phase 4 SERVICE-07) — the existing parser-util `@JvmStatic` signature net; keep green through every PSI-03/04 conversion wave. The reflective JVM-signature test (ROADMAP criterion 3) is its analog for interface getters.
- **`ParserRegressionTest.kt`** 6 heavy-gated fixtures — the parser-behavior net; run `-PincludeHeavyTests=true` after each gen-grammar conversion.
- **0-gen-implementer SDEF interfaces** (`AppleScriptCommand`, `DictionaryComponent`, `AppleScriptPropertyDefinition`, `AppleScriptHandler` — verified 0 `src/main/gen/` implementers) — the safe leaf-domain front half (D-02).
- **`@get:JvmName` precedent** — Phase 2 SDEF-01 already used property syntax on data classes where the platform-declaration-clash forced it (02-03 deviation); the bridge pattern is proven on a small scale.

### Established Patterns
- **Incremental waves with per-wave gate** (Phase 4 D-01) — each conversion batch is a wave; ParserUtilContractTest + heavy tests green before the next.
- **Leaf-up ordering by dependency/risk** (Phase 4 D-02) — applied here as 0-gen-leaf-first, gen-grammar-second.
- **Pilot-before-broad on risky mechanics** (Phase 4 SdefFileTypeRegistrar pilot) — D-01 two-pilot strategy is the same instinct on the property+sealing mechanics.

### Integration Points
- **Kotlin interface getter → `AppleScriptGeneratedParserUtil.java`** (1404 LOC, Java) — every getter the parser-util calls MUST keep its `getX()` JVM name via `@get:JvmName`. The reflective signature test enforces this.
- **Hand-written Kotlin PSI interface → `src/main/gen/` generated Java** — 125 gen Java files implement hand-written interfaces; renaming/sealing a gen-implemented interface requires BNF edit + regen (the gen-grammar back half of D-02). `AppleScriptExpression` has ≥1 gen implementer → gen-consumed (sealing-blocked under D-04 conservative).
- **BNF `{methods=...}` in `AppleScript.bnf`** — first unlocked this phase; coordinate edits with regen for any interface whose Kotlin signature change affects generated parser/PSI.
</code_context>

<specifics>
## Specific Ideas

- **Pilot candidate:** `AppleScriptPropertyDefinition` (5 getters, 0 gen implementers) is the recommended property-conversion pilot — smallest representative leaf. Researcher confirms.
- **Risk axis = 0-gen vs gen-consumed.** This split governs every D-01..D-04 decision: 0-gen interfaces are property-safe + seal-safe; gen-consumed need @get:JvmName + BNF/regen and stay sealing-blocked this phase.
- **Reflective signature test as the bridge net** (ROADMAP criterion 3) — assert every PSI/SDEF interface getter is callable from Java under its expected `getX()` name; this is what makes blanket @get:JvmName (D-03) safe to apply mechanically.
- **243 ≠ 222** — the getter count drifted up since the 2026-05-22 CONCERNS baseline; planner re-runs `rg -c '^\s*fun (get|set|is|has|find)[A-Z]' src/main/kotlin/` to get the live count and scope the waves.
</specifics>

<deferred>
## Deferred Ideas

- **Aggressive sealing of gen-implemented PSI grammar interfaces** — deferred to post-v1.6 (or revisited if the PSI-02 sealing pilot shows "runtime-exhaustiveness only, no build break"). This phase stays conservative (D-04).
- **`AppleScriptGeneratedParserUtil.java` → Kotlin migration** — explicitly out of scope (REQUIREMENTS); not before post-v1.6.
- **Test Java→Kotlin port (TESTPORT-01..06)** — Phase 6/v1.5. This phase writes the reflective signature test in Java (consistent with existing `.java` tests + Phase 4 ParserUtilContractTest).
- **`gradle.properties platformVersion` bump + `commons-lang3` CVE-2025-48924 audit** — carryforward deferred follow-ups; may ride a Phase 5 build-config chore or stay backlog.
- **v1.4.0 Marketplace publish (PSI-09)** — operator-initiated at milestone close per HOTFIX-04/SDEF-19/COROUTINE-09/SERVICE-14 precedent.
- **REQUIREMENTS.md Traceability table + ROADMAP Phase 3 row reconciliation** — Phase 7 (v1.6 milestone close) tracking-doc cleanup, per STATE.md Operator Next Steps.

### Reviewed Todos (not folded)
None — `gsd-sdk query todo.match-phase 5` returned `todo_count: 0`.
</deferred>

---

*Phase: 5-v1-4-psi-hierarchy-property-syntax*
*Context gathered: 2026-05-30*
