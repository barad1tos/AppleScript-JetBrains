# PSI-02 Sealing-Viability Pilot — Empirical Finding

**Probed:** 2026-05-30
**Target:** `AppleScriptHandler` (package `com.intellij.plugin.applescript.psi`) — the minimal GROUP B case (1 *generated-Java* implementer per the PSI-01 audit).
**Method:** throwaway `sealed` modifier added to `interface AppleScriptHandler`, `./gradlew compileKotlin compileJava` run, outcome recorded, edit reverted (single-file `git checkout`). Source working tree ends with ZERO diff.
**Satisfies:** PSI-02 / D-01b — the empirical build-vs-runtime question for sealing a gen-implemented interface.

## The Empirical Question (PSI-02 / D-01b)

When generated Java implements a `sealed` Kotlin interface, does the build break at `compileKotlin`/`compileJava`, or does only runtime `when`-exhaustiveness become a risk?

PITFALLS 5.1 **predicted** the latter: `sealed` is Kotlin-metadata-only, the Java compiler ignores it, so the generated implementer `AppleScriptHandlerInterleavedParametersDefinition.java extends AppleScriptHandler` would NOT break the build — only a runtime `NoWhenBranchMatchedException` would be at risk.

## Result: BUILD BREAK — yes, but NOT at the predicted boundary

### Build outcome: `./gradlew compileKotlin compileJava` → **FAILED** at `:compileKotlin`

Exact compiler error:

```
e: .../psi/impl/AppleScriptHandlerInterleavedParameters.kt:21:5
   A class can only extend a sealed class or interface declared in the same package.

> Task :compileKotlin FAILED
BUILD FAILED in 1s
```

`compileJava` never ran — the Kotlin compilation aborts first.

### Why this contradicts the PITFALLS 5.1 prediction

The break is **not** caused by the generated-Java implementer. It is caused by a **hand-written Kotlin** implementer the PSI-01 gen-only audit did not surface:

- **`src/main/kotlin/.../psi/impl/AppleScriptHandlerInterleavedParameters.kt:19-24`** —
  `open class AppleScriptHandlerInterleavedParameters(node: ASTNode) : AbstractAppleScriptComponent(node), AppleScriptHandler, ...`

This class lives in package `...psi.impl`, while `AppleScriptHandler` lives in package `...psi`. Kotlin's sealed rule (surfaced here as the **same-package** restriction, KT-29748 family) rejects a sealed supertype implemented from a different package. The Kotlin compiler aborts before the generated-Java boundary is ever evaluated, so the PITFALLS 5.1 "Java compiler ignores `sealed`" hypothesis was never reached — a Kotlin-side same-package violation pre-empts it.

**Audit-scope correction (for the planner):** the PSI-01 audit counted *generated-Java* implementers only (`rg ... src/main/gen/`). `AppleScriptHandler` actually has TWO implementer surfaces:

1. `src/main/gen/.../psi/AppleScriptHandlerInterleavedParametersDefinition.java` (generated Java interface, `extends AppleScriptHandler`) — the 1 counted in the audit.
2. `src/main/kotlin/.../psi/impl/AppleScriptHandlerInterleavedParameters.kt` (hand-written Kotlin class, `: ..., AppleScriptHandler, ...`) — **NOT** counted (it is hand-written, in `src/main/kotlin`, in a different package).

It is implementer (2), not (1), that breaks the build. The gen-only `rg` audit under-counts the true implementer set for sealing-viability purposes.

### Runtime outcome: not reached / no exhaustive `when (handler)` site exists anyway

`compileJava` and `-PincludeHeavyTests=true` were not run because the build failed at `compileKotlin` (no green build to test against). Independently, a static scan establishes there is **no exhaustive `when (handler)` over `AppleScriptHandler` subtypes** in `src/main/kotlin`:

- `rg -n "when ?\(" src/main/kotlin/ | rg -i handler` → only `AppleScriptHandlerSelectorPartImpl.kt:24` (`when (val parameter = getParameter())` — over a *parameter*, not over an `AppleScriptHandler`).
- All `AppleScriptHandler` type-discriminations are `if (x is AppleScriptHandler)` / `x !is AppleScriptHandler` guards (`AppleScriptPsiElementImpl.kt:69`, `AbstractAppleScriptHandlerCall.kt:68`, `AppleScriptNamesValidator.kt:45`, `AppleScriptStructureViewElement.kt:65`, two `*RefactoringSupportProvider.kt`) — single-arm boolean checks, NOT exhaustive `when` over a sealed subtype set.

So even if the build had stayed green, the runtime exhaustiveness risk for `AppleScriptHandler` is currently nil — there is no `when` that the compiler would believe exhaustive. The hypothetical runtime failure mode PITFALLS 5.1 warns about has no live call-site for this interface today.

## PSI-02 Verdict

- **Build-vs-runtime answer:** For `AppleScriptHandler`, sealing **breaks the build at `compileKotlin`** — but via a Kotlin same-package implementer (`AppleScriptHandlerInterleavedParameters.kt`), NOT via the generated-Java implementer PITFALLS 5.1 anticipated. The Java-boundary hypothesis was never reached; a Kotlin-side violation pre-empts it.
- **PITFALLS 5.1 status:** the *prediction* ("no build break, only runtime risk") is **falsified** for this interface — there IS a hard build break. But the falsification cause differs from the prediction's mechanism: it is a same-package sealed-supertype violation on a hand-written Kotlin PSI impl, not the "Java compiler silently implements a sealed interface" path. The PITFALLS 5.1 *mechanism* (Java ignores `sealed`) remains untested here because Kotlin failed first.
- **D-04 (keep all GROUP B OPEN this phase) — STANDS, reinforced.** Sealing this GROUP B interface is not merely "buys nothing / runtime-risky" — it is an immediate, hard `compileKotlin` failure under the current package layout. `AppleScriptHandler` is left UNSEALED (probe reverted; `git diff --exit-code` on the file is clean, `grep -c "sealed interface AppleScriptHandler"` = 0).
- **Post-v1.6 aggressive-sealing implication:** sealing GROUP B is NOT free even ignoring runtime exhaustiveness. Any future attempt must FIRST relocate every implementer (both the hand-written Kotlin `*Impl` classes AND, separately, contend with the generated-Java implementers) into the sealed interface's package or module-consistent arrangement, and re-run the gen-implementer + hand-written-Kotlin-implementer audit together. The gen-only audit is insufficient as a sealing-viability gate.

## Probe Hygiene

- `AppleScriptHandler.kt` restored to `interface AppleScriptHandler : AppleScriptComponent` (line 6). `git diff --exit-code src/main/kotlin/.../psi/AppleScriptHandler.kt` exits 0.
- No other source file touched; `git status --short` clean for `src/`.
- The `sealed` edit was a throwaway probe; the interface is OPEN at plan end per D-04.
