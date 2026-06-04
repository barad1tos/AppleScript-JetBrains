package com.intellij.plugin.applescript.lang.ide.sdef

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.annotations.VisibleForTesting

internal class DictionaryReadinessTracker {
    @VisibleForTesting
    internal val standardReady: CompletableDeferred<Result<Unit>> = CompletableDeferred()

    @VisibleForTesting
    internal val appsReady: CompletableDeferred<Result<Unit>> = CompletableDeferred()

    fun isStandardReady(): Boolean = standardReady.isSuccessful()

    fun areAppsReady(): Boolean = appsReady.isSuccessful()

    suspend fun awaitStandardReady(): Result<Unit> = standardReady.await()

    suspend fun awaitAppsReady(): Result<Unit> = appsReady.await()

    fun completeStandardReady() {
        standardReady.complete(Result.success(Unit))
    }

    fun completeAppsReady() {
        appsReady.complete(Result.success(Unit))
    }

    fun completeFailures() {
        completeFailureIfPending(
            deferred = standardReady,
            message = "standardReady init failed",
        )
        completeFailureIfPending(
            deferred = appsReady,
            message = "appsReady init failed",
        )
    }

    private fun completeFailureIfPending(
        deferred: CompletableDeferred<Result<Unit>>,
        message: String,
    ) {
        if (!deferred.isCompleted) {
            deferred.complete(Result.failure(IllegalStateException(message)))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CompletableDeferred<Result<Unit>>.isSuccessful(): Boolean = isCompleted && getCompleted().isSuccess
}
