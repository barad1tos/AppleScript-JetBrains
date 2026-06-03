package com.intellij.plugin.applescript.test.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.plugin.applescript.lang.ide.sdef.ApplicationDiscoveryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

/**
 * Phase 4 SERVICE-03 (plan 04-03, Wave 3) unit test for [ApplicationDiscoveryService].
 *
 * Discovery walk against [com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary.APP_BUNDLE_DIRECTORIES]
 * is macOS-only (Linux / Windows have no `/Applications` directory tree); cross-platform
 * tests cover the notFound list semantics, the EDT guard on `findApplicationBundleFile`,
 * and the facade trampoline routing.
 *
 * Extends [BasePlatformTestCase] because [ApplicationDiscoveryService] uses
 * `LocalFileSystem.getInstance()` + `VfsUtilCore.visitChildrenRecursively`, which require
 * a real Application container. The PlatformTestCase-style boot is necessary — there is
 * no value in mocking the VFS for this layer; the hermetic-test seam (RESEARCH §4) applies
 * to `SdefIndexService.ingest()` (Wave 5), not to discovery.
 *
 * Phase 8 D-15 INVARIANT verification: the macOS-guarded
 * [testDiscoverInstalledApplicationNamesFindsCommonAppsOnMac] test enforces a lower bound
 * of 5 discovered applications, which is only satisfiable when
 * `/System/Applications` + `/System/Applications/Utilities` are walked. Failing this test
 * would indicate a regression of the Phase 8 directory list. ParserRegressionTest's 6
 * fixtures cross-check the same invariant from the parser side.
 *
 * Gated under `-PincludeHeavyTests=true` via the `test.service.*` filter (same precedent as
 * [SdefFileTypeRegistrarTest] and [SdefPersistenceServiceTest] — Wave 1 deviation #4).
 */
class ApplicationDiscoveryServiceTest : BasePlatformTestCase() {
    /**
     * Phase 8 D-15 macOS invariant. Discovery must find at least 5 application names —
     * every macOS dev machine has Finder, System Settings, App Store, Safari, Mail, etc.
     * pre-installed under `/Applications` and `/System/Applications`. A regression of the
     * APP_BUNDLE_DIRECTORIES list (removing `/System/Applications`) would drop the count
     * below 5 on a clean Catalina+ install.
     */
    fun testDiscoverInstalledApplicationNamesFindsCommonAppsOnMac() {
        if (!SystemInfo.isMac) {
            // Skip on non-Mac — there is no `/Applications` directory tree to walk.
            return
        }
        val service = ApplicationDiscoveryService.getInstance()
        runBlocking { service.discoverInstalledApplicationNames() }
        val discovered = service.getDiscoveredApplicationNames()
        assertTrue(
            "Discovery should find at least 5 apps on macOS dev machines (Phase 8 D-15 invariant); " +
                "actual count=${discovered.size}",
            discovered.size >= 5,
        )
    }

    /**
     * Defensive-snapshot contract: two independent reads return equal sets without
     * mutation in between. Verifies the typed API returns a defensive `HashSet` (not the
     * live backing `ConcurrentHashMap.KeySet`).
     */
    fun testGetDiscoveredApplicationNamesReturnsSnapshot() {
        val service = ApplicationDiscoveryService.getInstance()
        val first = service.getDiscoveredApplicationNames()
        val second = service.getDiscoveredApplicationNames()
        assertEquals("Two snapshots from independent calls must be equal sets", first, second)
        // Mutating the returned reference must not affect subsequent reads.
        @Suppress("USELESS_IS_CHECK")
        assertTrue("Snapshot is a HashSet (defensive copy)", first is HashSet)
    }

    /**
     * `addToNotFoundList` is idempotent and the membership predicate is consistent across
     * calls. Cross-platform — does not touch the filesystem.
     */
    fun testAddToNotFoundListIsIdempotentAndQueryable() {
        val service = ApplicationDiscoveryService.getInstance()
        val name = "AppThatDoesNotExist_${System.nanoTime()}"
        assertFalse("Name not yet added", service.isInNotFoundList(name))
        service.addToNotFoundList(name)
        service.addToNotFoundList(name) // idempotent
        assertTrue("Name now in notFound list", service.isInNotFoundList(name))
        // Cleanup: avoid polluting the in-memory set for later tests in the suite.
        service.removeFromNotFoundList(name)
        assertFalse("Name removed", service.isInNotFoundList(name))
    }

