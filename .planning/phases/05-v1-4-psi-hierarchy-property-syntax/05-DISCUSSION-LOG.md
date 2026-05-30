# Phase 5: v1.4 PSI Hierarchy + Property Syntax - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-30
**Phase:** 5-v1-4-psi-hierarchy-property-syntax
**Areas discussed:** Pilot strategy, Conversion sequencing, @get:JvmName policy, Sealing aggressiveness

---

## Pilot strategy (PSI-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Two pilots | Property-conversion on a 0-gen leaf (AppleScriptPropertyDefinition) + separate sealing-viability pilot on a gen-implemented interface; isolates variables | ✓ |
| One combined pilot | One gen-implemented interface validates the whole risky path at once; faster but conflates 4 mechanisms | |

**User's choice:** Two pilots (Recommended)
**Notes:** PSI-02's sealing question is only meaningful on a gen-implemented interface; separating property-bridge from sealing means a failure points to one mechanism, not four. Aligns with Phase 3/4 incremental-gate lesson.

---

## Conversion sequencing (PSI-03/04)

| Option | Description | Selected |
|--------|-------------|----------|
| Leaf-domain → gen-grammar | 0-gen SDEF interfaces first (no BNF/regen), then gen-grammar interfaces with BNF+regen | ✓ |
| By-interface (dependency order) | Each interface incl. its BNF+regen; uniform but mixes safe + risky from the start | |
| By-package | Organizationally clean but cuts across the 0-gen/gen risk split | |

**User's choice:** Leaf-domain → gen-grammar (Recommended)
**Notes:** The 0-gen vs gen-consumed split is the natural risk axis. Front-loads safe wins; risk concentrated late but pilots have proven the path by then. Mirrors Phase 4 leaf-up D-02 + waves D-01.

---

## @get:JvmName policy (PSI-03)

| Option | Description | Selected |
|--------|-------------|----------|
| Blanket on interface getters | @get:JvmName on every Java-reachable interface getter; pure-internal skipped | ✓ |
| Surgical (proven consumers only) | Only where Java/gen/parser-util actually consumes; cleaner but audit-heavy + miss-risk | |

**User's choice:** Blanket on interface getters (Recommended)
**Notes:** PITFALLS 2.1 is a BLOCKER; blanket removes per-getter consumer analysis + its miss-risk (one miss = runtime NoSuchMethodError). Reflective JVM-signature test (ROADMAP criterion 3) locks every name. `is`-prefixed Booleans keep prefix.

---

## Sealing aggressiveness (PSI-01/05)

| Option | Description | Selected |
|--------|-------------|----------|
| Conservative — only 0-gen | Seal only interfaces with zero src/main/gen/ implementers; guaranteed safe | ✓ |
| Aggressive — also gen if pilot allows | Also seal gen-implemented ones if PSI-02 shows runtime-exhaustiveness only; bets grammar-PSI on runtime check | |

**User's choice:** Conservative — only 0-gen (Recommended)
**Notes:** PITFALLS 5.1/5.2. Highest-blast-radius phase; SDEF-domain sealing win is real, grammar-PSI sealing is marginal at high risk. Gen-interface sealing deferred to post-v1.6 if the pilot is encouraging.

---

## Claude's Discretion

- Exact pilot interfaces (property pilot: AppleScriptPropertyDefinition recommended; sealing pilot: a gen-implemented interface TBD by researcher).
- Reflective JVM-signature test shape/location/framework (Java for now).
- `data class + FakePsiElement` split (PSI-06) — where needed is audit-driven.
- BNF `{methods=...}` + `src/main/gen/` regen mechanics and per-interface order in the gen-grammar half.
- Whether build-config chores (platformVersion bump) ride a Phase 5 wave.

## Deferred Ideas

- Aggressive sealing of gen-implemented grammar interfaces → post-v1.6.
- AppleScriptGeneratedParserUtil.java → Kotlin migration → out of scope (post-v1.6).
- Test Java→Kotlin port → Phase 6/v1.5.
- gradle.properties platformVersion bump + commons-lang3 CVE audit → backlog/build-config chore.
- v1.4.0 Marketplace publish → operator-initiated at milestone close.
- REQUIREMENTS Traceability table + ROADMAP Phase 3 row reconciliation → Phase 7.
