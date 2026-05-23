package com.intellij.plugin.applescript.test.spikes

import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.runBlocking

/**
 * Codex MEDIUM 4 spike — proves at compile time the shape of the v2.x bundled
 * `withBackgroundProgress` API regarding nullable `Project`.
 *
 * SPIKE OUTCOME (recorded 2026-05-23):
 *   - WITH `!!` (force non-null): compiles cleanly. The API import resolves and the
 *     signature accepts a `Project` value.
 *   - WITHOUT `!!` (pass `Project?` directly): FAILS to compile with
 *     "Argument type mismatch: actual type is 'Project?', but 'Project' was expected."
 *
 * Conclusion: the bundled `withBackgroundProgress(project: Project, ...)` signature in
 * the LCD IDE matrix (2024.3.7.1 / 2025.1.7.1 / 2025.2.6.2) requires a non-null Project.
 * For application-scope (project-less) progress, production code falls back to
 * `Task.Backgroundable(project = null, ...)` per Codex MEDIUM 4 spike-fail path —
 * `Task.Backgroundable` is documented to accept `null` as the project parameter.
 *
 * This file is retained as ongoing API-shape proof — every CI run re-validates that
 * `withBackgroundProgress` still exists with at least a `Project` (non-null) signature,
 * so future Platform API removal or further nullability tightening surfaces at build
 * time, not at runtime.
 *
 * NOT a runtime test — this is a compile-time API-shape proof. Method is private +
 * `@Suppress("unused")` so it never executes and never participates in test discovery.
 */
@Suppress("unused")
private fun spikeWithBackgroundProgressAcceptsNullProject() = runBlocking {
    val nullProject: Project? = null
    // Note: `!!` is mandatory here per the spike outcome above — direct `Project?` does
    // not compile against the bundled signature. Keeping the call wired (rather than
    // commented out) ensures the import + invocation are revalidated by every CI run.
    withBackgroundProgress<Unit>(
        project = nullProject!!,
        title = "spike: AppleScript indexer",
        cancellable = true,
    ) {
        // no-op block
    }
}
