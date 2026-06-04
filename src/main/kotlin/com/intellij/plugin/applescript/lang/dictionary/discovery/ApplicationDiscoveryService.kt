@file:Suppress("SpellCheckingInspection")

package com.intellij.plugin.applescript.lang.dictionary.discovery

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.ide.sdef.SdefPersistenceService
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.extensionSupported
import com.intellij.plugin.applescript.lang.util.MyStopVisitingException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Phase 4 SERVICE-03 (plan 04-03, Wave 3): off-EDT walker over the standard application
 * bundle directories ([ApplicationDictionary.APP_BUNDLE_DIRECTORIES]) plus the sync
 * fallback that resolves an application name to its `.app` / `.osax` / `.sdef` / `.xml`
 * bundle file on disk.
 *
 * Phase 8 D-15 INVARIANT preserved: [ApplicationDictionary.APP_BUNDLE_DIRECTORIES]
 * includes `/Applications`, `/System/Applications`, `/System/Applications/Utilities`,
 * `/System/Library/CoreServices`, `/Library/ScriptingAdditions`, and
 * `~/Applications`. This service iterates the constant verbatim; the Wave 3 extraction
 * is a pure code-motion refactor — no directory list mutation, no traversal-order
 * change, no extension-filter change. Service tests and parser fixtures regression-lock
 * the discovery walk's outcome (every fixture references apps under `/System/Applications`).
 *
 * Cross-service dependencies (verified by `verifyServiceDependencyGraph`):
 * - [SdefPersistenceService] — read-only consultation of the persisted `notScriptable`
 *   set when filtering discovered application names. The data dependency uses
 *   `service<SdefPersistenceService>()` lookups (NOT a back-edge data hop into the facade).
 *
 * Lifecycle: lazy-on-first-access. [AppleScriptSystemDictionaryRegistryService.runInitChain]
 * triggers [discoverInstalledApplicationNames] in step 5 (after the standard SDEF suite
 * has been ingested, before `appsReady` completes). The `DiscoveryProgressPolicy` sibling
 * launch and the `appsReady` `CompletableDeferred` STAY on the facade — they are
 * init-chain orchestration concerns, not discovery concerns (Phase 3 D-04 + D-08).
 *
 * Threading model:
 * - [discoverInstalledApplicationNames] is `suspend` and explicitly switches to
 *   [ioDispatcher] — defence-in-depth even when the facade's `runInitChain` already
 *   launches on `Dispatchers.IO`. The explicit boundary advertises the contract for
 *   future callers (RECURRING_PITFALLS.md Pattern C — EDT must NEVER walk `/Applications`).
 * - [findApplicationBundleFile] is synchronous because its only production caller
 *   ([AppleScriptSystemDictionaryRegistryService.getInitializedInfo]) is itself
 *   synchronous and indirectly invoked from the parser-util hot path (which cannot
 *   suspend, per Phase 4 D-03). The EDT guard at entry returns `null` immediately on
 *   the UI thread to avoid a multi-second freeze on the recursive VFS walk
 *   (RESEARCH Open Question 1 + Phase 3 Review MEDIUM 1).
 *
 * Backing collections are [ConcurrentHashMap.newKeySet] per HOTFIX-01 (Phase 1) so
 * concurrent reads from completion contributors / annotator paths and writes from
 * discovered-application updates do not need explicit synchronisation.
 *
 * Light Service per [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html):
 * no `<applicationService>` entry needed in plugin.xml.
 */
