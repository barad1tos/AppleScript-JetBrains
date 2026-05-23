package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.plugin.applescript.lang.parser.ParsableScriptHelper
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.extensionSupported
import com.intellij.plugin.applescript.lang.util.MyStopVisitingException
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.CollectionBean
import com.intellij.util.xmlb.annotations.Tag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jdom.Document
import org.jdom.Element
import org.jdom.JDOMException
import org.jdom.Namespace
import org.jdom.input.SAXBuilder
import org.jdom.output.XMLOutputter
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.script.ScriptException
import javax.script.ScriptEngineManager

@Service(Service.Level.APP)
@State(
    name = AppleScriptSystemDictionaryRegistryService.COMPONENT_NAME,
    storages = [Storage(value = "appleScriptCachedDictionariesInfo.xml", roamingType = RoamingType.PER_OS)],
)
class AppleScriptSystemDictionaryRegistryService(
    // serviceScope is exposed `internal` so ServiceScopeLifecycleIntegrationTest can read its Job
    // tree. Same-module test code naturally accesses `internal` members — no @VisibleForTesting
    // needed on constructor parameters (annotation does not target value parameters in Kotlin).
    internal val serviceScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SimplePersistentStateComponent<AppleScriptSystemDictionaryRegistryService.PersistedState>(PersistedState()),
    ParsableScriptHelper {

    // persisted data
    private val dictionaryInfoMap: MutableMap<String, DictionaryInfo> = ConcurrentHashMap()
    private val notScriptableApplicationList: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // scripting additions installed in the system
    private val scriptingAdditions: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val notFoundApplicationList: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val discoveredApplicationNames: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private var xCodeApplicationFile: File? = null

    private val applicationNameToClassNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val applicationNameToClassNamePluralSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val applicationNameToCommandNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val applicationNameToRecordNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val applicationNameToPropertySetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val applicationNameToEnumerationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val applicationNameToEnumeratorConstantNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val stdClassNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val stdClassNamePluralToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val stdCommandNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val stdRecordNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val stdPropertyNameToDictionarySetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val stdEnumerationNameToApplicationNameSetMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val stdEnumeratorConstantNameToApplicationNameListMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()

    /**
     * Two-stage gating primitives replacing the Phase 1 [java.util.concurrent.CountDownLatch] (D-01, D-04).
     *
     * Typed as `CompletableDeferred<Result<Unit>>` per Codex HIGH 1 so failed init is communicated via
     * `complete(Result.failure(...))` (NOT `completeExceptionally(...)`). `isCompleted` alone is NOT
     * a success signal — readers must additionally check `getCompleted().isSuccess`. The two facades
     * [isInitialized] and [areAppDictionariesIndexed] encapsulate this success-semantic predicate.
     *
     * Exposed `@VisibleForTesting internal` so AppCommandGatingTest + DeferredFailureSemanticsTest +
     * ServiceScopeLifecycleIntegrationTest can drive / inspect them deterministically.
     */
    @VisibleForTesting
    internal val standardReady: CompletableDeferred<Result<Unit>> = CompletableDeferred()

    @VisibleForTesting
    internal val appsReady: CompletableDeferred<Result<Unit>> = CompletableDeferred()

    init {
        // Constructor returns immediately (COROUTINE-05 non-blocking-init invariant). The launch is
        // fire-and-forget; structured concurrency guarantees cancellation on plugin unload / app
        // shutdown via the injected [serviceScope] (RESEARCH §3 verified). `ioDispatcher` is injected
        // (Codex HIGH 2) so tests pass `StandardTestDispatcher` for deterministic runCurrent /
        // advanceUntilIdle control of init progression.
        serviceScope.launch(ioDispatcher) {
            runInitChain()
        }
    }

    /**
     * Two-stage init pipeline run inside [serviceScope] on [ioDispatcher].
     *
     * Order is load-bearing for the cold-start state machine (CoroutineColdStartTest Pattern L lock):
     *   1. `registerSdefExtension()` under `withContext(Dispatchers.EDT)` — write-action requires EDT
     *      (RECURRING_PITFALLS.md Pattern C — NEVER `Dispatchers.Main`).
     *   2. `initDictionariesInfoFromCache(state)` — restore persisted dictionary entries.
     *   3. `initStandardSuite()` — parse StandardAdditions + CocoaStandard.
     *   4. Complete `standardReady` with `Result.success(Unit)` — parser fast path unblocks.
     *   5. `discoverInstalledApplicationNames()` — walk the `/Applications` directory tree.
     *   6. Complete `appsReady` with `Result.success(Unit)` — completion/annotator paths unblock.
     *
     * Exception handling (RECURRING_PITFALLS.md Pattern B compliance):
     *   - Catch [CancellationException] FIRST and re-throw — never swallow structured cancellation.
     *   - Catch [Throwable] (not [Exception]) and `LOG.error` — captures `Error` subclasses too.
     *   - `finally` block completes any not-yet-completed deferred with `Result.failure(...)` so the
     *     facades see `isCompleted && isFailure` → return `false` (not-ready) rather than a
     *     false-positive "ready" for a failed init (Codex HIGH 1, Pattern G).
     */
    private suspend fun runInitChain() {
        try {
            withContext(Dispatchers.EDT) { registerSdefExtension() }
            initDictionariesInfoFromCache(state)
            initStandardSuite()
            standardReady.complete(Result.success(Unit))
            discoverInstalledApplicationNames()
            appsReady.complete(Result.success(Unit))
        } catch (e: CancellationException) {
            // Pattern B: structured cancellation re-thrown to honour the coroutine contract.
            throw e
        } catch (t: Throwable) {
            // Pattern B: Throwable (not Exception) — captures Error subclasses too.
            LOG.error("Error while initializing service", t)
        } finally {
            // Codex HIGH 1: complete with Result.failure so facades see isCompleted && isFailure
            // → return false. NOT `completeExceptionally` (which would make `await()` throw at
            // callers and lose the success-vs-failure distinction at the facade boundary).
            if (!standardReady.isCompleted) {
                standardReady.complete(
                    Result.failure(IllegalStateException("standardReady init failed")),
                )
            }
            if (!appsReady.isCompleted) {
                appsReady.complete(
                    Result.failure(IllegalStateException("appsReady init failed")),
                )
            }
        }
    }

    /**
     * Returns `true` only when the standard SDEF suite (StandardAdditions + CocoaStandard) has been
     * parsed AND indexed successfully. A completed-but-failed [standardReady] (init threw before the
     * `Result.success(Unit)` line) returns `false` — readers see "not ready" rather than a
     * false-positive "ready" for a failed init (Codex HIGH 1, RECURRING_PITFALLS.md Pattern G).
     *
     * Distinct from [areAppDictionariesIndexed]: this facade reflects the parser fast path readiness
     * (standard-library suite only), while [areAppDictionariesIndexed] reflects the full
     * `/Applications` discovery sweep (Gemini LOW 3).
     *
     * @return `true` if [standardReady] completed successfully; `false` if pending OR failed.
     */
    fun isInitialized(): Boolean =
        standardReady.isCompleted && standardReady.getCompleted().isSuccess

    /**
     * Returns `true` only when the full application catalog discovery has completed successfully.
     * Completion contributors and the annotator gate on this facade. A failed [appsReady] returns
     * `false` — readers see "not ready" rather than a false-positive "ready" for a failed
     * app-discovery sweep (Codex HIGH 1, RECURRING_PITFALLS.md Pattern G).
     *
     * Distinct from [isInitialized]: this facade reflects the full app-discovery pipeline, while
     * [isInitialized] reflects only the standard-library readiness (Gemini LOW 3).
     *
     * @return `true` if [appsReady] completed successfully; `false` if pending OR failed.
     */
    fun areAppDictionariesIndexed(): Boolean =
        appsReady.isCompleted && appsReady.getCompleted().isSuccess

    private fun registerSdefExtension() {
        ApplicationManager.getApplication().runWriteAction(
            Runnable {
                val fileType: FileType? = FileTypeManager.getInstance().getFileTypeByExtension("xml")
                if (fileType != null) {
                    FileTypeManager.getInstance().associateExtension(fileType, "sdef")
                }
            },
        )
    }

    private fun removeDictionaryInfo(applicationName: String) {
        dictionaryInfoMap.remove(applicationName)
        notScriptableApplicationList.add(applicationName)
    }

    private fun addDictionaryInfo(info: DictionaryInfo) {
        dictionaryInfoMap[info.getApplicationName()] = info
        discoveredApplicationNames.add(info.getApplicationName())
        notScriptableApplicationList.remove(info.getApplicationName())
    }

    internal fun getDictionaryInfoList(): Collection<DictionaryInfo> = dictionaryInfoMap.values

    // Defensive snapshot: backing storage is concurrent; callers historically did not mutate this.
    fun getNotScriptableApplicationList(): HashSet<String> = HashSet(notScriptableApplicationList)

    // Defensive snapshot: backing storage is concurrent; callers historically did not mutate this.
    // TODO(v1.1 SDEF-05): once DictionaryIndexes lands, narrow the interface to a read-only Set.
    override fun getScriptingAdditions(): HashSet<String> = HashSet(scriptingAdditions)

    override fun loadState(state: PersistedState) {
        super.loadState(state)
        try {
            initDictionariesInfoFromCache(state)
        } catch (e: Exception) {
            LOG.error("Error while loading state for AppleScript dictionaries", e)
        }
    }

    fun updateState() {
        val state = state
        val dictionaryInfos = dictionaryInfoMap.values
        state.dictionariesInfo = Array(dictionaryInfos.size) { DictionaryInfo.State() }
        val iterator = dictionaryInfos.iterator()
        for (i in state.dictionariesInfo.indices) {
            state.dictionariesInfo[i] = iterator.next().getState()
        }
        if (state.notScriptableApplications == null) {
            state.notScriptableApplications = ArrayList()
        } else {
            state.notScriptableApplications!!.clear()
        }
        state.notScriptableApplications!!.addAll(notScriptableApplicationList)
    }

    /** Fills [dictionaryInfoMap] from previously persisted [PersistedState]. */
    private fun initDictionariesInfoFromCache(state: PersistedState) {
        notScriptableApplicationList.clear()
        state.notScriptableApplications?.let { notScriptableApplicationList.addAll(it) }
        val infos = state.dictionariesInfo
        for (dInfoState in infos) {
            val appName = dInfoState.applicationName
            val dictionaryUrl = dInfoState.dictionaryUrl
            val applicationUrl = dInfoState.applicationUrl
            if (!StringUtil.isEmptyOrSpaces(appName) && !StringUtil.isEmptyOrSpaces(dictionaryUrl)) {
                val dictionaryFile = if (!StringUtil.isEmpty(dictionaryUrl)) File(dictionaryUrl!!) else null
                val applicationFile = if (!StringUtil.isEmpty(applicationUrl)) File(applicationUrl!!) else null
                if (dictionaryFile != null && dictionaryFile.exists()) {
                    dictionaryInfoMap.remove(appName)
                    addDictionaryInfo(DictionaryInfo(appName!!, dictionaryFile, applicationFile))
                }
            }
        }
    }

    private fun discoverInstalledApplicationNames() {
        for (applicationsDirectory in ApplicationDictionary.APP_BUNDLE_DIRECTORIES) {
            val appsDirVFile = LocalFileSystem.getInstance().findFileByPath(applicationsDirectory)
            if (appsDirVFile != null && appsDirVFile.exists()) {
                discoverApplicationsInDirectory(appsDirVFile)
            }
        }
        LOG.info("List of installed applications initialized. Count: ${discoveredApplicationNames.size}")
    }

    private fun discoverApplicationsInDirectory(appsDirVFile: VirtualFile) {
        VfsUtilCore.visitChildrenRecursively(
            appsDirVFile,
            object : VirtualFileVisitor<VirtualFile>(VirtualFileVisitor.limit(APP_DEPTH_SEARCH)) {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (extensionSupported(file.extension)) {
                        if ("xml" != file.extension) {
                            discoveredApplicationNames.add(file.nameWithoutExtension)
                        }
                        return false
                    }
                    return file.isDirectory
                }
            },
        )
    }

    /**
     * Called from the annotator to ensure that an application's dictionary is initialised.
     *
     * @return true if the dictionary was initialised
     */
    fun ensureDictionaryInitialized(anyApplicationName: String): Boolean =
        ensureKnownApplicationDictionaryInitialized(anyApplicationName) ||
            !StringUtil.isEmptyOrSpaces(anyApplicationName) && !isNotScriptable(anyApplicationName) &&
            !isInUnknownList(anyApplicationName) && getInitializedInfo(anyApplicationName) != null

    override fun ensureKnownApplicationDictionaryInitialized(knownApplicationName: String): Boolean {
        // D-04: app-name resolver path — gated on full app-discovery sweep (appsReady).
        if (!areAppDictionariesIndexed()) return false
        if (discoveredApplicationNames.contains(knownApplicationName)) {
            val dInfo = dictionaryInfoMap[knownApplicationName]
            return dInfo != null && (dInfo.isInitialized() || initializeDictionaryFromInfo(dInfo)) ||
                getInitializedInfo(knownApplicationName) != null
        }
        return false
    }

    // ParsableScriptHelper methods

    override fun isStdLibClass(name: String): Boolean {
        if (!isInitialized()) return false
        return stdClassNameToApplicationNameSetMap.containsKey(name)
    }

    override fun isApplicationClass(applicationName: String, className: String): Boolean {
        if (!isInitialized()) return false
        val classNameSet = applicationNameToClassNameSetMap[applicationName]
        return classNameSet != null && classNameSet.contains(className)
    }

    override fun isStdLibClassPluralName(pluralName: String): Boolean {
        if (!isInitialized()) return false
        return stdClassNamePluralToApplicationNameSetMap.containsKey(pluralName)
    }

    override fun isApplicationClassPluralName(applicationName: String, pluralClassName: String): Boolean {
        if (!isInitialized()) return false
        val pluralClassNameSet = applicationNameToClassNamePluralSetMap[applicationName]
        return pluralClassNameSet != null && pluralClassNameSet.contains(pluralClassName)
    }

    override fun isStdClassWithPrefixExist(classNamePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(classNamePrefix, stdClassNameToApplicationNameSetMap.keys)
    }

    override fun isClassWithPrefixExist(applicationName: String, classNamePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(classNamePrefix, applicationNameToClassNameSetMap[applicationName])
    }

    override fun isStdClassPluralWithPrefixExist(namePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(namePrefix, stdClassNamePluralToApplicationNameSetMap.keys)
    }

    override fun isClassPluralWithPrefixExist(applicationName: String, pluralClassNamePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(pluralClassNamePrefix, applicationNameToClassNamePluralSetMap[applicationName])
    }

    private fun isNameWithPrefixExist(namePrefix: String, nameSet: Set<String>?): Boolean {
        if (nameSet == null) return false
        for (objectName in nameSet) {
            if (startsWithWord(objectName, namePrefix)) return true
        }
        return false
    }

    override fun isStdCommand(name: String): Boolean {
        if (!isInitialized()) return false
        return stdCommandNameToApplicationNameSetMap.containsKey(name)
    }

    override fun isApplicationCommand(applicationName: String, commandName: String): Boolean {
        if (!isInitialized()) return false
        val appCommands = applicationNameToCommandNameSetMap[applicationName]
        return appCommands != null && appCommands.contains(commandName)
    }

    override fun isCommandWithPrefixExist(applicationName: String, commandNamePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(commandNamePrefix, applicationNameToCommandNameSetMap[applicationName])
    }

    override fun isStdCommandWithPrefixExist(namePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(namePrefix, stdCommandNameToApplicationNameSetMap.keys)
    }

    /**
     * Bounded-wait resolver for standard-suite commands.
     *
     * Codex MEDIUM 1 + Gemini LOW 1: EDT guard at entry — never blocks the UI thread.
     * Codex HIGH 5: split-gate — waits on [standardReady] (parser fast path readiness only).
     * Codex HIGH 1: timeout returns `null`, failure returns `isFailure` → both yield empty list.
     * Gemini LOW 2: `LOG.warn` records the 2s timeout site for slow-init diagnostics.
     */
    override fun findStdCommands(project: Project, commandName: String): Collection<AppleScriptCommand> {
        if (ApplicationManager.getApplication().isDispatchThread) {
            LOG.warn("findStdCommands called from EDT; returning empty list to avoid 2s freeze")
            return emptyList()
        }
        if (!isInitialized()) {
            val gate = runBlockingCancellable {
                withTimeoutOrNull(2_000) { standardReady.await() }
            }
            if (gate == null) {
                LOG.warn("findStdCommands: 2s timeout waiting on standardReady — returning emptyList")
                return emptyList()
            }
            if (gate.isFailure) {
                // Codex HIGH 1: init failed — treat as not-ready.
                return emptyList()
            }
        }
        val appNameList = stdCommandNameToApplicationNameSetMap[commandName] ?: return HashSet(0)
        val result = HashSet<AppleScriptCommand>()
        for (applicationName in appNameList) {
            result.addAll(findApplicationCommands(project, applicationName, commandName))
        }
        return result
    }

    /**
     * Bounded-wait resolver for app-scoped commands.
     *
     * Codex MEDIUM 1 + Gemini LOW 1: EDT guard at entry — never blocks the UI thread.
     * Codex HIGH 5: split-gate — waits on [appsReady] (NOT [standardReady]). App-scoped command
     * lookup must not proceed before the full `/Applications` discovery sweep completes, otherwise
     * the reader sees an empty/partial app catalog. AppCommandGatingTest locks this invariant.
     * Codex HIGH 1: timeout returns `null`, failure returns `isFailure` → both yield empty list.
     * Gemini LOW 2: `LOG.warn` records the 2s timeout site for slow-init diagnostics.
     */
    override fun findApplicationCommands(
        project: Project,
        applicationName: String,
        commandName: String,
    ): List<AppleScriptCommand> {
        if (ApplicationManager.getApplication().isDispatchThread) {
            LOG.warn("findApplicationCommands called from EDT; returning empty list to avoid 2s freeze")
            return emptyList()
        }
        if (!areAppDictionariesIndexed()) {
            val gate = runBlockingCancellable {
                withTimeoutOrNull(2_000) { appsReady.await() }
            }
            if (gate == null) {
                LOG.warn("findApplicationCommands: 2s timeout waiting on appsReady — returning emptyList")
                return emptyList()
            }
            if (gate.isFailure) {
                return emptyList()
            }
        }
        val projectDictionaryRegistry = project.getService(AppleScriptProjectDictionaryService::class.java)
        // Among the loaded dictionaries the standard additions should always be present, but if the command
        // was not found there a new dictionary may need to be initialised here for the project — once.
        val dictionary = projectDictionaryRegistry.getDictionary(applicationName)
            ?: projectDictionaryRegistry.createDictionary(applicationName)
        if (dictionary != null) {
            return dictionary.findAllCommandsWithName(commandName)
        }
        return ArrayList(0)
    }

    override fun isStdProperty(name: String): Boolean {
        if (!isInitialized()) return false
        return stdPropertyNameToDictionarySetMap.containsKey(name)
    }

    override fun isStdPropertyWithPrefixExist(namePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(namePrefix, stdPropertyNameToDictionarySetMap.keys)
    }

    override fun isApplicationProperty(applicationName: String, propertyName: String): Boolean {
        if (!isInitialized()) return false
        val propertySet = applicationNameToPropertySetMap[applicationName]
        return propertySet != null && propertySet.contains(propertyName)
    }

    override fun isPropertyWithPrefixExist(applicationName: String, propertyNamePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(propertyNamePrefix, applicationNameToPropertySetMap[applicationName])
    }

    override fun isStdConstant(name: String): Boolean {
        if (!isInitialized()) return false
        return stdEnumeratorConstantNameToApplicationNameListMap.containsKey(name)
    }

    override fun isApplicationConstant(applicationName: String, constantName: String): Boolean {
        if (!isInitialized()) return false
        val applicationConstantSet = applicationNameToEnumeratorConstantNameSetMap[applicationName]
        return applicationConstantSet != null && applicationConstantSet.contains(constantName)
    }

    override fun isStdConstantWithPrefixExist(namePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(namePrefix, stdEnumeratorConstantNameToApplicationNameListMap.keys)
    }

    override fun isConstantWithPrefixExist(applicationName: String, namePrefix: String): Boolean {
        if (!isInitialized()) return false
        return isNameWithPrefixExist(namePrefix, applicationNameToEnumeratorConstantNameSetMap[applicationName])
    }

    /** Initialise from cached, previously generated files. */
    @Suppress("unused")
    private fun initDictionariesFromCachedFiles() {
        for (dictionaryInfo in dictionaryInfoMap.values) {
            if (!initializeDictionaryFromInfo(dictionaryInfo)) {
                LOG.warn(
                    "Failed to initialize dictionary for application: ${dictionaryInfo.getApplicationName()}" +
                        " from generated file ${dictionaryInfo.getDictionaryFile()}",
                )
            } else {
                LOG.warn(
                    "Failed to initialize dictionary for application: ${dictionaryInfo.getApplicationName()}" +
                        "Dictionary file ${dictionaryInfo.getDictionaryFile()} is not valid",
                )
            }
        }
    }

    /**
     * Initialises dictionary information for [applicationName] either from a previously generated cached
     * dictionary file or by generating one. Standard folders are searched for the application's location.
     *
     * @return the [DictionaryInfo] of the generated and cached dictionary for the application, or null
     */
    fun getInitializedInfo(applicationName: String): DictionaryInfo? {
        if (StringUtil.isEmptyOrSpaces(applicationName) || isNotScriptable(applicationName) ||
            isInUnknownList(applicationName)
        ) {
            return null
        }

        val savedDictionaryInfo = getDictionaryInfo(applicationName)
        if (savedDictionaryInfo != null &&
            (savedDictionaryInfo.isInitialized() || initializeDictionaryFromInfo(savedDictionaryInfo))
        ) {
            return savedDictionaryInfo
        }
        val appFile = findApplicationBundleFile(applicationName)
        if (appFile != null) {
            return createAndInitializeInfo(appFile, applicationName)
        }
        return null
    }

    private fun findApplicationBundleFile(applicationName: String): File? {
        if (!SystemInfo.isMac) {
            notFoundApplicationList.add(applicationName)
            return null
        }
        // Fast path: try standard locations first.
        for (applicationsDirectory in ApplicationDictionary.APP_BUNDLE_DIRECTORIES) {
            for (ext in ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS) {
                val appBundleFilePath = "$applicationsDirectory/$applicationName.$ext"
                val appFile = File(appBundleFilePath)
                if (appFile.exists() && appFile.isFile) return appFile
            }
        }
        // Fallback: recursive search.
        for (applicationsDirectory in ApplicationDictionary.APP_BUNDLE_DIRECTORIES) {
            val appDirectory = File(applicationsDirectory)
            if (appDirectory.exists() && appDirectory.isDirectory) {
                val appVDirectory = LocalFileSystem.getInstance().findFileByIoFile(appDirectory) ?: continue
                val appBundleFile = findApplicationFileRecursively(appVDirectory, applicationName)
                if (appBundleFile != null && appBundleFile.exists()) return appBundleFile
            }
        }
        LOG.warn(
            "No file was found for application: $applicationName in roots: " +
                "${Arrays.toString(ApplicationDictionary.APP_BUNDLE_DIRECTORIES)}" +
                " Adding application to unknown applications list.",
        )
        notFoundApplicationList.add(applicationName)
        return null
    }

    private fun findApplicationFileRecursively(appDirectory: VirtualFile, applicationName: String): File? {
        val fileVisitor = object : VirtualFileVisitor<VirtualFile>(
            VirtualFileVisitor.limit(APP_DEPTH_SEARCH),
            VirtualFileVisitor.SKIP_ROOT,
            VirtualFileVisitor.NO_FOLLOW_SYMLINKS,
        ) {
            override fun visitFile(file: VirtualFile): Boolean {
                if (ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS.contains(file.extension)) {
                    if (applicationName == file.nameWithoutExtension) {
                        throw MyStopVisitingException(file)
                    }
                    return false // do not search inside application bundles
                }
                return true
            }
        }
        return try {
            VfsUtilCore.visitChildrenRecursively(appDirectory, fileVisitor, MyStopVisitingException::class.java)
            null
        } catch (e: MyStopVisitingException) {
            LOG.debug("Application file found for application $applicationName : ${e.result}")
            File(e.result.path)
        }
    }

    /**
     * Initialises dictionary information for an application from its bundle file (or `.xml`/`.sdef` file)
     * and persists the generated dictionary for later use by the [ApplicationDictionary] PSI class.
     *
     * @param applicationIoFile file of the application bundle or dictionary file (.app, .osax, .xml, .sdef)
     * @param applicationName name of the macOS application
     * @return the [DictionaryInfo] of the generated, cached and initialised dictionary, or null
     */
    @Synchronized
    fun createAndInitializeInfo(applicationIoFile: File, applicationName: String): DictionaryInfo? {
        // Kotlin stdlib File.extension: suffix after the last '.' WITHOUT the dot,
        // matches the Guava Files.getFileExtension behaviour byte-for-byte.
        val appExtension: String = applicationIoFile.extension
        if (!extensionSupported(appExtension)) return null
        if (!applicationIoFile.exists()) return null
        if (getDictionaryInfo(applicationName) != null) {
            LOG.warn(
                "Dictionary for application $applicationName was already initialized. " +
                    "Generating new dictionary file any way.",
            )
        }
        val createdDictionaryInfo = createDictionaryInfoForApplication(applicationName, applicationIoFile)
        if (createdDictionaryInfo != null) {
            if (initializeDictionaryFromInfo(createdDictionaryInfo)) return createdDictionaryInfo
        }
        return null
    }

    /** @return true if dictionary terms were successfully initialised */
    private fun initializeDictionaryFromInfo(dictionaryInfo: DictionaryInfo): Boolean {
        val file = File(dictionaryInfo.getDictionaryFile().path)
        val applicationName = dictionaryInfo.getApplicationName()
        if (file.exists() && parseDictionaryFile(file, applicationName)) {
            return dictionaryInfo.setInitialized(true)
        }
        // Parsing failed — remove the broken generated dictionary file from the cache.
        LOG.warn("Initialization failed for application [$applicationName].")
        removeDictionaryInfo(applicationName)
        return false
    }

    /** Persistent state for the application-level service. Field names are XML attribute names. */
    class PersistedState : BaseState() {
        @JvmField
        @Tag("applicationsInfo")
        @AbstractCollection(surroundWithTag = false)
        var dictionariesInfo: Array<DictionaryInfo.State> = emptyArray()

        @JvmField
        @CollectionBean
        var notScriptableApplications: MutableList<String>? = ArrayList()
    }

    /** Initialise Standard Terminology and installed Scripting Addition libraries. */
    private fun initStandardSuite() {
        try {
            if (SystemInfo.isMac) {
                // Scripting additions.
                val di = getDictionaryInfo(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
                if (di == null) {
                    initializeScriptingAdditions()
                    mergeScriptingAdditions()
                } else {
                    initializeDictionaryFromInfo(di)
                }
                // Standard Cocoa terminology.
                val applicationName = ApplicationDictionary.COCOA_STANDARD_LIBRARY
                val dInfo = getInitializedInfo(applicationName)
                if (dInfo != null) {
                    initializeDictionaryFromInfo(dInfo)
                } else {
                    var stdLibFile = File(ApplicationDictionary.COCOA_STANDARD_LIBRARY_PATH)
                    if (!stdLibFile.exists() || !stdLibFile.isFile) {
                        val isStd: InputStream? = javaClass.getResourceAsStream(ApplicationDictionary.COCOA_STANDARD_FILE)
                        stdLibFile = stream2file(isStd, applicationName.replace(" ", "_"), ".sdef")
                    }
                    if (stdLibFile.exists() && stdLibFile.isFile) {
                        createAndInitializeInfo(stdLibFile, applicationName)
                    } else {
                        LOG.warn("Can not find standard suite dictionary in the classpath")
                    }
                }
            } else {
                initStdTerms(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
                initStdTerms(ApplicationDictionary.COCOA_STANDARD_LIBRARY)
            }
        } catch (e: IOException) {
            LOG.error("Failed to initialize dictionary for standard terms ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initializeScriptingAdditions() {
        for (stdLibFolder in ApplicationDictionary.SCRIPTING_ADDITIONS_FOLDERS) {
            val dir = File(stdLibFolder)
            if (!dir.isDirectory) continue
            val stdLibs = dir.listFiles() ?: continue
            if (stdLibs.isEmpty()) continue
            for (stdLib in stdLibs) {
                var libraryName = stdLib.name
                val last = libraryName.lastIndexOf(".")
                libraryName = libraryName.substring(0, if (last > 0) last else libraryName.length - 1)
                if (StringUtil.isEmpty(libraryName)) continue
                var dInfo = getDictionaryInfo(libraryName)
                if (dInfo != null) {
                    initializeDictionaryFromInfo(dInfo)
                } else if (stdLib.exists()) {
                    dInfo = createAndInitializeInfo(stdLib, libraryName)
                }
                if (dInfo != null) {
                    scriptingAdditions.add(dInfo.getApplicationName())
                } else {
                    LOG.warn(
                        "Can not initialize scripting addition library from file: $stdLib. Will copy bundled lib.",
                    )
                    try {
                        dInfo = initStdTerms(ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY)
                        if (dInfo != null) scriptingAdditions.add(dInfo.getApplicationName())
                    } catch (e: IOException) {
                        LOG.warn("Can not initialize scripting addition library from bundle: ${e.message}")
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun initStdTerms(stdLibName: String): DictionaryInfo? {
        var stdDInfo = dictionaryInfoMap[stdLibName]
        if (stdDInfo != null) {
            initializeDictionaryFromInfo(stdDInfo)
        } else {
            val libPathResource: String? = when (stdLibName) {
                ApplicationDictionary.COCOA_STANDARD_LIBRARY -> ApplicationDictionary.COCOA_STANDARD_FILE
                ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY -> ApplicationDictionary.STANDARD_ADDITIONS_FILE
                else -> null
            } ?: return null

            val isStd: InputStream? = javaClass.getResourceAsStream(libPathResource)
            val tmpFile = stream2file(isStd, stdLibName.replace(" ", "_"), ".sdef")
            if (tmpFile.exists() && tmpFile.isFile) {
                stdDInfo = createAndInitializeInfo(tmpFile, stdLibName)
            } else {
                LOG.warn("Can not find standard suite dictionary in the classpath")
            }
        }
        return stdDInfo
    }

    /** Create a single dictionary by merging all scripting additions installed on the system. */
    private fun mergeScriptingAdditions(): DictionaryInfo? {
        try {
            val dictionaryFiles = ArrayList<File>()
            val libName = ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY
            val mergedFile = File.createTempFile(libName, ".sdef")
            for (scriptingAddition in scriptingAdditions) {
                val di = getDictionaryInfo(scriptingAddition) ?: continue
                dictionaryFiles.add(File(di.getDictionaryFile().path))
            }
            val iterator = dictionaryFiles.iterator()
            if (!iterator.hasNext()) return null
            val first = iterator.next()
            val builder = newSecureSaxBuilder()
            val firstDocument: Document = builder.build(first)
            val firstRoot: Element = firstDocument.rootElement
            while (iterator.hasNext()) {
                val second = iterator.next()
                val secondDocument: Document = builder.build(second)
                val secondRoot: Element = secondDocument.rootElement
                val suiteElements: List<Element> = secondRoot.getChildren("suite")
                for (originalSuite in suiteElements) {
                    val suite = originalSuite.clone()
                    suite.detach()
                    firstRoot.addContent(suite)
                }
            }
            val outputter = XMLOutputter()
            FileOutputStream(mergedFile).use { out ->
                outputter.output(firstDocument, out)
                out.flush()
            }
            return createAndInitializeInfo(mergedFile, libName)
        } catch (e: JDOMException) {
            LOG.warn("Can not parse scripting additions file: ${e.message}")
            e.printStackTrace()
        } catch (e: IOException) {
            LOG.warn("Can not merge scripting additions: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            LOG.warn("Can not merge scripting additions: ${e.message}")
            e.printStackTrace()
        }
        return null
    }

    /**
     * Generates the dictionary file and creates a [DictionaryInfo] for the application.
     *
     * @return the [DictionaryInfo] for this application, or null
     */
    private fun createDictionaryInfoForApplication(applicationName: String, applicationIoFile: File): DictionaryInfo? {
        // Kotlin stdlib File.extension: suffix after the last '.' WITHOUT the dot.
        val appExtension = applicationIoFile.extension
        if (!SystemInfo.isMac && !("xml" == appExtension || "sdef" == appExtension)) return null
        LOG.debug("=== Caching Dictionary for application [$applicationName] ===")
        val serializePath = serializeDictionaryPathForApplication(applicationName)
        var fileGenerated = false
        val targetFile = File(serializePath)
        if (!targetFile.parentFile.exists() && !targetFile.parentFile.mkdirs()) return null
        try {
            if (!SystemInfo.isMac && ("xml" == appExtension || "sdef" == appExtension)) {
                fileGenerated = copyDictionaryFileToCacheDir(applicationName, applicationIoFile, targetFile, true)
            } else if (SystemInfo.isMac) {
                val cmdName = if ("xml" == appExtension || "sdef" == appExtension) "cat" else "sdef"
                fileGenerated = doGenerateDictionaryFile(applicationName, serializePath, cmdName, applicationIoFile.path)
            }
        } catch (e: NotScriptableApplicationException) {
            LOG.warn("Generation failed: ${e.message}. Adding to ignore list")
            notScriptableApplicationList.add(e.getApplicationName())
        } catch (e: DeveloperToolsNotInstalledException) {
            LOG.warn("Generation failed: ${e.message}")
            // DispatchThread is needed for the write action in copyDictionaryFileToCacheDir().
            if (ApplicationManager.getApplication().isDispatchThread) {
                LOG.warn("Will try to find application scripting definition file...")
                val sdefFile = findSdefForApplication(applicationIoFile)
                if (sdefFile != null && sdefFile.exists()) {
                    fileGenerated = copyDictionaryFileToCacheDir(applicationName, sdefFile, targetFile, true)
                } else {
                    LOG.warn("Scripting definition was not found for application ${applicationIoFile.absolutePath}")
                    notScriptableApplicationList.add(applicationName)
                }
            }
        } finally {
            if (!fileGenerated) {
                LOG.warn("Error occurred while generating file.")
                if (targetFile.delete()) LOG.warn("Created file was deleted")
            } else if (notFoundApplicationList.remove(applicationName)) {
                LOG.debug("Application was removed from ignored list")
            }
        }
        if (fileGenerated && targetFile.exists()) {
            val applicationBundle =
                if (ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS.contains(appExtension)) applicationIoFile else null
            val dInfo = DictionaryInfo(applicationName, targetFile, applicationBundle)
            addDictionaryInfo(dInfo)
            LOG.debug("Dictionary file generated for application [$applicationName]$targetFile")
            return dInfo
        }
        return null
    }

    private fun findSdefForApplication(applicationIoFile: File): File? {
        val appResources = File(applicationIoFile, "/Contents/Resources")
        val files = appResources.listFiles { _, s -> s.endsWith("sdef") }
        if (files != null && files.isNotEmpty()) {
            return files[0]
        }
        return null
    }

    fun isXcodeInstalled(): Boolean {
        if (!SystemInfo.isMac) return false
        val cached = xCodeApplicationFile
        if (cached != null && cached.exists()) return true
        // "null" name means that Xcode was not found previously.
        if (cached != null && "null" == cached.name) return false

        val xCodeApp = File("/Applications/Xcode.app")
        if (xCodeApp.exists()) {
            xCodeApplicationFile = xCodeApp
            return true
        }
        try {
            val engineManager = ScriptEngineManager()
            val engine = engineManager.getEngineByName("AppleScriptEngine")
            if (engine != null) {
                val script = "try\n" +
                    "tell application \"Finder\" to return POSIX path of (get application file id \"com.apple.dt.Xcode\" " +
                    "as alias)\n" + "on error\n" + "  return \"null\"\n" + "end try"
                val scriptResult = engine.eval(script)
                if (scriptResult != null) {
                    val result = File(scriptResult.toString())
                    xCodeApplicationFile = result
                    return result.exists()
                }
            }
        } catch (e: ScriptException) {
            LOG.error("Error evaluating applescript: ${e.message}")
        }
        xCodeApplicationFile = File("null")
        return false
    }

    /**
     * @return true if the file was generated successfully
     * @throws NotScriptableApplicationException if the application does not support AppleScript scripting
     * @throws DeveloperToolsNotInstalledException if Xcode is not installed
     */
    @Throws(NotScriptableApplicationException::class, DeveloperToolsNotInstalledException::class)
    private fun doGenerateDictionaryFile(
        applicationName: String,
        serializePath: String,
        cmdName: String,
        appFilePath: String,
    ): Boolean {
        try {
            val shellCommand = arrayOf("/bin/bash", "-c", " $cmdName \"$appFilePath\" > $serializePath")
            LOG.debug("executing command: ${Arrays.toString(shellCommand)}")
            val timeout = 5L
            val execStart = System.currentTimeMillis()
            val exitStatus = Runtime.getRuntime().exec(shellCommand).waitFor(timeout, TimeUnit.SECONDS)
            val execEnd = System.currentTimeMillis()
            if (!exitStatus) {
                if (isXcodeInstalled()) {
                    throw NotScriptableApplicationException(
                        applicationName,
                        "Waiting time elapsed for command ${Arrays.toString(shellCommand)}. " +
                            "Seems that application \"$applicationName\" is not scriptable.",
                    )
                } else {
                    throw DeveloperToolsNotInstalledException()
                }
            }
            LOG.debug("Waiting time elapsed. Execution time: ${execEnd - execStart} ms.")
            return true
        } catch (e: InterruptedException) {
            LOG.error(
                "Failed to create dictionary file for application [$applicationName] Command:$cmdName " +
                    "target path: $appFilePath Reason: ${e.cause}",
            )
            e.printStackTrace()
        } catch (e: IOException) {
            LOG.error(
                "Failed to create dictionary file for application [$applicationName] Command:$cmdName " +
                    "target path: $appFilePath Reason: ${e.cause}",
            )
            e.printStackTrace()
        }
        return false
    }

    /** Copy the application dictionary file into the IDE's cache directory. */
    private fun copyDictionaryFileToCacheDir(
        applicationName: String,
        applicationDictionaryFile: File,
        targetFile: File,
        rewrite: Boolean,
    ): Boolean {
        if (!targetFile.parentFile.exists()) return false
        if (ApplicationManager.getApplication().isDispatchThread && (!targetFile.exists() || rewrite)) {
            ApplicationManager.getApplication().runWriteAction(
                Runnable {
                    try {
                        if (targetFile.exists() && targetFile.delete()) {
                            LOG.debug("Existing target file deleted: $targetFile")
                        }
                        // Guava's Files.copy(File, File) defaulted to overwrite; java.nio
                        // does not — pass REPLACE_EXISTING explicitly to preserve semantics
                        // (D-11). The pre-delete above also guards the read-after-write race.
                        Files.copy(
                            applicationDictionaryFile.toPath(),
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    } catch (e: IOException) {
                        LOG.error("Failed to move file $applicationDictionaryFile to cache directory: $targetFile")
                        e.printStackTrace()
                    }
                },
            )
        } else {
            LOG.debug("Generated file already exists for application $applicationName")
        }
        if (targetFile.exists()) {
            LOG.debug("Dictionary file moved to ${targetFile.parent} directory")
            return true
        }
        return false
    }

    /** @return true if `/usr/bin/sdef` invocation previously failed to generate a dictionary */
    fun isNotScriptable(applicationName: String): Boolean = notScriptableApplicationList.contains(applicationName)

    /** @return true if the application file was not found earlier in [findApplicationBundleFile] */
    fun isInUnknownList(applicationName: String): Boolean = notFoundApplicationList.contains(applicationName)

    private fun serializeDictionaryPathForApplication(applicationName: String): String {
        val unescaped = "$GENERATED_DICTIONARIES_SYSTEM_FOLDER/${applicationName}_generated.sdef"
        return unescaped.replace(" ", "_")
    }

    private fun getDictionaryInfo(applicationName: String?): DictionaryInfo? = dictionaryInfoMap[applicationName]

    fun getDictionaryFile(applicationName: String?): File? = dictionaryInfoMap[applicationName]?.getDictionaryFile()

    fun getDictionaryInfoByApplicationPath(applicationPath: String): DictionaryInfo? {
        for (dInfo in dictionaryInfoMap.values) {
            val appFile = dInfo.getApplicationFile()
            if (appFile != null && appFile.path == applicationPath) return dInfo
        }
        // Standard libraries do not have an application path.
        if (applicationPath.endsWith("CocoaStandard.sdef")) {
            return dictionaryInfoMap[ApplicationDictionary.COCOA_STANDARD_LIBRARY]
        }
        return null
    }

    fun getCachedApplicationNames(): Collection<String> = dictionaryInfoMap.keys

    // Defensive snapshot: backing storage is concurrent; callers historically did not mutate this.
    fun getDiscoveredApplicationNames(): HashSet<String> = HashSet(discoveredApplicationNames)

    fun isDictionaryInitialized(applicationName: String): Boolean =
        dictionaryInfoMap[applicationName]?.isInitialized() == true

    /**
     * Fills the internal structures with terms from an application dictionary file.
     *
     * @return true if the file was parsed successfully
     */
    private fun parseDictionaryFile(xmlFile: File, applicationName: String): Boolean {
        val builder = newSecureSaxBuilder()
        try {
            val document: Document = builder.build(xmlFile)
            val rootNode: Element = document.rootElement
            val suiteElements: List<Element> = rootNode.children

            if (ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY == applicationName) {
                for (suiteElem in suiteElements) {
                    parseSuiteElementForApplication(suiteElem, applicationName) // TODO: 26/12/15 remove?
                    parseSuiteElementForScriptingAdditions(suiteElem, applicationName)
                }
            } else {
                for (suiteElem in suiteElements) {
                    parseSuiteElementForApplication(suiteElem, applicationName)
                }
            }
            return true
        } catch (e: JDOMException) {
            LOG.warn("Exception occurred while parsing dictionary file: ${e.message}")
            e.printStackTrace()
        } catch (e: IOException) {
            LOG.warn("Exception occurred while parsing dictionary file: ${e.message}")
        }
        return false
    }

    private fun parseSuiteElementForScriptingAdditions(suiteElem: Element, applicationName: String) {
        val suiteClasses: List<Element> = suiteElem.getChildren("class")
        val suiteValueTypes: List<Element> = suiteElem.getChildren("value-type")
        val suiteClassExtensions: List<Element> = suiteElem.getChildren("class-extension")
        val suiteCommands: List<Element> = suiteElem.getChildren("command")
        val recordTypeTags: List<Element> = suiteElem.getChildren("record-type")
        val enumerationTags: List<Element> = suiteElem.getChildren("enumeration")

        for (valType in suiteValueTypes) {
            parseClassElement(applicationName, valType, false)
        }

        for (classTag in suiteClasses) {
            parseClassElement(applicationName, classTag, false)
            parseElementsForApplication(classTag.getChildren("property"), applicationName, stdPropertyNameToDictionarySetMap)
        }

        for (classTag in suiteClassExtensions) {
            parseClassElement(applicationName, classTag, false)
            parseElementsForApplication(classTag.getChildren("property"), applicationName, stdPropertyNameToDictionarySetMap)
        }

        parseElementsForApplication(suiteCommands, applicationName, stdCommandNameToApplicationNameSetMap)
        parseElementsForApplication(recordTypeTags, applicationName, stdRecordNameToApplicationNameSetMap)

        for (recordTag in recordTypeTags) {
            parseElementsForApplication(recordTag.getChildren("property"), applicationName, stdPropertyNameToDictionarySetMap)
        }

        parseElementsForApplication(enumerationTags, applicationName, stdEnumerationNameToApplicationNameSetMap)

        for (enumerationTag in enumerationTags) {
            parseElementsForApplication(
                enumerationTag.getChildren("enumerator"),
                applicationName,
                stdEnumeratorConstantNameToApplicationNameListMap,
            )
        }
    }

    private fun parseSuiteElementForApplication(suiteElem: Element, applicationName: String) {
        val xIncludeNs = Namespace.getNamespace("http://www.w3.org/2003/XInclude")
        val xiIncludes: List<Element> = suiteElem.getChildren("include", xIncludeNs)
        val suiteClasses: List<Element> = suiteElem.getChildren("class")
        val suiteValueTypes: List<Element> = suiteElem.getChildren("value-type")
        val suiteClassExtensions: List<Element> = suiteElem.getChildren("class-extension")
        val suiteCommands: List<Element> = suiteElem.getChildren("command")
        val recordTypeTags: List<Element> = suiteElem.getChildren("record-type")
        val enumerationTags: List<Element> = suiteElem.getChildren("enumeration")

        for (include in xiIncludes) {
            var hrefIncl = include.getAttributeValue("href")
            hrefIncl = hrefIncl.replace("localhost", "")
            val inclFile = File(hrefIncl)
            if (inclFile.exists()) {
                parseDictionaryFile(inclFile, applicationName)
            }
        }

        for (valType in suiteValueTypes) {
            parseClassElement(applicationName, valType, false)
        }

        for (classTag in suiteClasses) {
            parseClassElement(applicationName, classTag, false)
            parseHashElementsForApplication(classTag.getChildren("property"), applicationName, applicationNameToPropertySetMap)
        }

        for (classTag in suiteClassExtensions) {
            parseClassElement(applicationName, classTag, false)
            parseHashElementsForApplication(classTag.getChildren("property"), applicationName, applicationNameToPropertySetMap)
        }

        parseHashElementsForApplication(suiteCommands, applicationName, applicationNameToCommandNameSetMap)
        parseHashElementsForApplication(recordTypeTags, applicationName, applicationNameToRecordNameSetMap)

        for (recordTag in recordTypeTags) {
            parseHashElementsForApplication(recordTag.getChildren("property"), applicationName, applicationNameToPropertySetMap)
        }

        parseHashElementsForApplication(enumerationTags, applicationName, applicationNameToEnumerationNameSetMap)

        for (enumerationTag in enumerationTags) {
            parseHashElementsForApplication(
                enumerationTag.getChildren("enumerator"),
                applicationName,
                applicationNameToEnumeratorConstantNameSetMap,
            )
        }
    }

    private fun parseClassElement(applicationName: String, classElement: Element, isExtends: Boolean) {
        // If isExtends → name is always "application".
        val className = if (isExtends) classElement.getAttributeValue("extends") else classElement.getAttributeValue("name")
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

    companion object {
        private val LOG: Logger = Logger.getInstance("#${AppleScriptSystemDictionaryRegistryService::class.java.name}")

        const val COMPONENT_NAME: String = "AppleScriptSystemDictionaryRegistryComponent"
        private const val APP_DEPTH_SEARCH: Int = 3

        private val GENERATED_DICTIONARIES_SYSTEM_FOLDER: String = "${PathManager.getSystemPath()}/sdef"

        @JvmStatic
        fun getInstance(): AppleScriptSystemDictionaryRegistryService =
            ApplicationManager.getApplication().getService(AppleScriptSystemDictionaryRegistryService::class.java)

        /**
         * SDEF files declare a DOCTYPE pointing at Apple's `sdef.dtd`, so disallowing DOCTYPE entirely would
         * break parsing. Instead, harden the builder against XXE by refusing external DTDs, suppressing
         * general/parameter entities and disabling entity expansion (defends against the Billion Laughs DoS).
         */
        private fun newSecureSaxBuilder(): SAXBuilder {
            val builder = SAXBuilder()
            builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            builder.setFeature("http://xml.org/sax/features/external-general-entities", false)
            builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            builder.expandEntities = false
            return builder
        }

        @Throws(IOException::class)
        private fun stream2file(input: InputStream?, prefix: String, suffix: String): File {
            val tempFile = File.createTempFile(prefix, suffix)
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use { out ->
                requireNotNull(input) { "InputStream for $prefix$suffix is null" }
                var c = input.read()
                while (c != -1) {
                    out.write(c)
                    c = input.read()
                }
                input.close()
            }
            tempFile.deleteOnExit()
            return tempFile
        }

        private fun parseElementsForApplication(
            xmlElements: List<Element>,
            applicationName: String,
            objectTagNameToApplicationNameListMap: MutableMap<String, MutableSet<String>>,
        ) {
            for (applicationObjectTag in xmlElements) {
                parseSimpleElementForObject(applicationObjectTag, applicationName, objectTagNameToApplicationNameListMap)
            }
        }

        private fun parseHashElementsForApplication(
            xmlElements: List<Element>,
            applicationName: String,
            objectTagNameToApplicationNameListMap: MutableMap<String, MutableSet<String>>,
        ) {
            for (applicationObjectTag in xmlElements) {
                hashSimpleElementForObject(applicationObjectTag, applicationName, objectTagNameToApplicationNameListMap)
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
                (existing ?: ConcurrentHashMap.newKeySet<String>()).also { it.add(applicationName) }
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
                (existing ?: ConcurrentHashMap.newKeySet<String>()).also { it.add(applicationObjectName) }
            }
        }

        private fun startsWithWord(string: String, prefix: String): Boolean =
            string.startsWith(prefix) && (prefix.length == string.length || ' ' == string[prefix.length])
    }
}
