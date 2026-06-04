package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class DictionaryStartupPipeline(
    private val ioDispatcher: CoroutineDispatcher,
    private val readiness: DictionaryReadinessTracker,
    private val actions: DictionaryStartupActions,
) {
    /*
     * Startup is the application-service boundary: unexpected runtime failures must be logged
     * and represented as failed readiness gates instead of escaping as successful initialization.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun run() {
        withContext(ioDispatcher) {
            var shouldCompleteFailures = true
            try {
                actions.registerFileTypes()
                actions.loadCachedDictionaries()
                actions.initializeStandardDictionaries()
                readiness.completeStandardReady()
                actions.discoverInstalledApplicationNames()
                readiness.completeAppsReady()
                actions.restartOpenProjectDaemons()
            } catch (e: CancellationException) {
                shouldCompleteFailures = false
                throw e
            } catch (e: ProcessCanceledException) {
                shouldCompleteFailures = false
                throw e
            } catch (e: RuntimeException) {
                LOG.error("Error while initializing service", e)
            } finally {
                if (shouldCompleteFailures) {
                    readiness.completeFailures()
                }
            }
        }
    }

    private companion object {
        val LOG: Logger = Logger.getInstance("#${DictionaryStartupPipeline::class.java.name}")
    }
}

internal class DictionaryStartupActions(
    val registerFileTypes: suspend () -> Unit,
    val loadCachedDictionaries: () -> Unit,
    val initializeStandardDictionaries: () -> Unit,
    val discoverInstalledApplicationNames: suspend () -> Unit,
    val restartOpenProjectDaemons: () -> Unit,
)
