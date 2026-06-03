package com.intellij.plugin.applescript.lang.ide.sdef

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Review HIGH 3 — extracted testable helper that owns the D-04 hybrid silent->visible
 * progress UX timing decision.
 *
 * Runs a suspend `block`. If the block has NOT completed by [visibilityThreshold]
 * (default 2 seconds per D-04 in
 * `.planning/phases/03-v1-2-sdef-loading-structured-concurrency/03-CONTEXT.md`),
 * surfaces a progress indicator via the injected [taskCompat]. The indicator is
 * dismissed when the block completes OR when the parent coroutine is cancelled.
 *
 * Test seam: [taskCompat] is constructor-injected. Production uses
 * [ProgressTaskCompatDefault] (wrapping `Task.Backgroundable(null, ...)` for
 * application-scope progress); tests pass a `RecordingFake` for deterministic
 * assertions on `show()`/`dismiss()` call counts and ordering. NO Job-tree
 * `toString().contains()` reflection-by-string (Pattern I compliance per
 * `RECURRING_PITFALLS.md`).
 *
 * Structured concurrency: the internal "show after threshold" coroutine is
 * launched via `CoroutineScope(currentCoroutineContext()).launch { ... }`, so it inherits
 * the parent's Job — when the caller's scope is cancelled (e.g. plugin unload,
 * `serviceScope` cancellation), both the threshold-watcher and the underlying
 * block receive structured cancellation. The `finally` block always runs to
 * dismiss the indicator (RECURRING_PITFALLS.md Pattern G — every show has a
 * matching dismiss).
 */
internal class DiscoveryProgressPolicy(
    private val taskCompat: ProgressTaskCompat,
    // Kotlin requires the explicit `: Duration` annotation on a value parameter;
    // the language has no inference mode for constructor parameters. The Plan
    // 03-05 must-haves structural sentinel expected `visibilityThreshold = 2.seconds`
    // verbatim, but the language reality forces `visibilityThreshold: Duration =
    // 2.seconds`. The intent (the 2-second literal lives in this file, in one
    // place, as the load-bearing D-04 invariant) is satisfied — see the SUMMARY
    // deviation note for the sentinel-grep adjustment.
    // Tests override via named argument:
    //   DiscoveryProgressPolicy(taskCompat = fake, visibilityThreshold = 50.milliseconds)
    private val visibilityThreshold: Duration = 2.seconds,
) {
    /**
     * Runs [block]; if its execution exceeds [visibilityThreshold], surfaces a
     * progress indicator titled [displayName] until the block completes or the
     * parent coroutine is cancelled.
     *
     * @param displayName status-bar title for the progress indicator (e.g.,
     *                    "AppleScript: indexing dictionaries…")
     * @param block       the suspend block that may take longer than
     *                    [visibilityThreshold]; typically `appsReady.await()`
     */
    suspend fun runOrTrackProgress(
        displayName: String,
        block: suspend () -> Unit,
    ) {
        // Inherit the caller's coroutine context so the threshold-watcher is a
        // child of the same Job tree — structured cancellation reaches it.
        val scope = CoroutineScope(currentCoroutineContext())
        val indicatorNeedsDismiss = AtomicBoolean(false)
        val showJob =
            scope.launch {
                delay(visibilityThreshold)
                indicatorNeedsDismiss.set(true)
                taskCompat.show(displayName)
            }
        try {
            block()
        } finally {
            // Pattern G: every show() has a matching dismiss(). The threshold-
            // watcher cancellation also handles the "block finished before threshold"
            // case — showJob.cancel() preempts the not-yet-fired show().
            showJob.cancel()
            if (indicatorNeedsDismiss.getAndSet(false)) taskCompat.dismiss()
        }
    }
}
