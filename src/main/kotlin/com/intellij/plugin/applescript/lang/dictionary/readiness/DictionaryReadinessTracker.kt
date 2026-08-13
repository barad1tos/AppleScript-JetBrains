package com.intellij.plugin.applescript.lang.dictionary.readiness

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.APP)
class DictionaryReadinessTracker internal constructor() {
    @VisibleForTesting
    internal val standardReady: CompletableDeferred<Result<Unit>> = CompletableDeferred()

    @VisibleForTesting
    internal val appsReady: CompletableDeferred<Result<Unit>> = CompletableDeferred()

    internal fun isStandardReady(): Boolean = standardReady.isSuccessful()

    internal fun areAppsReady(): Boolean = appsReady.isSuccessful()

    internal suspend fun awaitStandardReady(): Result<Unit> = standardReady.await()

    internal suspend fun awaitAppsReady(): Result<Unit> = appsReady.await()

    internal fun completeStandardReady() {
        standardReady.complete(Result.success(Unit))
    }

    internal fun completeAppsReady() {
        appsReady.complete(Result.success(Unit))
    }

    internal fun completeFailures() {
        failIfPending(
            deferred = standardReady,
            message = "standardReady init failed",
        )
        failIfPending(
            deferred = appsReady,
            message = "appsReady init failed",
        )
    }

    private fun failIfPending(
        deferred: CompletableDeferred<Result<Unit>>,
        message: String,
    ) {
        if (!deferred.isCompleted) {
            deferred.complete(Result.failure(IllegalStateException(message)))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CompletableDeferred<Result<Unit>>.isSuccessful(): Boolean = isCompleted && getCompleted().isSuccess

    companion object {
        internal fun getInstance(): DictionaryReadinessTracker =
            ApplicationManager.getApplication().getService(DictionaryReadinessTracker::class.java)
    }
}
