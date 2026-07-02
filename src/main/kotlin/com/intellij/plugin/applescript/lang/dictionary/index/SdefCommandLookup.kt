package com.intellij.plugin.applescript.lang.dictionary.index

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.parser.ParsableScriptSuiteRegistryHelper
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val LOG: Logger = Logger.getInstance("#${SdefCommandLookup::class.java.name}")

private val COMMAND_READY_TIMEOUT: Duration = 2.seconds

internal object SdefIndexReadiness {
    fun isInitialized(): Boolean = ParsableScriptSuiteRegistryHelper.isInitialized()

    fun areAppDictionariesIndexed(): Boolean = ParsableScriptSuiteRegistryHelper.areAppDictionariesIndexed()
}

internal class SdefCommandLookup(
    private val serviceScope: CoroutineScope,
    private val indexStore: SdefIndexStore,
) {
    fun lookupStdCommand(name: String): Boolean = lookupStdCommandResult(name).isHit

    fun lookupStdCommandResult(name: String): LookupResult =
        when {
            !SdefIndexReadiness.isInitialized() -> LookupResult.Stale
            indexStore.stdCommandNameToApplicationNameSetMap.containsKey(name) -> LookupResult.Hit
            else -> LookupResult.Miss
        }

    fun lookupApplicationCommand(
        applicationName: String,
        commandName: String,
    ): Boolean = lookupApplicationCommandResult(applicationName, commandName).isHit

    fun lookupApplicationCommandResult(
        applicationName: String,
        commandName: String,
    ): LookupResult =
        when {
            !SdefIndexReadiness.areAppDictionariesIndexed() -> LookupResult.Stale
            commandName in (indexStore.applicationNameToCommandNameSetMap[applicationName] ?: emptySet()) ->
                LookupResult.Hit
            else -> LookupResult.Miss
        }

    fun lookupCommandWithPrefixExist(
        applicationName: String,
        commandNamePrefix: String,
    ): Boolean = lookupCommandWithPrefixResult(applicationName, commandNamePrefix).isHit

    fun lookupCommandWithPrefixResult(
        applicationName: String,
        commandNamePrefix: String,
    ): LookupResult =
        when {
            !SdefIndexReadiness.areAppDictionariesIndexed() -> LookupResult.Stale
            hasNameWithPrefix(commandNamePrefix, indexStore.applicationNameToCommandNameSetMap[applicationName]) ->
                LookupResult.Hit
            else -> LookupResult.Miss
        }

    fun lookupStdCommandWithPrefixExist(namePrefix: String): Boolean =
        lookupStdCommandWithPrefixResult(namePrefix).isHit

    fun lookupStdCommandWithPrefixResult(namePrefix: String): LookupResult =
        when {
            !SdefIndexReadiness.isInitialized() -> LookupResult.Stale
            hasNameWithPrefix(namePrefix, indexStore.stdCommandNameToApplicationNameSetMap.keys) -> LookupResult.Hit
            else -> LookupResult.Miss
        }

    /**
     * Resolver for standard-suite commands.
     *
     * Background callers may use the bounded readiness wait. EDT callers only inspect already-ready
     * state and return immediately when dictionaries are still cold, preserving the no-freeze guard
     * without dropping command definitions after indexing has completed.
     */
    fun findStdCommands(
        project: Project,
        commandName: String,
    ): Collection<AppleScriptCommand> {
        val isOnDispatchThread = ApplicationManager.getApplication().isDispatchThread
        val isReady =
            if (isOnDispatchThread) {
                SdefIndexReadiness.isInitialized()
            } else {
                isStandardReady()
            }
        if (!isReady) {
            if (isOnDispatchThread) {
                LOG.warn(
                    "findStdCommands called from EDT before standard dictionaries are ready; " +
                        "returning empty list",
                )
            }
            return emptyList()
        }

        val appNameList = indexStore.stdCommandNameToApplicationNameSetMap[commandName] ?: emptySet()
        val result = HashSet<AppleScriptCommand>()
        for (applicationName in appNameList) {
            result.addAll(findApplicationCommands(project, applicationName, commandName))
        }
        return result
    }

    fun findStdCommandsIfReady(
        project: Project,
        commandName: String,
    ): Collection<AppleScriptCommand> {
        if (!SdefIndexReadiness.isInitialized()) return emptyList()

        val appNameList = indexStore.stdCommandNameToApplicationNameSetMap[commandName] ?: emptySet()
        val result = HashSet<AppleScriptCommand>()
        for (applicationName in appNameList) {
            result.addAll(findApplicationCommandsIfReady(project, applicationName, commandName))
        }
        return result
    }

    /**
     * Resolver for app-scoped commands.
     *
     * Background callers may use the bounded readiness wait. EDT callers only inspect already-ready
     * state and return immediately when app dictionaries are still cold, preserving the no-freeze
     * guard without dropping command definitions after indexing has completed.
     */
    fun findApplicationCommands(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand> {
        val isOnDispatchThread = ApplicationManager.getApplication().isDispatchThread
        val isReady =
            if (isOnDispatchThread) {
                SdefIndexReadiness.areAppDictionariesIndexed()
            } else {
                isAppReady()
            }
        if (!isReady) {
            if (isOnDispatchThread) {
                LOG.warn(
                    "findApplicationCommands called from EDT before app dictionaries are ready; " +
                        "returning empty list",
                )
            }
            return emptyList()
        }

        return findApplicationCommandsInReadyProjectDictionary(project, applicationName, commandName)
    }

    fun findApplicationCommandsIfReady(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand> {
        if (!SdefIndexReadiness.areAppDictionariesIndexed()) return emptyList()
        return findApplicationCommandsInCachedProjectDictionary(project, applicationName, commandName)
    }

    private fun isStandardReady(): Boolean =
        SdefIndexReadiness.isInitialized() ||
            awaitReady("standardReady", ParsableScriptSuiteRegistryHelper::awaitStandardReady)

    private fun isAppReady(): Boolean =
        SdefIndexReadiness.areAppDictionariesIndexed() ||
            awaitReady("appsReady", ParsableScriptSuiteRegistryHelper::awaitAppsReady)

    private fun awaitReady(
        gateName: String,
        awaitGate: suspend () -> Result<Unit>,
    ): Boolean {
        val future = CompletableFuture<Result<Unit>>()
        serviceScope.launch(start = CoroutineStart.UNDISPATCHED) {
            future.complete(awaitGate())
        }
        val gate =
            try {
                future.get(COMMAND_READY_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                LOG.warn(
                    "Timed out after $COMMAND_READY_TIMEOUT waiting on $gateName; " +
                        "returning empty results",
                    e,
                )
                null
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Result.failure(e)
            } catch (e: ExecutionException) {
                Result.failure(e.cause ?: e)
            }
        return gate?.isSuccess == true
    }

    private fun findApplicationCommandsInCachedProjectDictionary(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand> {
        val projectDictionaryRegistry = project.getService(AppleScriptProjectDictionaryService::class.java)
        val dictionary = projectDictionaryRegistry.getDictionary(applicationName)
        return dictionary?.findAllCommandsWithName(commandName) ?: emptyList()
    }

    private fun findApplicationCommandsInReadyProjectDictionary(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand> {
        val projectDictionaryRegistry = project.getService(AppleScriptProjectDictionaryService::class.java)
        val dictionary =
            projectDictionaryRegistry.getDictionary(applicationName)
                ?: projectDictionaryRegistry.createDictionary(applicationName)
        return dictionary?.findAllCommandsWithName(commandName) ?: emptyList()
    }
}

private val LookupResult.isHit: Boolean
    get() = this == LookupResult.Hit
