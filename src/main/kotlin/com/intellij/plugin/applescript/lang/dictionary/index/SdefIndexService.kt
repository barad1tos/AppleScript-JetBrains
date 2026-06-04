package com.intellij.plugin.applescript.lang.dictionary.index

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.plugin.applescript.lang.parser.ParsableScriptSuiteRegistryHelper
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdom.JDOMException
import java.io.File
import java.io.IOException

private val LOG: Logger = Logger.getInstance("#${SdefIndexService::class.java.name}")

private fun startsWithWord(
    string: String,
    prefix: String,
): Boolean = string.startsWith(prefix) && (prefix.length == string.length || ' ' == string[prefix.length])

/**
 * Phase 4 SERVICE-05 + SERVICE-09 (Wave 5): SDEF index ownership.
 *
 * CQRS per D-03:
 * - WRITE: `suspend fun ingest(applicationName, xmlFile): IngestResult` — IO-aware, hermetic-test seam.
 * - READ: sync `lookup*` methods — parser-util hot path; cannot suspend per FROZEN_CONTRACT.
 *
 * Coordinates:
 * - 14 ConcurrentHashMap indexes delegated to [SdefIndexStore].
 * - 21 sync lookup methods (migrated 1:1 from facade `isXxx` bodies).
 * - [findStdCommands] + [findApplicationCommands] (EDT-guarded + bounded-wait readiness
 *   bridges preserved per Phase 3 Review MEDIUM 1 + HIGH 5 / HIGH 1).
 * - XML parsing pipeline (`parseDictionaryFile` + 3 element handlers + 7 companion helpers).
 *
 * Cycle-prevention (plan-checker iteration-1 BLOCKER 1 + iteration-2 BLOCKER mitigation):
 *
 * `isInitialized()` + `areAppDictionariesIndexed()` STAY on the facade because they own the
 * Phase 3 `CompletableDeferred<Result<Unit>>` lifecycle (D-01 / D-04). SdefIndexService consults
 * those facade-owned predicates via [ParsableScriptSuiteRegistryHelper] (the @JvmStatic shim in
 * `lang/parser/`, NOT in the services list scanned by `verifyServiceDependencyGraph`). This
 * avoids the `SdefIndexService -> AppleScriptSystemDictionaryRegistryService` back-edge that DFS
 * would otherwise detect as a cycle. Parser-facing lookup trampolines live on
 * [ParsableScriptSuiteRegistryHelper], while the application-level facade only invokes the write
 * path through an injected parser dependency.
 *
 * Dependencies (real service-graph edges):
 * - service<AppleScriptProjectDictionaryService> — accessed from `findApplicationCommands` to
 *   resolve project-scoped dictionaries; same pattern as pre-Wave-5 facade.
 */
