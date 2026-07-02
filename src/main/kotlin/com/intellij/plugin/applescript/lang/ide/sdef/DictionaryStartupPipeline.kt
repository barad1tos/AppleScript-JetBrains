package com.intellij.plugin.applescript.lang.ide.sdef

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class DictionaryStartupPipeline(
    private val ioDispatcher: CoroutineDispatcher,
    private val readiness: DictionaryReadinessTracker,
    private val actions: DictionaryStartupActions,
    private val reportRuntimeFailure: (RuntimeException) -> Unit,
) {
    /**
     * Runs startup actions in order and returns a typed result for direct callers and tests.
     * Production failure reporting still flows through [reportRuntimeFailure].
     */
    suspend fun run(): DictionaryStartupResult =
        withContext(ioDispatcher) {
            try {
                actions.registerFileTypes()
                actions.loadCachedDictionaries()
                actions.initializeStandardDictionaries()
                readiness.completeStandardReady()
                actions.discoverInstalledApplicationNames()
                readiness.completeAppsReady()
                actions.restartOpenProjectDaemons()
                DictionaryStartupResult.Completed
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: RuntimeException) {
                reportRuntimeFailure(failure)
                readiness.completeFailures()
                DictionaryStartupResult.Failed(failure)
            }
        }
}

internal class DictionaryStartupActions(
    val registerFileTypes: suspend () -> Unit,
    val loadCachedDictionaries: () -> Unit,
    val initializeStandardDictionaries: () -> Unit,
    val discoverInstalledApplicationNames: suspend () -> Unit,
    val restartOpenProjectDaemons: () -> Unit,
)

internal sealed interface DictionaryStartupResult {
    data object Completed : DictionaryStartupResult

    data class Failed(
        val failure: RuntimeException,
    ) : DictionaryStartupResult
}
