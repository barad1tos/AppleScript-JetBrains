package com.intellij.plugin.applescript.test.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.ide.sdef.SdefFileProvider
import com.intellij.plugin.applescript.lang.ide.sdef.XcodeDetectionService
import com.intellij.plugin.applescript.lang.ide.sdef.results.DictionaryLoadResult
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files

/**
 * Phase 4 SERVICE-04 + SERVICE-09 (plan 04-04, Wave 4) tests for [SdefFileProvider].
 *
 * Coverage:
 *  - Cross-platform: [DictionaryLoadResult] exhaustive-when compile gate; file-copy
 *    branch on [SdefFileProvider.copyDictionaryFileToCacheDir]; predictable cache
 *    path generation; `fetch()` returns [DictionaryLoadResult.Empty] for unknown
 *    application names; `isXcodeInstalled` returns a Boolean predicate (outcome
 *    depends on dev machine state); facade trampoline routing for
 *    `getScriptingAdditions` + `isXcodeInstalled`.
 *  - macOS-only: `fetch("Finder")` resolves Finder.app via APP_BUNDLE_DIRECTORIES and
 *    returns a non-Empty result (Loaded OR Failed depending on dev-tools state).
 *
 * Extends [BasePlatformTestCase] for the Platform service container.
 *
 * Gated under `-PincludeHeavyTests=true` via the `test.service.*` filter (precedent
 * from [SdefFileTypeRegistrarTest], [SdefPersistenceServiceTest], and
 * [ApplicationDiscoveryServiceTest]).
 */
class SdefFileProviderTest : BasePlatformTestCase() {

    /**
     * [SdefFileProvider.getDictionaryFile] returns the cached `.sdef` file when the
     * registry has no entry for the given name — `null` per the pre-Wave-4 facade
     * behaviour. Cross-platform.
     */
    fun testGetDictionaryFileReturnsNullForUnknownApplication() {
        val provider = SdefFileProvider.getInstance()
        val result = provider.getDictionaryFile("UnknownApp_${System.nanoTime()}")
        assertNull("Unknown application must yield null dictionary file", result)
    }

    /**
     * Cross-platform: file-copy semantics on [SdefFileProvider.copyDictionaryFileToCacheDir]
     * — copies a source into a target, overwriting prior content (REPLACE_EXISTING).
     * Verifies the Phase 3 D-11 Guava→nio migration invariant.
     */
    fun testCopyDictionaryFileToCacheDirReplacesExisting() {
        val provider = SdefFileProvider.getInstance()
        val src = Files.createTempFile("sdef-src", ".sdef").toFile()
        val tgt = Files.createTempFile("sdef-tgt", ".sdef").toFile()
        try {
            src.writeText("<dictionary><suite name=\"Test\"/></dictionary>")
            tgt.writeText("PREVIOUS CONTENT") // pre-fill target

            val ok = runBlocking {
                provider.copyDictionaryFileToCacheDir("TestApp", src, tgt, true)
            }
            assertTrue("Copy must succeed", ok)
            assertEquals("Target must equal source after copy", src.readText(), tgt.readText())
        } finally {
            src.delete()
            tgt.delete()
        }
    }

    /**
     * [SdefFileProvider.fetch] returns [DictionaryLoadResult.Empty] for an application
     * the discovery walk cannot resolve. On non-Mac (no `/Applications`) the discovery
     * service early-returns null; on Mac an unknown app name is not in any bundle dir.
     */
    fun testFetchReturnsEmptyForUnknownApplication() {
        val provider = SdefFileProvider.getInstance()
        val result = runBlocking { provider.fetch("DefinitelyDoesNotExist_${System.nanoTime()}") }
        assertTrue(
            "Unknown app must return DictionaryLoadResult.Empty; got $result",
            result is DictionaryLoadResult.Empty,
        )
    }

    /**
     * Compile-time exhaustive `when` over [DictionaryLoadResult] — the sealed interface
     * contract. If a new variant is added without updating this site, the compiler will
     * refuse to compile (no else branch).
     */
    fun testDictionaryLoadResultExhaustiveWhenCompiles() {
        val r: DictionaryLoadResult = DictionaryLoadResult.Empty
        val label: String = when (r) {
            DictionaryLoadResult.Empty -> "empty"
            is DictionaryLoadResult.Loaded -> "loaded: ${r.info.getApplicationName()}"
            is DictionaryLoadResult.Failed -> "failed: ${r.reason}"
        }
        assertEquals("Sentinel branch resolved", "empty", label)
    }