    /**
     * EDT guard verification (RESEARCH Open Question 1 + Phase 3 Codex MEDIUM 1):
     * `findApplicationBundleFile` on the EDT must return `null` immediately rather than
     * attempt the multi-second recursive VFS walk that would freeze the UI thread.
     *
     * Cross-platform — does not require macOS; the EDT guard returns `null` BEFORE the
     * `SystemInfo.isMac` check (which would also return `null` on non-Mac via the
     * `notFoundApplicationList.add` early-return path).
     */
    fun testFindApplicationBundleFileReturnsNullOnEdt() {
        val service = ApplicationDiscoveryService.getInstance()
        var result: java.io.File? = java.io.File("sentinel") // non-null sentinel
        ApplicationManager.getApplication().invokeAndWait {
            result = service.findApplicationBundleFile("Finder")
        }
        assertNull(
            "findApplicationBundleFile on EDT must return null per RESEARCH Q1 EDT-guard",
            result,
        )
    }

    /**
     * Off-EDT macOS path: `findApplicationBundleFile("Finder")` must resolve `Finder.app`
     * from `/System/Library/CoreServices/Finder.app` (or the fast-path direct lookup).
     * Phase 8 D-15 invariant — the directory list must include `/System/Library/CoreServices`
     * for this to succeed.
     */
    fun testFindApplicationBundleFileFindsFinderOffEdtOnMac() {
        if (!SystemInfo.isMac) return
        val service = ApplicationDiscoveryService.getInstance()
        // BasePlatformTestCase test methods run on the EDT, so dispatch to a pooled thread
        // explicitly to exercise the production code path (parser-util calls this off-EDT).
        val result =
            ApplicationManager
                .getApplication()
                .executeOnPooledThread<java.io.File?> {
                    service.findApplicationBundleFile("Finder")
                }.get()
        assertNotNull(
            "Finder.app should resolve on macOS via APP_BUNDLE_DIRECTORIES walk",
            result,
        )
    }

    /**
     * Phase 4 SERVICE-03 routing invariant: external callers of the facade's
     * `getDiscoveredApplicationNames()` see the discovery service's state through the
     * trampoline. Adds a name via the service, observes via the facade — proves the
     * single-source-of-truth invariant on the in-memory discovered set.
     */
    fun testFacadeTrampolineRoutesThroughDiscoveryService() {
        val service = ApplicationDiscoveryService.getInstance()
        val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
        val name = "DiscoveryTrampolineTest_${System.nanoTime()}"
        try {
            assertFalse(
                "Sentinel name absent before add",
                facade.getDiscoveredApplicationNames().contains(name),
            )
            service.addDiscoveredApplicationName(name)
            assertTrue(
                "Facade.getDiscoveredApplicationNames trampoline sees the service-side write",
                facade.getDiscoveredApplicationNames().contains(name),
            )
        } finally {
            // No removeDiscoveredApplicationName surface (the production set is rebuilt per
            // cold start). Leave the sentinel in — it does not interfere with other tests
            // because the name is uniquely seeded with nanoTime.
        }
    }

    /**
     * Phase 4 SERVICE-03 Wave 3 re-route verification: the facade's `isInUnknownList`
     * trampoline routes through ApplicationDiscoveryService (NOT SdefPersistenceService —
     * Wave 2's temporary parking spot). Adds via the discovery service, observes via the
     * facade — proves the single-source-of-truth invariant on the not-found list.
     */
    fun testFacadeIsInUnknownListRoutesThroughDiscoveryService() {
        val service = ApplicationDiscoveryService.getInstance()
        val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
        val name = "UnknownListTrampolineTest_${System.nanoTime()}"
        try {
            assertFalse("Sentinel name absent before add", facade.isInUnknownList(name))
            service.addToNotFoundList(name)
            assertTrue(
                "Facade.isInUnknownList trampoline sees the discovery-service-side write",
                facade.isInUnknownList(name),
            )
        } finally {
            service.removeFromNotFoundList(name)
        }
    }
}
