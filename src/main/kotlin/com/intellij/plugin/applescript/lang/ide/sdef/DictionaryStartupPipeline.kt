package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.progress.ProcessCanceledException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class DictionaryStartupPipeline(
    private val ioDispatcher: CoroutineDispatcher,
    private val readiness: DictionaryReadinessTracker,
    private val actions: DictionaryStartupActions,
) {
    suspend fun run() {
        withContext(ioDispatcher) {
            var shouldCompleteFailures = true
            var completedSuccessfully = false
            try {
                actions.registerFileTypes()
                actions.loadCachedDictionaries()
                actions.initializeStandardDictionaries()
                readiness.completeStandardReady()
                actions.discoverInstalledApplicationNames()
                readiness.completeAppsReady()
                actions.restartOpenProjectDaemons()
                completedSuccessfully = true
            } catch (e: CancellationException) {
                shouldCompleteFailures = false
                throw e
            } catch (e: ProcessCanceledException) {
                shouldCompleteFailures = false
                throw e
            } finally {
                if (!completedSuccessfully && shouldCompleteFailures) {
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
