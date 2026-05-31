package com.intellij.plugin.applescript.lang.ide.sdef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.CoroutineScope
import java.io.File
import javax.script.ScriptEngineManager
import javax.script.ScriptException

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
class XcodeDetectionService @JvmOverloads constructor(
    @Suppress("unused") private val serviceScope: CoroutineScope,
) {

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

    companion object {
        private val LOG: Logger = Logger.getInstance("#${XcodeDetectionService::class.java.name}")

        @JvmStatic
        fun getInstance(): XcodeDetectionService =
            ApplicationManager.getApplication().getService(XcodeDetectionService::class.java)
    }
}
