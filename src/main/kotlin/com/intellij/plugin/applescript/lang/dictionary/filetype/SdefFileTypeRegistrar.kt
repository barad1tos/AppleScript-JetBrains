package com.intellij.plugin.applescript.lang.dictionary.filetype

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileTypes.FileTypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Phase 4 SERVICE-01 (plan 04-01, Wave 1 pilot): Light Service that associates `.sdef` files
 * with the XML file type at IDE startup.
 *
 * Extracted from the facade's former extension-registration logic as the lowest-blast-radius
 * warm-up extraction per CONTEXT D-02 (leaf-up by dependency direction).
 * Light Service per the IntelliJ Platform Plugin Services docs — no `<applicationService>`
 * entry needed in plugin.xml (auto-discovered by the platform via the `@Service` annotation).
 *
 * Lifecycle: lazy-on-first-access. The startup pipeline triggers it as the first step
 * of the init pipeline via `service<SdefFileTypeRegistrar>().register()` (preserves Phase 3
 * ordering: register -> load -> ingestStandard -> discoverApps).
 *
 * Constructor signature follows Phase 3 COROUTINE-03: constructor-injected [CoroutineScope]
 * (Platform-supplied) + injectable EDT coroutine context for test determinism. The
 * `@JvmOverloads` annotation emits the single-arg `(CoroutineScope)` JVM constructor that
 * the Platform service container expects for `@Service(Service.Level.APP)` services (per
 * `InstantiateKt.findConstructor` — same shape as the facade at AppleScriptSystemDictionary
 * RegistryService:63-77 verified during Phase 3 gap closure).
 */
@Service(Service.Level.APP)
class SdefFileTypeRegistrar
    @JvmOverloads
    constructor(
        @Suppress("unused") private val serviceScope: CoroutineScope,
        /**
         * Coroutine context for the EDT dispatch. Typed as [CoroutineContext] (not a
         * `CoroutineDispatcher`) because the Platform's
         * [com.intellij.openapi.application.EDT] extension returns a [CoroutineContext] on the
         * IPGP 2.16.0 / Platform 2025.1 classpath (matches the facade's `withContext(Dispatchers.EDT)`
         * pattern at AppleScriptSystemDictionaryRegistryService line ~175). Tests can inject a
         * test dispatcher because dispatcher implementations are also [CoroutineContext]s.
         */
        private val edtDispatcher: CoroutineContext = EmptyCoroutineContext + Dispatchers.EDT,
    ) {
        /**
         * Idempotent registration of the `.sdef` <-> XML file type association.
         *
         * Safe to call multiple times: [FileTypeManager.associateExtension] is a no-op if the
         * association already exists. The actual write action is dispatched onto EDT via
         * [edtDispatcher] (RECURRING_PITFALLS Pattern C — `runWriteAction` requires EDT and the
         * Platform-blessed dispatcher is `Dispatchers.EDT`, NOT `kotlinx.coroutines.Dispatchers.Main`).
         *
         * Suspend so the startup pipeline caller
         * structurally awaits completion before progressing to step 2 (`loadFromState`). Phase 3
         * D-08 invariant preserved: the `.sdef` <-> XML association is observable user behaviour
         * (the `LoadDictionaryAction` file picker filters on `.sdef` after first invocation).
         */
        suspend fun register() {
            withContext(edtDispatcher) {
                ApplicationManager.getApplication().runWriteAction {
                    val xmlFileType = FileTypeManager.getInstance().getFileTypeByExtension("xml")
                    FileTypeManager.getInstance().associateExtension(xmlFileType, "sdef")
                }
            }
        }

        companion object {
            @JvmStatic
            fun getInstance(): SdefFileTypeRegistrar =
                ApplicationManager
                    .getApplication()
                    .getService(SdefFileTypeRegistrar::class.java)
        }
    }
