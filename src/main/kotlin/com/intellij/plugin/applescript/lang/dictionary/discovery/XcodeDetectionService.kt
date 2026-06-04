package com.intellij.plugin.applescript.lang.dictionary.discovery

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.files.SdefFileProvider
import com.intellij.plugin.applescript.lang.dictionary.filetype.SdefFileTypeRegistrar
import java.io.File
import javax.script.ScriptEngineManager
import javax.script.ScriptException

private const val APPLE_SCRIPT_ENGINE = "AppleScriptEngine"
private const val XCODE_APPLICATION_PATH = "/Applications/Xcode.app"
private const val XCODE_ABSENT_SENTINEL = "null"
private const val XCODE_BUNDLE_ID = "com.apple.dt.Xcode"
private val FIND_XCODE_SCRIPT: String =
    """
    try
      tell application "Finder" to return POSIX path of (get application file id "$XCODE_BUNDLE_ID" as alias)
    on error
      return "$XCODE_ABSENT_SENTINEL"
    end try
    """.trimIndent()

/**
 * Phase 7 CLEANUP-01 / D-05: Xcode-detection seam, extracted out of [SdefFileProvider].
 *
 * Detecting whether Xcode (and therefore Apple's developer tools, incl. the `sdef` CLI) is
 * installed is orthogonal to SDEF file generation / parsing — it is a host-environment probe,
 * not a dictionary concern. It previously lived on [SdefFileProvider] (Phase 4 SERVICE-04,
 * flagged there for v1.6 cleanup); this service closes that marker by giving the probe its own
 * single-responsibility owner. The body, including the lazy-cached detection result, is moved
 * here byte-for-byte — no behavioural change. Every prior caller (the file provider's internal
 * generation path, the facade trampoline, the color annotator via the facade) routes through
 * `service<XcodeDetectionService>().isXcodeInstalled()` and gets the same Boolean.
 *
 * Light Service per [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html):
 * no `<applicationService>` entry needed in plugin.xml (auto-discovered via the `@Service`
 * annotation, mirroring [SdefFileProvider] and [SdefFileTypeRegistrar]).
 */
@Service(Service.Level.APP)
class XcodeDetectionService {
    // Migrated from SdefFileProvider (Phase 7 D-05): lazy-cached Xcode bundle file. Null means
    // "not yet detected"; a non-null File with name "null" means "previously detected as absent"
    // (sentinel preserved byte-for-byte from the SdefFileProvider body — do NOT change the
    // sentinel encoding without re-running the AppleScriptColorAnnotator path on a dev machine
    // without Xcode installed).
    private var xCodeApplicationFile: File? = null

    /**
     * Detect whether Xcode (and the developer tools it brings, incl. the `sdef` CLI) is
     * installed on the host. Result is lazy-cached in [xCodeApplicationFile].
     *
     * Dispatcher invariant: non-suspend; safe on any thread. The only writes are inside this
     * method and the field is consulted only here, so no `@Volatile` is required — preserved
     * byte-for-byte from the pre-extraction SdefFileProvider body.
     */
    fun isXcodeInstalled(): Boolean {
        val isInstalled =
            if (!SystemInfo.isMac) {
                false
            } else {
                readCachedInstallState()
                    ?: detectXcodeApplicationFile().exists()
            }
        return isInstalled
    }

    private fun readCachedInstallState(): Boolean? {
        val cached = xCodeApplicationFile
        return when {
            cached == null -> null
            cached.exists() -> true
            cached.name == XCODE_ABSENT_SENTINEL -> false
            else -> null
        }
    }

    private fun detectXcodeApplicationFile(): File {
        val xCodeApplication = File(XCODE_APPLICATION_PATH)
        val detected =
            if (xCodeApplication.exists()) {
                xCodeApplication
            } else {
                findXcodeWithAppleScript() ?: File(XCODE_ABSENT_SENTINEL)
            }
        xCodeApplicationFile = detected
        return detected
    }

    private fun findXcodeWithAppleScript(): File? {
        val engine = ScriptEngineManager().getEngineByName(APPLE_SCRIPT_ENGINE)
        return if (engine == null) {
            null
        } else {
            try {
                engine.eval(FIND_XCODE_SCRIPT)?.toString()?.let(::File)
            } catch (e: ScriptException) {
                LOG.error("Error evaluating applescript: ${e.message}")
                null
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getInstance("#${XcodeDetectionService::class.java.name}")
        private val application
            get() = ApplicationManager.getApplication()

        @JvmStatic
        fun getInstance(): XcodeDetectionService = application.getService(XcodeDetectionService::class.java)
    }
}
