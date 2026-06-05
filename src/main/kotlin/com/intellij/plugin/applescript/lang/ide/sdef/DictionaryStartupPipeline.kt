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
    suspend fun run() {
        withContext(ioDispatcher) {
            val startupResult =
                runCatching {
                    actions.registerFileTypes()
                    actions.loadCachedDictionaries()
                    actions.initializeStandardDictionaries()
                    readiness.completeStandardReady()
                    actions.discoverInstalledApplicationNames()
                    readiness.completeAppsReady()
                    actions.restartOpenProjectDaemons()
                }
            val failure = startupResult.exceptionOrNull() ?: return@withContext

            when (failure) {
                is CancellationException -> throw failure
                is RuntimeException -> {
                    reportRuntimeFailure(failure)
                    readiness.completeFailures()
                }
            }
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
