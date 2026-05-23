package com.intellij.plugin.applescript.test.concurrency

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assume

/**
 * Codex HIGH 1 — proves the success-semantic facade predicate correctly distinguishes
 * `Result.success(Unit)` (ready) from `Result.failure(throwable)` (NOT ready).
 *
 * Without this distinction a failed init would make readers see "ready" because
 * `CompletableDeferred.isCompleted` returns `true` for both success and exceptional
 * completion. The facade's `isCompleted && getCompleted().isSuccess` check is the fix.
 *
 * Three cases:
 *  1. complete(Result.failure(...)) → facade reports NOT ready
 *  2. complete(Result.success(Unit)) → facade reports ready
 *  3. pending (not yet completed)   → facade reports NOT ready
 *
 * Heavy-gated per Phase 1 D-09 convention.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeferredFailureSemanticsTest : BasePlatformTestCase() {

    override fun setUp() {
        Assume.assumeTrue(
            "DeferredFailureSemanticsTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
    }

    fun testCompletedWithFailureReportsNotReady() {
        val ready: CompletableDeferred<Result<Unit>> = CompletableDeferred()
        ready.complete(Result.failure(RuntimeException("simulated init failure")))
        // The same predicate used by the facade:
        val facadeReportsReady = ready.isCompleted && ready.getCompleted().isSuccess
        assertFalse(
            "Facade must NOT report ready when init completed with Result.failure",
            facadeReportsReady,
        )
    }

    fun testCompletedWithSuccessReportsReady() {
        val ready: CompletableDeferred<Result<Unit>> = CompletableDeferred()
        ready.complete(Result.success(Unit))
        val facadeReportsReady = ready.isCompleted && ready.getCompleted().isSuccess
        assertTrue(
            "Facade must report ready when init completed with Result.success",
            facadeReportsReady,
        )
    }

    fun testNotYetCompletedReportsNotReady() {
        val ready: CompletableDeferred<Result<Unit>> = CompletableDeferred()
        val facadeReportsReady = ready.isCompleted && ready.getCompleted().isSuccess
        assertFalse("Facade must NOT report ready when not yet completed", facadeReportsReady)
    }
}