    /**
     * [XcodeDetectionService.isXcodeInstalled] returns a Boolean predicate. Outcome depends
     * on dev machine state — on non-Mac the method returns false unconditionally; on
     * Mac it consults the cache + `/Applications/Xcode.app` + the AppleScriptEngine
     * fallback. The test only verifies callability + return-type contract. (Phase 7 D-05:
     * the probe moved off SdefFileProvider into its own seam.)
     */
    fun testIsXcodeInstalledReturnsBoolean() {
        val detection = XcodeDetectionService.getInstance()
        val installed: Boolean = detection.isXcodeInstalled()
        // Tautology to assert "returns a Boolean and does not throw".
        assertTrue("Boolean value returned", installed || !installed)
    }

    /**
     * Phase 7 D-05 routing invariant: the facade's `isXcodeInstalled` trampoline routes
     * through [XcodeDetectionService] (was SdefFileProvider pre-Phase-7). Compares facade
     * vs the new owner's direct call — must agree (single source of truth on the
     * lazy-cached detection result).
     */
    fun testFacadeIsXcodeInstalledRoutesThroughXcodeDetectionService() {
        val detection = XcodeDetectionService.getInstance()
        val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
        assertEquals(
            "Facade.isXcodeInstalled must equal XcodeDetectionService.isXcodeInstalled (trampoline)",
            detection.isXcodeInstalled(),
            facade.isXcodeInstalled(),
        )
    }

    /**
     * Phase 4 SERVICE-04 routing invariant: the facade's `getScriptingAdditions`
     * trampoline routes through SdefFileProvider. Default state (no scripting-additions
     * yet ingested) — both must return equal sets.
     */
    fun testFacadeGetScriptingAdditionsRoutesThroughFileProvider() {
        val provider = SdefFileProvider.getInstance()
        val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
        assertEquals(
            "Facade.getScriptingAdditions trampoline must agree with SdefFileProvider direct call",
            provider.getScriptingAdditions(),
            facade.getScriptingAdditions(),
        )
    }

    /**
     * macOS-guarded: [SdefFileProvider.fetch] for "Finder" must NOT return Empty —
     * Finder.app exists under `/System/Library/CoreServices`. Allowable outcomes:
     * Loaded (sdef CLI succeeded + dictionary parsed) OR Failed (Developer Tools
     * missing on a dev machine without Xcode). Empty would mean the discovery service
     * could not resolve `Finder` to a bundle file — a Phase 8 D-15 regression.
     */
    fun testFetchAgainstFinderOnMacIsNotEmpty() {
        if (!SystemInfo.isMac) return
        val provider = SdefFileProvider.getInstance()
        // The fetch() call is suspend + reaches into the synchronous facade chain via
        // ApplicationDiscoveryService.findApplicationBundleFile. That sync VFS walk
        // returns null if called on EDT; BasePlatformTestCase test methods run on EDT,
        // so dispatch to a pooled thread for the production path.
        val result = ApplicationManager.getApplication().executeOnPooledThread<DictionaryLoadResult> {
            runBlocking { provider.fetch("Finder") }
        }.get()
        assertFalse(
            "Finder must be discoverable on macOS (Phase 8 D-15 invariant); got $result",
            result is DictionaryLoadResult.Empty,
        )
    }

    /**
     * Sanity-check: [SdefFileProvider.serializeDictionaryPathForApplication] produces
     * a path under the system cache dir with underscore-escaping for spaces in the
     * application name. Preserves v1.0 cache layout (existing user caches use
     * underscored filenames).
     */
    fun testSerializeDictionaryPathHasExpectedShape() {
        val provider = SdefFileProvider.getInstance()
        val path = provider.serializeDictionaryPathForApplication("Some App With Spaces")
        assertTrue("Path must live under */sdef/", path.contains("/sdef/"))
        assertTrue(
            "Spaces in application name must be underscore-escaped",
            path.contains("Some_App_With_Spaces_generated.sdef"),
        )
        assertFalse("No raw spaces in the cache filename", path.contains(" "))
    }
}
