// AUDIT 2026-05-24: scanned for EDT-context assumption patterns (assertFalse/assumeFalse on
// isDispatchThread, @RunsInEdt/EdtRule, ThreadingAssertions); none found. File is compatible
// with BasePlatformTestCase's EDT-by-default threading model. Audit performed during Plan 03-11
// (DEBUG.md ADDENDUM Layer 5 sweep).
package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.dictionary.discovery.DiscoveryProgressPolicy
import com.intellij.plugin.applescript.lang.dictionary.discovery.ProgressTaskCompat
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assume
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Review HIGH 3 — verifies the D-04 hybrid silent->visible progress UX via
 * `DiscoveryProgressPolicy` + `RecordingFake` test seam. NO Job-tree
 * `toString().contains()` reflection-by-string (deleted from the prior plan
 * revision).
 *
 *  (a) discovery < threshold -> RecordingFake.shownCount == 0 (silent path)
 *  (b) discovery >= threshold -> RecordingFake.shownCount == 1 (visible path)
 *  (c) cancellation after show -> RecordingFake.dismissedAfterShow == true
 *
 * Pattern I (RECURRING_PITFALLS.md): `ProgressTaskCompat` is a constructor-
 * injected interface with a production `Default` impl. The `RecordingFake`
 * below is local to this test file; nothing in production code touches a
 * `@TestOnly` global seam.
 *
 * Pattern L: defensive timing-threshold coverage. The three assertions lock
 * the three valid `(shownCount, dismissCount)` transitions of the policy's
 * state machine.
 *
 * Heavy-gated per Phase 1 D-09 convention (`-PincludeHeavyTests=true`).
 * The tests use `kotlinx.coroutines.test.TestScope` + `StandardTestDispatcher`
 * so wall-clock time is virtual — actual test wall time stays sub-second
 * despite simulating multi-second discovery durations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressBackgroundableTest : BasePlatformTestCase() {
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    override fun setUp() {
        Assume.assumeTrue(
            "ProgressBackgroundableTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        testScope = TestScope()
        testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        Disposer.register(testRootDisposable) { testScope.cancel() }
    }

    /**
     * Review HIGH 3 — recording fake for `ProgressTaskCompat` assertions. Captures
     * `show()` / `dismiss()` call counts so tests can deterministically lock the
     * policy's state-machine transitions WITHOUT inspecting Job-tree internals.
     */
    private class RecordingFake : ProgressTaskCompat {
        var shownCount: Int = 0
            private set
        var dismissCount: Int = 0
            private set
        val dismissedAfterShow: Boolean
            get() = shownCount > 0 && dismissCount > 0

        override fun show(displayName: String) {
            shownCount++
        }

        override fun dismiss() {
            dismissCount++
        }
    }

    private class BlockingShowFake : ProgressTaskCompat {
        private val showEntered = CountDownLatch(1)
        private val allowShowReturn = CountDownLatch(1)

        var dismissCount: Int = 0
            private set

        override fun show(displayName: String) {
            showEntered.countDown()
            check(allowShowReturn.await(5, TimeUnit.SECONDS)) {
                "Timed out waiting for the test to release show()"
            }
        }

        override fun dismiss() {
            dismissCount++
        }

        fun awaitShowEntered() {
            check(showEntered.await(5, TimeUnit.SECONDS)) {
                "Timed out waiting for show() to start"
            }
        }

        fun releaseShow() {
            allowShowReturn.countDown()
        }
    }

    /**
     * (a) Silent path — block completes BEFORE the visibility threshold.
     * Expected: `shownCount == 0` (indicator never surfaced).
     *
     * Uses an artificially-short visibilityThreshold (50ms) so the test runs
     * fast under virtual time while the RELATIVE ordering matches production
     * (block completes < threshold).
     */
    fun testSilentPath_indicatorDoesNotShowWhenBlockCompletesBeforeThreshold() =
        testScope.runTest {
            val fake = RecordingFake()
            val policy =
                DiscoveryProgressPolicy(
                    taskCompat = fake,
                    visibilityThreshold = 50.milliseconds,
                )
            val deferred = CompletableDeferred<Unit>()
            val job =
                launch {
                    policy.runOrTrackProgress("AppleScript: indexing dictionaries…") {
                        deferred.await()
                    }
                }
            // Block completes at virtual t=25ms (BEFORE the 50ms threshold).
            advanceTimeBy(25.milliseconds)
            deferred.complete(Unit)
            runCurrent()
            job.join()
            assertEquals("Silent path: shownCount must be 0", 0, fake.shownCount)
            assertEquals("Silent path: dismissCount must be 0 (no show -> no dismiss)", 0, fake.dismissCount)
        }

    /**
     * (b) Visible path — block exceeds the visibility threshold.
     * Expected: `shownCount == 1` (indicator surfaced exactly once) AND
     * `dismissedAfterShow == true` once the block completes.
     */
    fun testVisiblePath_indicatorShowsExactlyOnceWhenBlockExceedsThreshold() =
        testScope.runTest {
            val fake = RecordingFake()
            val policy =
                DiscoveryProgressPolicy(
                    taskCompat = fake,
                    visibilityThreshold = 50.milliseconds,
                )
            val deferred = CompletableDeferred<Unit>()
            val job =
                launch {
                    policy.runOrTrackProgress("AppleScript: indexing dictionaries…") {
                        deferred.await()
                    }
                }
            // Threshold elapses at virtual t=50ms; show() fires.
            advanceTimeBy(100.milliseconds)
            runCurrent()
            assertEquals(
                "Visible path: shownCount must be 1 after threshold elapsed",
                1,
                fake.shownCount,
            )
            // Block completes after the indicator has been shown; dismiss() fires.
            deferred.complete(Unit)
            runCurrent()
            job.join()
            assertEquals(
                "Visible path: shownCount stays 1 (no double-fire after completion)",
                1,
                fake.shownCount,
            )
            assertTrue(
                "Visible path: dismissedAfterShow must be true after block completes",
                fake.dismissedAfterShow,
            )
        }

    /**
     * (c) Cancellation propagation — cancelling the parent coroutine dismisses
     * a visible indicator. Review HIGH 3 — assertion on RecordingFake state
     * (`dismissCount` increments), NOT on Job-tree introspection.
     */
    fun testCancellation_dismissesIndicatorWhenParentCancels() =
        testScope.runTest {
            val fake = RecordingFake()
            val policy =
                DiscoveryProgressPolicy(
                    taskCompat = fake,
                    visibilityThreshold = 50.milliseconds,
                )
            val deferred = CompletableDeferred<Unit>()
            val job =
                launch {
                    policy.runOrTrackProgress("AppleScript: indexing dictionaries…") {
                        deferred.await()
                    }
                }
            advanceTimeBy(100.milliseconds)
            runCurrent()
            assertEquals("Pre-cancel: indicator must be shown", 1, fake.shownCount)
            // Cancel the parent coroutine — `block` throws CancellationException,
            // the `finally` in DiscoveryProgressPolicy.runOrTrackProgress runs the
            // dismiss path.
            job.cancel()
            runCurrent()
            assertTrue(
                "Cancellation must dismiss the indicator (Pattern G — every show has matching dismiss)",
                fake.dismissedAfterShow,
            )
        }

    fun testCompletionWhileShowIsInProgressStillDismissesIndicator() =
        runBlocking {
            val fake = BlockingShowFake()
            val policy =
                DiscoveryProgressPolicy(
                    taskCompat = fake,
                    visibilityThreshold = 1.milliseconds,
                )
            val indexingComplete = CompletableDeferred<Unit>()
            val job =
                launch(Dispatchers.Default) {
                    policy.runOrTrackProgress("AppleScript: indexing dictionaries…") {
                        indexingComplete.await()
                    }
                }

            fake.awaitShowEntered()
            indexingComplete.complete(Unit)
            withTimeout(1.seconds) {
                while (fake.dismissCount == 0) {
                    delay(10.milliseconds)
                }
            }
            fake.releaseShow()
            job.join()

            assertEquals(
                "Completion racing with show() must still dismiss the visible indicator",
                1,
                fake.dismissCount,
            )
        }
}
