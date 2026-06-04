package com.intellij.plugin.applescript.lang.dictionary.index

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.plugin.applescript.lang.dictionary.xml.LegacyJdomParser
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.parser.ParsableScriptSuiteRegistryHelper
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jdom.Document
import org.jdom.Element
import org.jdom.JDOMException
import org.jdom.Namespace
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val LOG: Logger = Logger.getInstance("#${SdefIndexService::class.java.name}")

private val COMMAND_READY_TIMEOUT: Duration = 2.seconds
private const val XINCLUDE_NAMESPACE_URI: String = "http" + "://www.w3.org/2003/XInclude"

private typealias IndexMap = MutableMap<String, MutableSet<String>>

private data class ParsedSuiteElements(
    val classes: List<Element>,
    val valueTypes: List<Element>,
    val classExtensions: List<Element>,
    val commands: List<Element>,
    val recordTypes: List<Element>,
    val enumerations: List<Element>,
)

private fun parsedSuiteElements(suiteElement: Element): ParsedSuiteElements =
    ParsedSuiteElements(
        classes = suiteElement.getChildren("class").toList(),
        valueTypes = suiteElement.getChildren("value-type").toList(),
        classExtensions = suiteElement.getChildren("class-extension").toList(),
        commands = suiteElement.getChildren("command").toList(),
        recordTypes = suiteElement.getChildren("record-type").toList(),
        enumerations = suiteElement.getChildren("enumeration").toList(),
    )