@Service(Service.Level.APP)
@Suppress("TooManyFunctions")
class SdefIndexService
    @JvmOverloads
    constructor(
        internal val serviceScope: CoroutineScope,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        private val indexStore = SdefIndexStore()
        private val indexIngestor = SdefIndexIngestor(indexStore, LOG)
        private val commandLookup = SdefCommandLookup(serviceScope, indexStore)

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

        // Sync lookup methods for the parser-util hot path.
        // Each method preserves the facade's `if (!isInitialized()) return false` gate, but the
        // gate now routes through [ParsableScriptSuiteRegistryHelper] (the @JvmStatic shim) to
        // avoid the SdefIndexService -> facade back-edge that verifyServiceDependencyGraph would
        // detect as a cycle.

        fun lookupStdLibClass(name: String): Boolean {
            if (!facadeInitialized()) return false
            return indexStore.stdClassNameToApplicationNameSetMap.containsKey(name)
        }

        fun lookupApplicationClass(
            applicationName: String,
            className: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val classNameSet: Set<String>? = indexStore.applicationNameToClassNameSetMap[applicationName]
            return classNameSet != null && classNameSet.contains(className)
        }

        fun lookupStdLibClassPluralName(pluralName: String): Boolean {
            if (!facadeInitialized()) return false
            return indexStore.stdClassNamePluralToApplicationNameSetMap.containsKey(pluralName)
        }

        fun lookupApplicationClassPluralName(
            applicationName: String,
            pluralClassName: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val pluralClassNameSet: Set<String>? = indexStore.applicationNameToClassNamePluralSetMap[applicationName]
            return pluralClassNameSet != null && pluralClassNameSet.contains(pluralClassName)
        }

        fun lookupStdClassWithPrefixExist(classNamePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(classNamePrefix, indexStore.stdClassNameToApplicationNameSetMap.keys)
        }

        fun lookupClassWithPrefixExist(
            applicationName: String,
            classNamePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(classNamePrefix, indexStore.applicationNameToClassNameSetMap[applicationName])
        }

        fun lookupStdClassPluralWithPrefixExist(namePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, indexStore.stdClassNamePluralToApplicationNameSetMap.keys)
        }

        fun lookupClassPluralWithPrefixExist(
            applicationName: String,
            pluralClassNamePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(
                pluralClassNamePrefix,
                indexStore.applicationNameToClassNamePluralSetMap[applicationName],
            )
        }

        fun lookupStdCommand(name: String): Boolean {
            if (!facadeInitialized()) return false
            return indexStore.stdCommandNameToApplicationNameSetMap.containsKey(name)
        }

        fun lookupApplicationCommand(
            applicationName: String,
            commandName: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val appCommands: Set<String>? = indexStore.applicationNameToCommandNameSetMap[applicationName]
            return appCommands != null && appCommands.contains(commandName)
        }

        fun lookupCommandWithPrefixExist(
            applicationName: String,
            commandNamePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(
                commandNamePrefix,
                indexStore.applicationNameToCommandNameSetMap[applicationName],
            )
        }

        fun lookupStdCommandWithPrefixExist(namePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, indexStore.stdCommandNameToApplicationNameSetMap.keys)
        }

        fun lookupStdProperty(name: String): Boolean {
            if (!facadeInitialized()) return false
            return indexStore.stdPropertyNameToDictionarySetMap.containsKey(name)
        }

        fun lookupStdPropertyWithPrefixExist(namePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, indexStore.stdPropertyNameToDictionarySetMap.keys)
        }

        fun lookupApplicationProperty(
            applicationName: String,
            propertyName: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val propertySet: Set<String>? = indexStore.applicationNameToPropertySetMap[applicationName]
            return propertySet != null && propertySet.contains(propertyName)
        }

        fun lookupPropertyWithPrefixExist(
            applicationName: String,
            propertyNamePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(
                propertyNamePrefix,
                indexStore.applicationNameToPropertySetMap[applicationName],
            )
        }

        fun lookupStdConstant(name: String): Boolean {
            if (!facadeInitialized()) return false
            return indexStore.stdEnumeratorConstantNameToApplicationNameListMap.containsKey(name)
        }

        fun lookupApplicationConstant(
            applicationName: String,
            constantName: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val applicationConstantSet: Set<String>? =
                indexStore.applicationNameToEnumeratorConstantNameSetMap[applicationName]
            return applicationConstantSet != null && applicationConstantSet.contains(constantName)
        }

        fun lookupStdConstantWithPrefixExist(namePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, indexStore.stdEnumeratorConstantNameToApplicationNameListMap.keys)
        }

        fun lookupConstantWithPrefixExist(
            applicationName: String,
            namePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(
                namePrefix,
                indexStore.applicationNameToEnumeratorConstantNameSetMap[applicationName],
            )
        }

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

        // Helpers.

        private fun isNameWithPrefixExist(
            namePrefix: String,
            nameSet: Set<String>?,
        ): Boolean = nameSet?.any { objectName -> startsWithWord(objectName, namePrefix) } == true

        /**
         * Thin proxy to the facade's `isInitialized` trampoline
         * that bypasses the service-graph by going through Phase 3's @JvmStatic helper class
         * ([ParsableScriptSuiteRegistryHelper], NOT in the services list scanned by
         * `verifyServiceDependencyGraph`). This avoids the
         * `SdefIndexService -> AppleScriptSystemDictionaryRegistryService` back-edge that DFS would
         * otherwise detect as a cycle (plan-checker BLOCKER 1 mitigation).
         */
        private fun facadeInitialized(): Boolean = SdefIndexReadiness.isInitialized()

        companion object {
            @JvmStatic
            fun getInstance(): SdefIndexService =
                ApplicationManager
                    .getApplication()
                    .getService(SdefIndexService::class.java)
        }
    }
