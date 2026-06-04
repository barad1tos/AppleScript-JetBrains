package com.intellij.plugin.applescript.lang.dictionary.discovery

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Review HIGH 3 — test seam interface that abstracts the Platform progress primitive.
 *
 * Production: [ProgressTaskCompatDefault] wraps [Task.Backgroundable] with a `null`
 * project for application-scope progress.
 *
 * Follow-up: re-check this when bumping the IntelliJ Platform SDK. If
 * `withBackgroundProgress` gains application-scope or nullable-project support,
 * this compatibility wrapper can move back to the coroutine progress API.
 *
 * Tests: a `RecordingFake` (lives in `ProgressBackgroundableTest`) captures
 * `show()`/`dismiss()` calls for deterministic assertions on call counts and
 * ordering. This avoids the brittle `Job.toString().contains("BackgroundProgress")`
 * reflection-by-string approach the prior plan revision used.
 *
 * Pattern I compliance (RECURRING_PITFALLS.md): `ProgressTaskCompat` is a
 * constructor-injected interface with a production `Default` impl — NOT a
 * `@TestOnly` global seam. Production code receives `ProgressTaskCompatDefault()`;
 * tests construct `DiscoveryProgressPolicy` directly with a `RecordingFake`.
 */
interface ProgressTaskCompat {
    /**
     * Surfaces a user-visible progress indicator with [displayName] as its title.
     * Idempotent: calling [show] when an indicator is already visible is a no-op.
     */
    fun show(displayName: String)

    /**
     * Removes the indicator (if visible). Idempotent: calling [dismiss] when no
     * indicator is visible is a no-op.
     */
    fun dismiss()
}

/**
 * Production implementation. Wraps `Task.Backgroundable(project = null, ...)` for
 * application-scope progress.
 *
 * Mechanism: [show] queues a `Task.Backgroundable` whose `run(indicator)` body
 * spins on `indicator.isCanceled` OR an internal [dismissed] flag — releasing the
 * indicator when either source signals stop. [dismiss] sets the flag AND cancels
 * the stored indicator handle (when available), so the indicator collapses
 * promptly either way.
 *
 * Cancellation hand-off: when the platform's "Cancel" button is pressed, the
 * indicator's `isCanceled` flips → `run()` exits → the Backgroundable completes
 * → the indicator disappears. The discovery work itself runs OUTSIDE this task
 * (inside the registry service's `serviceScope`); cancel here does NOT cancel
 * the startup pipeline, by design (T-03-cancel-leak mitigation per the plan's STRIDE
 * register).
 */
class ProgressTaskCompatDefault : ProgressTaskCompat {
    private val dismissed = AtomicBoolean(false)
    private val visible = AtomicBoolean(false)
    private val indicatorRef = AtomicReference<ProgressIndicator?>(null)

    override fun show(displayName: String) {
        // Idempotency guard: a second show() with an already-visible indicator is a no-op.
        if (!visible.compareAndSet(false, true)) return
        // Reset dismissed flag in case this instance is being reused. The plan uses
        // a fresh instance per init pipeline so this is a defence-in-depth guard.
        dismissed.set(false)
        val task =
            object : Task.Backgroundable(
                // project =
                null,
                // title =
                displayName,
                // canBeCancelled =
                true,
            ) {
                override fun run(indicator: ProgressIndicator) {
                    indicatorRef.set(indicator)
                    indicator.isIndeterminate = true
                    // Block this background-task thread until either: (a) dismiss() flips
                    // the flag, or (b) the indicator's Cancel button is pressed. Polling
                    // interval is short (100ms) so the indicator dismissal is perceptually
                    // immediate to the user but doesn't burn CPU.
                    while (!dismissed.get() && !indicator.isCanceled) {
                        try {
                            Thread.sleep(POLL_INTERVAL_MS)
                        } catch (_: InterruptedException) {
                            // Restore interrupt status; exit loop so the task completes.
                            Thread.currentThread().interrupt()
                            break
                        }
                    }
                }
            }
        ProgressManager.getInstance().run(task)
    }

    override fun dismiss() {
        // Idempotency guard: dismiss() without a prior show() is a no-op; subsequent
        // dismiss() calls are also no-ops. The visible flag is NOT reset here so
        // a stale show() call after dismiss() also no-ops — by design (single-shot
        // semantics per init pipeline).
        if (dismissed.getAndSet(true)) return
        // Also cancel the indicator handle if available — flips isCanceled inside
        // the spinning run() loop so the task exits promptly even if the polling
        // window is mid-sleep.
        indicatorRef.get()?.cancel()
    }

    private companion object {
        const val POLL_INTERVAL_MS: Long = 100
    }
}