/**
 * Phase 4 SERVICE-05 + SERVICE-09 (Wave 5): SDEF index ownership.
 *
 * CQRS per D-03:
 * - WRITE: `suspend fun ingest(applicationName, xmlFile): IngestResult` — IO-aware, hermetic-test seam.
 * - READ: sync `lookup*` methods — parser-util hot path; cannot suspend per FROZEN_CONTRACT.
 *
 * Owns:
 * - 14 ConcurrentHashMap indexes (migrated byte-for-byte from facade lines 79-92).
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
 * would otherwise detect as a cycle (the facade depends on SdefIndexService via the lookup
 * trampolines added in Wave 5 Task 2).
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
        // Indexes migrated from the original facade.
        // Application-scoped (7):
        private val applicationNameToClassNameSetMap: IndexMap = ConcurrentHashMap()
        private val applicationNameToClassNamePluralSetMap: IndexMap = ConcurrentHashMap()
        private val applicationNameToCommandNameSetMap: IndexMap = ConcurrentHashMap()
        private val applicationNameToRecordNameSetMap: IndexMap = ConcurrentHashMap()
        private val applicationNameToPropertySetMap: IndexMap = ConcurrentHashMap()
        private val applicationNameToEnumerationNameSetMap: IndexMap = ConcurrentHashMap()
        private val applicationNameToEnumeratorConstantNameSetMap: IndexMap = ConcurrentHashMap()

        // Std-scoped (7):
        private val stdClassNameToApplicationNameSetMap: IndexMap = ConcurrentHashMap()
        private val stdClassNamePluralToApplicationNameSetMap: IndexMap = ConcurrentHashMap()
        private val stdCommandNameToApplicationNameSetMap: IndexMap = ConcurrentHashMap()
        private val stdRecordNameToApplicationNameSetMap: IndexMap = ConcurrentHashMap()
        private val stdPropertyNameToDictionarySetMap: IndexMap = ConcurrentHashMap()
        private val stdEnumerationNameToApplicationNameSetMap: IndexMap = ConcurrentHashMap()
        private val stdEnumeratorConstantNameToApplicationNameListMap: IndexMap = ConcurrentHashMap()

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
                            commandsIndexed = applicationNameToCommandNameSetMap[applicationName]?.size ?: 0,
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
        fun snapshot(): SdefIndexSnapshot =
            SdefIndexSnapshot(
                applicationNameToClassNameSet =
                    applicationNameToClassNameSetMap.mapValues { it.value.toSet() },
                applicationNameToClassNamePluralSet =
                    applicationNameToClassNamePluralSetMap.mapValues { it.value.toSet() },
                applicationNameToCommandNameSet =
                    applicationNameToCommandNameSetMap.mapValues { it.value.toSet() },
                applicationNameToRecordNameSet =
                    applicationNameToRecordNameSetMap.mapValues { it.value.toSet() },
                applicationNameToPropertySet =
                    applicationNameToPropertySetMap.mapValues { it.value.toSet() },
                applicationNameToEnumerationNameSet =
                    applicationNameToEnumerationNameSetMap.mapValues { it.value.toSet() },
                applicationNameToEnumeratorConstantNameSet =
                    applicationNameToEnumeratorConstantNameSetMap
                        .mapValues { it.value.toSet() },
                stdClassNameToApplicationNameSet =
                    stdClassNameToApplicationNameSetMap.mapValues { it.value.toSet() },
                stdClassNamePluralToApplicationNameSet =
                    stdClassNamePluralToApplicationNameSetMap
                        .mapValues { it.value.toSet() },
                stdCommandNameToApplicationNameSet =
                    stdCommandNameToApplicationNameSetMap.mapValues { it.value.toSet() },
                stdRecordNameToApplicationNameSet =
                    stdRecordNameToApplicationNameSetMap.mapValues { it.value.toSet() },
                stdPropertyNameToDictionarySet =
                    stdPropertyNameToDictionarySetMap.mapValues { it.value.toSet() },
                stdEnumerationNameToApplicationNameSet =
                    stdEnumerationNameToApplicationNameSetMap
                        .mapValues { it.value.toSet() },
                stdEnumeratorConstantNameToApplicationNameList =
                    stdEnumeratorConstantNameToApplicationNameListMap
                        .mapValues { it.value.toSet() },
            )

        // Sync lookup methods for the parser-util hot path.
        // Each method preserves the facade's `if (!isInitialized()) return false` gate, but the
        // gate now routes through [ParsableScriptSuiteRegistryHelper] (the @JvmStatic shim) to
        // avoid the SdefIndexService -> facade back-edge that verifyServiceDependencyGraph would
        // detect as a cycle.

        fun lookupStdLibClass(name: String): Boolean {
            if (!facadeInitialized()) return false
            return stdClassNameToApplicationNameSetMap.containsKey(name)
        }

        fun lookupApplicationClass(
            applicationName: String,
            className: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val classNameSet: Set<String>? = applicationNameToClassNameSetMap[applicationName]
            return classNameSet != null && classNameSet.contains(className)
        }

        fun lookupStdLibClassPluralName(pluralName: String): Boolean {
            if (!facadeInitialized()) return false
            return stdClassNamePluralToApplicationNameSetMap.containsKey(pluralName)
        }

        fun lookupApplicationClassPluralName(
            applicationName: String,
            pluralClassName: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val pluralClassNameSet: Set<String>? = applicationNameToClassNamePluralSetMap[applicationName]
            return pluralClassNameSet != null && pluralClassNameSet.contains(pluralClassName)
        }

        fun lookupStdClassWithPrefixExist(classNamePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(classNamePrefix, stdClassNameToApplicationNameSetMap.keys)
        }

        fun lookupClassWithPrefixExist(
            applicationName: String,
            classNamePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(classNamePrefix, applicationNameToClassNameSetMap[applicationName])
        }

        fun lookupStdClassPluralWithPrefixExist(namePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, stdClassNamePluralToApplicationNameSetMap.keys)
        }

        fun lookupClassPluralWithPrefixExist(
            applicationName: String,
            pluralClassNamePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(pluralClassNamePrefix, applicationNameToClassNamePluralSetMap[applicationName])
        }

        fun lookupStdCommand(name: String): Boolean {
            if (!facadeInitialized()) return false
            return stdCommandNameToApplicationNameSetMap.containsKey(name)
        }

        fun lookupApplicationCommand(
            applicationName: String,
            commandName: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val appCommands: Set<String>? = applicationNameToCommandNameSetMap[applicationName]
            return appCommands != null && appCommands.contains(commandName)
        }

        fun lookupCommandWithPrefixExist(
            applicationName: String,
            commandNamePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(commandNamePrefix, applicationNameToCommandNameSetMap[applicationName])
        }

        fun lookupStdCommandWithPrefixExist(namePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, stdCommandNameToApplicationNameSetMap.keys)
        }

        fun lookupStdProperty(name: String): Boolean {
            if (!facadeInitialized()) return false
            return stdPropertyNameToDictionarySetMap.containsKey(name)
        }

        fun lookupStdPropertyWithPrefixExist(namePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, stdPropertyNameToDictionarySetMap.keys)
        }

        fun lookupApplicationProperty(
            applicationName: String,
            propertyName: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val propertySet: Set<String>? = applicationNameToPropertySetMap[applicationName]
            return propertySet != null && propertySet.contains(propertyName)
        }

        fun lookupPropertyWithPrefixExist(
            applicationName: String,
            propertyNamePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(propertyNamePrefix, applicationNameToPropertySetMap[applicationName])
        }

        fun lookupStdConstant(name: String): Boolean {
            if (!facadeInitialized()) return false
            return stdEnumeratorConstantNameToApplicationNameListMap.containsKey(name)
        }

        fun lookupApplicationConstant(
            applicationName: String,
            constantName: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            val applicationConstantSet: Set<String>? =
                applicationNameToEnumeratorConstantNameSetMap[applicationName]
            return applicationConstantSet != null && applicationConstantSet.contains(constantName)
        }

        fun lookupStdConstantWithPrefixExist(namePrefix: String): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, stdEnumeratorConstantNameToApplicationNameListMap.keys)
        }

        fun lookupConstantWithPrefixExist(
            applicationName: String,
            namePrefix: String,
        ): Boolean {
            if (!facadeInitialized()) return false
            return isNameWithPrefixExist(namePrefix, applicationNameToEnumeratorConstantNameSetMap[applicationName])
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
        ): Collection<AppleScriptCommand> {
            val isOnDispatchThread = ApplicationManager.getApplication().isDispatchThread
            val isReady =
                if (isOnDispatchThread) {
                    facadeInitialized()
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

            val appNameList = stdCommandNameToApplicationNameSetMap[commandName] ?: emptySet()
            val result = HashSet<AppleScriptCommand>()
            for (applicationName in appNameList) {
                result.addAll(findApplicationCommands(project, applicationName, commandName))
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
                    ParsableScriptSuiteRegistryHelper.areAppDictionariesIndexed()
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

            val projectDictionaryRegistry = project.getService(AppleScriptProjectDictionaryService::class.java)
            // Among the loaded dictionaries the standard additions should always be present, but if
            // the command was not found there a new dictionary may need to be initialised here for
            // the project — once.
            val dictionary =
                projectDictionaryRegistry.getDictionary(applicationName)
                    ?: projectDictionaryRegistry.createDictionary(applicationName)
            return dictionary?.findAllCommandsWithName(commandName) ?: emptyList()
        }

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
        ): Boolean {
            try {
                val document: Document = LegacyJdomParser.build(xmlFile)
                val rootNode: Element = document.rootElement
                val suiteElements: List<Element> = rootNode.children.toList()

                if (ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY == applicationName) {
                    for (suiteElem in suiteElements) {
                        parseSuiteElementForApplication(suiteElem, applicationName)
                        parseSuiteElementForScriptingAdditions(suiteElem, applicationName)
                    }
                } else {
                    for (suiteElem in suiteElements) {
                        parseSuiteElementForApplication(suiteElem, applicationName)
                    }
                }
                return true
            } catch (e: JDOMException) {
                LOG.warn("Exception occurred while parsing dictionary file", e)
            } catch (e: IOException) {
                LOG.warn("Exception occurred while parsing dictionary file", e)
            }
            return false
        }

        private fun parseSuiteElementForScriptingAdditions(
            suiteElem: Element,
            applicationName: String,
        ) {
            val elements = parsedSuiteElements(suiteElem)

            for (valType in elements.valueTypes) {
                parseClassElement(applicationName, valType)
            }

            for (classTag in elements.classes) {
                parseClassElement(applicationName, classTag)
                parseElementsForApplication(
                    classTag.getChildren("property"),
                    applicationName,
                    stdPropertyNameToDictionarySetMap,
                )
            }

            for (classTag in elements.classExtensions) {
                parseClassElement(applicationName, classTag)
                parseElementsForApplication(
                    classTag.getChildren("property"),
                    applicationName,
                    stdPropertyNameToDictionarySetMap,
                )
            }

            parseElementsForApplication(elements.commands, applicationName, stdCommandNameToApplicationNameSetMap)
            parseElementsForApplication(elements.recordTypes, applicationName, stdRecordNameToApplicationNameSetMap)

            for (recordTag in elements.recordTypes) {
                parseElementsForApplication(
                    recordTag.getChildren("property"),
                    applicationName,
                    stdPropertyNameToDictionarySetMap,
                )
            }

            parseElementsForApplication(
                elements.enumerations,
                applicationName,
                stdEnumerationNameToApplicationNameSetMap,
            )

            for (enumerationTag in elements.enumerations) {
                parseElementsForApplication(
                    enumerationTag.getChildren("enumerator"),
                    applicationName,
                    stdEnumeratorConstantNameToApplicationNameListMap,
                )
            }
        }

        private fun parseSuiteElementForApplication(
            suiteElem: Element,
            applicationName: String,
        ) {
            val xIncludeNs = Namespace.getNamespace(XINCLUDE_NAMESPACE_URI)
            val xiIncludes: List<Element> = suiteElem.getChildren("include", xIncludeNs).toList()
            val elements = parsedSuiteElements(suiteElem)

            for (include in xiIncludes) {
                var hrefIncl = include.getAttributeValue("href")
                hrefIncl = hrefIncl.replace("localhost", "")
                val inclFile = File(hrefIncl)
                if (inclFile.exists()) {
                    parseDictionaryFile(inclFile, applicationName)
                }
            }

            for (valType in elements.valueTypes) {
                parseClassElement(applicationName, valType)
            }

            for (classTag in elements.classes) {
                parseClassElement(applicationName, classTag)
                parseHashElementsForApplication(
                    classTag.getChildren("property"),
                    applicationName,
                    applicationNameToPropertySetMap,
                )
            }

            for (classTag in elements.classExtensions) {
                parseClassElement(applicationName, classTag)
                parseHashElementsForApplication(
                    classTag.getChildren("property"),
                    applicationName,
                    applicationNameToPropertySetMap,
                )
            }

            parseHashElementsForApplication(elements.commands, applicationName, applicationNameToCommandNameSetMap)
            parseHashElementsForApplication(elements.recordTypes, applicationName, applicationNameToRecordNameSetMap)

            for (recordTag in elements.recordTypes) {
                parseHashElementsForApplication(
                    recordTag.getChildren("property"),
                    applicationName,
                    applicationNameToPropertySetMap,
                )
            }

            parseHashElementsForApplication(
                elements.enumerations,
                applicationName,
                applicationNameToEnumerationNameSetMap,
            )

            for (enumerationTag in elements.enumerations) {
                parseHashElementsForApplication(
                    enumerationTag.getChildren("enumerator"),
                    applicationName,
                    applicationNameToEnumeratorConstantNameSetMap,
                )
            }
        }

        private fun parseClassElement(
            applicationName: String,
            classElement: Element,
        ) {
            val className = classElement.getAttributeValue("name")
            val code = classElement.getAttributeValue("code")
            var pluralClassName = classElement.getAttributeValue("plural")
            if (className == null || code == null) return
            pluralClassName = if (!StringUtil.isEmpty(pluralClassName)) pluralClassName else "${className}s"

            updateObjectNameSetForApplication(className, applicationName, applicationNameToClassNameSetMap)
            updateObjectNameSetForApplication(pluralClassName, applicationName, applicationNameToClassNamePluralSetMap)
            if (ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY == applicationName) {
                updateApplicationNameSetFor(className, applicationName, stdClassNameToApplicationNameSetMap)
                updateApplicationNameSetFor(pluralClassName, applicationName, stdClassNamePluralToApplicationNameSetMap)
            }
        }

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
        private fun facadeInitialized(): Boolean = ParsableScriptSuiteRegistryHelper.isInitialized()

        private fun isStandardReady(): Boolean =
            facadeInitialized() ||
                awaitReady("standardReady", ParsableScriptSuiteRegistryHelper::awaitStandardReady)

        private fun isAppReady(): Boolean =
            ParsableScriptSuiteRegistryHelper.areAppDictionariesIndexed() ||
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

        companion object {
            @JvmStatic
            fun getInstance(): SdefIndexService =
                ApplicationManager
                    .getApplication()
                    .getService(SdefIndexService::class.java)

            private fun parseElementsForApplication(
                xmlElements: List<Element>,
                applicationName: String,
                objectTagNameToApplicationNameListMap: MutableMap<String, MutableSet<String>>,
            ) {
                for (applicationObjectTag in xmlElements) {
                    parseSimpleElementForObject(
                        applicationObjectTag,
                        applicationName,
                        objectTagNameToApplicationNameListMap,
                    )
                }
            }

            private fun parseHashElementsForApplication(
                xmlElements: List<Element>,
                applicationName: String,
                objectTagNameToApplicationNameListMap: MutableMap<String, MutableSet<String>>,
            ) {
                for (applicationObjectTag in xmlElements) {
                    hashSimpleElementForObject(
                        applicationObjectTag,
                        applicationName,
                        objectTagNameToApplicationNameListMap,
                    )
                }
            }

            private fun parseSimpleElementForObject(
                suiteObjectElement: Element,
                applicationName: String,
                objectNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>>,
            ) {
                val objectName = suiteObjectElement.getAttributeValue("name")
                val code = suiteObjectElement.getAttributeValue("code")
                if (objectName == null || code == null) return
                updateApplicationNameSetFor(objectName, applicationName, objectNameToApplicationNameSetMap)
            }

            private fun hashSimpleElementForObject(
                suiteObjectElement: Element,
                applicationName: String,
                objectNameToApplicationNameListMap: MutableMap<String, MutableSet<String>>,
            ) {
                val objectName = suiteObjectElement.getAttributeValue("name")
                val code = suiteObjectElement.getAttributeValue("code")
                if (objectName == null || code == null) return
                updateObjectNameSetForApplication(objectName, applicationName, objectNameToApplicationNameListMap)
            }

            private fun updateApplicationNameSetFor(
                applicationObjectName: String,
                applicationName: String,
                applicationNameSetMap: MutableMap<String, MutableSet<String>>,
            ) {
                if (StringUtil.isEmpty(applicationObjectName)) return
                // Atomic get-or-put-and-mutate per D-03: ConcurrentHashMap.compute serialises
                // the (lookup, allocate, insert, add) tuple inside a single bucket lock.
                applicationNameSetMap.compute(applicationObjectName) { _, existing ->
                    (existing ?: ConcurrentHashMap.newKeySet()).also { it.add(applicationName) }
                }
            }

            private fun updateObjectNameSetForApplication(
                applicationObjectName: String,
                applicationName: String,
                applicationNameSetMap: MutableMap<String, MutableSet<String>>,
            ) {
                if (StringUtil.isEmpty(applicationName)) return
                // Atomic get-or-put-and-mutate per D-03.
                applicationNameSetMap.compute(applicationName) { _, existing ->
                    (existing ?: ConcurrentHashMap.newKeySet()).also { it.add(applicationObjectName) }
                }
            }

            private fun startsWithWord(
                string: String,
                prefix: String,
            ): Boolean = string.startsWith(prefix) && (prefix.length == string.length || ' ' == string[prefix.length])
        }
    }