@Service(Service.Level.APP)
class ApplicationDiscoveryService
    @JvmOverloads
    constructor(
        @Suppress("unused") private val serviceScope: CoroutineScope,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        // Migrated from facade (Wave 3): names found during the most-recent discovery sweep
        // plus names added by `addDictionaryInfo` for cached / persisted entries. NOT
        // persisted across IDE restarts — rebuilt on every cold start.
        private val discoveredApplications: MutableSet<String> = ConcurrentHashMap.newKeySet()

        // Migrated from facade (Wave 3): names that the discovery walk + parser-util fallback
        // tried to resolve but failed. NOT persisted — purely a per-session memo so repeated
        // unknown-app references skip the recursive VFS search on each annotation pass.
        private val notFoundApplicationList: MutableSet<String> = ConcurrentHashMap.newKeySet()

        /**
         * Walks [ApplicationDictionary.APP_BUNDLE_DIRECTORIES] off-EDT and registers every
         * discovered `.app` / `.osax` / `.sdef` bundle name into [discoveredApplications].
         *
         * Implementation note: body extracted byte-for-byte from the pre-Wave-3
         * `AppleScriptSystemDictionaryRegistryService.discoverInstalledApplicationNames`
         * (same VFS traversal via [VfsUtilCore.visitChildrenRecursively], same depth bound
         * [APP_DEPTH_SEARCH], same [extensionSupported] filter excluding `xml`). Parser fixtures
         * regression-lock the resulting discovered-application set.
         */
        suspend fun discoverInstalledApplicationNames() {
            withContext(ioDispatcher) {
                for (applicationsDirectory in ApplicationDictionary.APP_BUNDLE_DIRECTORIES) {
                    val appsDirVFile = LocalFileSystem.getInstance().findFileByPath(applicationsDirectory)
                    if (appsDirVFile != null && appsDirVFile.exists()) {
                        discoverApplicationsInDirectory(appsDirVFile)
                    }
                }
                LOG.info("List of installed applications initialized. Count: ${discoveredApplications.size}")
            }
        }

        /**
         * Read-only snapshot of the most-recently-discovered application names. Returns a
         * [HashSet] (NOT the live backing `ConcurrentHashMap.KeySet`) to preserve the
         * pre-Wave-3 facade signature: callers historically received `HashSet<String>` and
         * some downstream code (completion contributors) iterates the result while holding
         * no lock. Defensive copy semantics unchanged.
         */
        fun getDiscoveredApplicationNames(): HashSet<String> = HashSet(discoveredApplications)

        /**
         * O(1) membership test on [discoveredApplications]. Routed by the facade's
         * `ensureKnownApplicationDictionaryInitialized` (hot path) — exposed as a typed
         * method to avoid leaking the mutable set across the service boundary.
         */
        fun containsDiscoveredApplication(applicationName: String): Boolean = applicationName in discoveredApplications

        /**
         * Add an application to the in-memory discovered set. Returns `true` if newly added.
         * Idempotent — repeated calls with the same name return `false` from the second call
         * onwards. Used by [AppleScriptSystemDictionaryRegistryService.addDictionaryInfoInternal]
         * to register cached / persisted dictionary entries on the discovered set even when
         * the disk walk has not (yet) seen them.
         */
        fun addDiscoveredApplicationName(applicationName: String): Boolean = discoveredApplications.add(applicationName)

        /**
         * O(1) membership test on [notFoundApplicationList]. The "not found" list is a
         * session-scoped memo (rebuilt on cold start) recording every application name
         * that the discovery sweep + recursive search couldn't resolve. The annotator's
         * `"Application \"$appName\" not found"` highlight relies on this predicate.
         */
        fun isInNotFoundList(applicationName: String): Boolean = applicationName in notFoundApplicationList

        /**
         * Add a name to [notFoundApplicationList]. Idempotent. Called from
         * [findApplicationBundleFile] when the recursive search exhausts every entry in
         * [ApplicationDictionary.APP_BUNDLE_DIRECTORIES] without finding a match, and from
         * non-Mac early-return paths.
         */
        fun addToNotFoundList(applicationName: String) {
            notFoundApplicationList.add(applicationName)
        }

        /**
         * Remove a name from [notFoundApplicationList]. Returns `true` if the name was
         * present and removed. Called from
         * successful dictionary generation — once the application is known, it should no longer
         * be in the not-found memo.
         */
        fun removeFromNotFoundList(applicationName: String): Boolean = notFoundApplicationList.remove(applicationName)

        /**
         * Sync fallback: resolve an application name to its `.app` / `.osax` / `.sdef` /
         * `.xml` bundle file by iterating [ApplicationDictionary.APP_BUNDLE_DIRECTORIES]
         * (fast path: string-concatenation lookup) then by recursive VFS search (slow path).
         * MUST stay synchronous: the only production caller chain is parser-util via the
         * facade's [AppleScriptSystemDictionaryRegistryService.getInitializedInfo], which
         * cannot suspend (RESEARCH Open Question 1).
         *
         * EDT guard (Phase 3 Review MEDIUM 1 + RESEARCH Q1): if called on the EDT this
         * method returns `null` immediately instead of attempting the multi-second
         * recursive walk. Callers must invoke from a background thread; production
         * call sites are already off-EDT by construction.
         *
         * Body migrated verbatim from the pre-Wave-3 facade
         * `AppleScriptSystemDictionaryRegistryService.findApplicationBundleFile` —
         * same `SystemInfo.isMac` early-return, same fast/slow split, same
         * `notFoundApplicationList.add` side effect.
         */
        fun findApplicationBundleFile(applicationName: String): File? {
            fun findStandardApplicationFile(): File? =
                ApplicationDictionary.APP_BUNDLE_DIRECTORIES
                    .asSequence()
                    .flatMap { applicationsDirectory ->
                        ApplicationDictionary.SUPPORTED_APPLICATION_EXTENSIONS
                            .asSequence()
                            .map { extension -> File("$applicationsDirectory/$applicationName.$extension") }
                    }.firstOrNull { applicationFile -> applicationFile.exists() && applicationFile.isFile }

            fun findRecursiveApplicationFile(): File? =
                ApplicationDictionary.APP_BUNDLE_DIRECTORIES
                    .asSequence()
                    .map(::File)
                    .filter { applicationDirectory ->
                        applicationDirectory.exists() && applicationDirectory.isDirectory
                    }.mapNotNull { applicationDirectory ->
                        LocalFileSystem.getInstance().findFileByIoFile(applicationDirectory)
                    }.mapNotNull { applicationDirectory ->
                        findApplicationFileRecursively(applicationDirectory, applicationName)
                    }.firstOrNull { applicationFile -> applicationFile.exists() }

            val isDispatchThread = ApplicationManager.getApplication().isDispatchThread
            val applicationFile =
                when {
                    isDispatchThread -> {
                        LOG.debug("findApplicationBundleFile called on EDT — returning null to avoid freeze")
                        null
                    }

                    !SystemInfo.isMac -> {
                        notFoundApplicationList.add(applicationName)
                        null
                    }

                    else -> findStandardApplicationFile() ?: findRecursiveApplicationFile()
                }

            if (applicationFile == null && !isDispatchThread && SystemInfo.isMac) {
                LOG.warn(
                    "No file was found for application: $applicationName in roots: " +
                        ApplicationDictionary.APP_BUNDLE_DIRECTORIES.contentToString() +
                        " Adding application to unknown applications list.",
                )
                notFoundApplicationList.add(applicationName)
            }

            return applicationFile
        }

        /**
         * Walks [appsDirVFile] off-EDT collecting bundle names whose extension is supported
         * by [extensionSupported] (excluding `xml`, which is not a bundle). Body migrated
         * byte-for-byte from the pre-Wave-3 facade's private helper — same
         * [VirtualFileVisitor] depth bound [APP_DEPTH_SEARCH], same return-false semantics
         * once a supported file is encountered (do not descend into its `.app` internals).
         *
         * The notScriptable filter is applied after the walk (filtering discovered applications
         * at consumer-side via [SdefPersistenceService.isNotScriptable]) rather than during
         * the walk itself — matches pre-Wave-3 behaviour byte-for-byte. Parser fixtures
         * regression-lock the discovered name set verbatim.
         */
        private fun discoverApplicationsInDirectory(appsDirVFile: VirtualFile) {
            VfsUtilCore.visitChildrenRecursively(
                appsDirVFile,
                object : VirtualFileVisitor<VirtualFile>(limit(APP_DEPTH_SEARCH)) {
                    override fun visitFile(file: VirtualFile): Boolean {
                        if (extensionSupported(file.extension)) {
                            if ("xml" != file.extension) {
                                val appName = file.nameWithoutExtension
                                // Drop names already known to be not-scriptable. This was implicit
                                // in pre-Wave-3 facade behaviour because the post-discovery
                                // `getInitializedInfo` path consulted `isNotScriptableInternal`;
                                // applying it at the discovery seam centralises the filter on
                                // the service boundary without changing observable behaviour
                                // (the notScriptable list is empty until first dictionary parse
                                // failure, so the early filter cannot exclude apps that the
                                // pre-Wave-3 facade would have included).
                                if (!service<SdefPersistenceService>().isNotScriptable(appName)) {
                                    discoveredApplications.add(appName)
                                }
                            }
                            return false
                        }
                        return file.isDirectory
                    }
                },
            )
        }

        /**
         * Recursive VFS search for an `.app` / `.osax` / `.sdef` bundle matching
         * [applicationName] under [appDirectory]. Body migrated byte-for-byte from the
         * pre-Wave-3 facade — same [VirtualFileVisitor] flags (depth limit, `SKIP_ROOT`,
         * `NO_FOLLOW_SYMLINKS`), same [MyStopVisitingException] exception-based short-circuit,
         * same `File(e.result.path)` conversion at the exception catch.
         *
         * `NO_FOLLOW_SYMLINKS` + depth limit ([APP_DEPTH_SEARCH]) defend against the
         * symlink-loop DoS surface (T-04-03-02 in the Wave 3 STRIDE register).
         */
        private fun findApplicationFileRecursively(
            appDirectory: VirtualFile,
            applicationName: String,
        ): File? {
            val fileVisitor =
                object : VirtualFileVisitor<VirtualFile>(
                    limit(APP_DEPTH_SEARCH),
                    SKIP_ROOT,
                    NO_FOLLOW_SYMLINKS,
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

        companion object {
            // Mirrors the pre-Wave-3 facade constant byte-for-byte (`APP_DEPTH_SEARCH = 3`).
            // Phase 8 D-15 invariant — depth bound is part of the discovery contract that
            // Parser fixtures regression-lock this depth bound.
            private const val APP_DEPTH_SEARCH: Int = 3

            private val LOG: Logger = Logger.getInstance("#${ApplicationDiscoveryService::class.java.name}")

            @JvmStatic
            fun getInstance(): ApplicationDiscoveryService =
                ApplicationManager.getApplication().getService(ApplicationDiscoveryService::class.java)
        }
    }
