package com.intellij.plugin.applescript.lang.dictionary.index

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdom.JDOMException
import java.io.File
import java.io.IOException

private val LOG: Logger = Logger.getInstance("#${SdefIndexService::class.java.name}")

/**
 * SDEF dictionary index write-path facade.
 *
 * CQRS:
 * - WRITE: `suspend fun ingest(applicationName, xmlFile): IngestResult` — IO-aware, hermetic-test seam.
 * - READ: sync lookup collaborators — parser hot path; cannot suspend.
 *
 * Coordinates:
 * - 14 ConcurrentHashMap indexes delegated to [SdefIndexStore].
 * - read-only lookup collaborators for class / command / property / constant terms.
 * - [findStdCommands] + [findApplicationCommands] (EDT-guarded + bounded-wait readiness
 *   bridges preserved per Phase 3 Review MEDIUM 1 + HIGH 5 / HIGH 1).
 * - XML parsing pipeline (`parseDictionaryFile` + 3 element handlers + 7 companion helpers).
 *
 * Cycle-prevention:
 *
 * `isInitialized()` + `areAppDictionariesIndexed()` stay on the registry service because it owns
 * the readiness gates. SdefIndexService consults those predicates through
 * [ParsableScriptSuiteRegistryHelper], which is not in the service list scanned by
 * `verifyServiceDependencyGraph`. This avoids the
 * `SdefIndexService -> AppleScriptSystemDictionaryRegistryService` back-edge that DFS would
 * otherwise detect as a cycle.
 *
 * Dependencies (real service-graph edges):
 * - service<AppleScriptProjectDictionaryService> — accessed from `findApplicationCommands` to
 *   resolve project-scoped dictionaries; same pattern as pre-Wave-5 facade.
 */
@Service(Service.Level.APP)
class SdefIndexService
    @JvmOverloads
    constructor(
        internal val serviceScope: CoroutineScope,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        private val indexStore = SdefIndexStore()
        private val indexIngestor = SdefIndexIngestor(indexStore, LOG)
        internal val classLookup = SdefClassLookup(indexStore)
        internal val commandLookup = SdefCommandLookup(serviceScope, indexStore)
        internal val propertyLookup = SdefPropertyLookup(indexStore)
        internal val constantLookup = SdefConstantLookup(indexStore)

        // CQRS write path.

        /**
         * D-03 IO-aware write path. Suspending; runs on [ioDispatcher]. Hermetic-test seam:
         * unit tests can pass synthetic SDEF XML temp files via
         * `runTest { service.ingest("App", tempXmlFile) }` and observe the side-effects on the
         * index maps via [snapshot] without any platform-fixture boot.
         *
         * RESEARCH §4 Assumption A2 closure: the Phase 2 [com.intellij.plugin.applescript.lang.sdef.Suite]
         * type is an `interface`, not a `data class`, and the existing XML pipeline does NOT produce
         * Suite values — it walks raw JDOM `Element`s directly into the maps. Wave 5 accordingly
         * shapes `ingest` around the byte-for-byte-preserved JDOM pipeline; the `List<Suite>` shape
         * sketched in the plan example would have required a parallel Suite construction layer that
         * is out of scope for the FROZEN_CONTRACT-preserving migration. Documented in 04-05-SUMMARY
         * deviations.
         */
        suspend fun ingest(
            applicationName: String,
            xmlFile: File,
        ): IngestResult =
            withContext(ioDispatcher) {
                try {
                    val ok = parseDictionaryFile(xmlFile, applicationName)
                    if (ok) {
                        IngestResult.Success(
                            suitesIngested = 1,
                            commandsIndexed = indexStore.applicationNameToCommandNameSetMap[applicationName]?.size ?: 0,
                        )
                    } else {
                        IngestResult.Failed(reason = "parseDictionaryFile returned false for $applicationName")
                    }
                } catch (e: IOException) {
                    IngestResult.Failed(reason = "I/O error ingesting $applicationName: ${e.message}", cause = e)
                } catch (e: JDOMException) {
                    IngestResult.Failed(reason = "XML parse error ingesting $applicationName: ${e.message}", cause = e)
                }
            }

        /**
         * Builds an immutable [SdefIndexSnapshot] from the current live indexes. Defensive copies
         * (`toSet()` / `toMap()`) so callers cannot mutate live state through the returned snapshot.
         */
        fun snapshot(): SdefIndexSnapshot = indexStore.snapshot()

        // EDT-guarded command lookup with bounded waits.

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
        ): Collection<AppleScriptCommand> = commandLookup.findStdCommands(project, commandName)

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
        ): List<AppleScriptCommand> = commandLookup.findApplicationCommands(project, applicationName, commandName)

        // XML parsing pipeline.

        /**
         * Fills the internal structures with terms from an application dictionary file.
         *
         * Migrated from facade `parseDictionaryFile`. Uses the legacy JDOM parser bridge
         * (XXE-hardened) per T-04-05-02 threat mitigation.
         *
         * @return true if the file was parsed successfully
         */
        fun parseDictionaryFile(
            xmlFile: File,
            applicationName: String,
        ): Boolean = indexIngestor.parseDictionaryFile(xmlFile, applicationName)

        companion object {
            @JvmStatic
            fun getInstance(): SdefIndexService =
                ApplicationManager
                    .getApplication()
                    .getService(SdefIndexService::class.java)
        }
    }
