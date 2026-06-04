package com.intellij.plugin.applescript.test.service

import com.intellij.plugin.applescript.lang.dictionary.index.IngestResult
import com.intellij.plugin.applescript.lang.dictionary.index.LookupResult
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Phase 4 SERVICE-05 + SERVICE-09 + CLEANUP-03 (v1.6) enabler.
 *
 * HERMETIC unit test — NO [com.intellij.testFramework.fixtures.BasePlatformTestCase], NO
 * `myFixture`, NO `/Applications` scan. Constructs [SdefIndexService] directly with an injected
 * [StandardTestDispatcher] + [TestScope] and exercises the suspend [SdefIndexService.ingest]
 * write seam against [SyntheticSuiteFixtures]-produced temp `.sdef` files; reads results via
 * the immutable [SdefIndexSnapshot] returned from [SdefIndexService.snapshot].
 *
 * Notable deviation from the plan example: the plan envisioned `ingest(List<Suite>)` where
 * `Suite` is a Phase 2 data class. Phase 2 [com.intellij.plugin.applescript.lang.sdef.Suite] is
 * actually an `interface` and the live XML pipeline does NOT produce `Suite` values — it parses
 * raw JDOM `Element`s. RESEARCH §4 Assumption A2 closure: the test consumes the same
 * temp-`.sdef`-file shape that production [SdefIndexService.parseDictionaryFile] consumes during
 * facade init, so we exercise the real ingest path without any synthetic-Suite construction
 * layer. Documented in 04-05-SUMMARY deviations.
 *
 * Phase 8 invariant: [SdefIndexService.parseDictionaryFile] uses an XXE-hardened legacy JDOM
 * parser bridge. The DOCTYPE test below verifies Apple's SDEF DOCTYPE remains accepted while
 * external DTD loading is disabled by the bridge.
 *
 * This pattern is the prerequisite for v1.6 CLEANUP-03 (promote heavy tests to default CI):
 * once hermetic-test seams exist for every service, CI can run unit tests without
 * `-PincludeHeavyTests=true`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SdefIndexServiceTest {
    /**
     * Builds a freshly-constructed service whose dispatcher is bound to [scheduler] — the
     * receiver [kotlinx.coroutines.test.TestScope.testScheduler] inside `runTest`. Sharing the
     * scheduler is mandatory: `withContext(ioDispatcher)` inside the service body would
     * otherwise throw `IllegalStateException: Detected use of different schedulers` because
     * `runTest` creates its own [kotlinx.coroutines.test.TestCoroutineScheduler] and rejects
     * dispatches from a foreign one.
     */
    private fun newService(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler): SdefIndexService {
        val dispatcher = StandardTestDispatcher(scheduler)
        val scope = TestScope(dispatcher)
        return SdefIndexService(serviceScope = scope, ioDispatcher = dispatcher)
    }

    @Test
    fun ingestEmptySuiteReturnsSuccessWithZeroCommandsIndexed() =
        runTest {
            val service = newService(testScheduler)
            val xml = SyntheticSuiteFixtures.emptySuiteXml()
            val file = SyntheticSuiteFixtures.writeToTempFile("empty", xml)

            val result: IngestResult = service.ingest("Empty", file)

            assertTrue(result is IngestResult.Success, "expected Success for empty-suite ingest, got $result")
            result as IngestResult.Success
            assertEquals(1, result.suitesIngested)
            assertEquals(0, result.commandsIndexed)
        }

    @Test
    fun ingestMusicSuitePlacesPlayCommandInIndex() =
        runTest {
            val service = newService(testScheduler)
            val xml = SyntheticSuiteFixtures.musicAppPlayCommandXml()
            val file = SyntheticSuiteFixtures.writeToTempFile("music", xml)

            val result: IngestResult = service.ingest("Music", file)

            assertTrue(result is IngestResult.Success, "expected Success, got $result")
            val snapshot: SdefIndexSnapshot = service.snapshot()
            assertTrue(
                snapshot.isApplicationCommand("Music", "play"),
                "Music play command must be present in app-scoped command index after ingest",
            )
            // Class index also populated for "track".
            val musicClasses = snapshot.applicationNameToClassNameSet["Music"]
            assertNotNull(musicClasses, "Music application must have an entry in applicationNameToClassNameSet")
            assertTrue("track" in (musicClasses ?: emptySet()), "Music track class must be present")
            assertTrue(
                snapshot.isApplicationProperty("Music", "name"),
                "Music track name property must be present in app-scoped property index after ingest",
            )
        }

    @Test
    fun ingestMusicSuiteAllowsAppleDoctypeWithoutLoadingExternalDtd() =
        runTest {
            val service = newService(testScheduler)
            val xml = SyntheticSuiteFixtures.musicAppPlayCommandWithAppleDoctypeXml()
            val file = SyntheticSuiteFixtures.writeToTempFile("music-doctype", xml)

            val result: IngestResult = service.ingest("Music", file)

            assertTrue(result is IngestResult.Success, "expected Success for Apple SDEF DOCTYPE, got $result")
            val snapshot: SdefIndexSnapshot = service.snapshot()
            assertTrue(
                snapshot.isApplicationCommand("Music", "play"),
                "Music play command must be indexed when the SDEF declares Apple's DTD",
            )
        }

    @Test
    fun ingestMusicSuiteSkipsXIncludeWithoutHref() =
        runTest {
            val service = newService(testScheduler)
            val xml = SyntheticSuiteFixtures.musicAppPlayCommandWithXIncludeXml(includeHref = null)
            val file = SyntheticSuiteFixtures.writeToTempFile("music-missing-xinclude-href", xml)

            val result: IngestResult = service.ingest("Music", file)

            assertTrue(result is IngestResult.Success, "missing XInclude href must not crash ingest, got $result")
            assertTrue(
                service.snapshot().isApplicationCommand("Music", "play"),
                "Music play command must still be indexed when a malformed XInclude is skipped",
            )
        }

    @Test
    fun ingestMusicSuiteSkipsRecursiveXIncludeCycle() =
        runTest {
            val service = newService(testScheduler)
            val firstFile = java.io.File.createTempFile("synthetic-music-include-cycle-first-", ".sdef")
            val secondFile = java.io.File.createTempFile("synthetic-music-include-cycle-second-", ".sdef")
            firstFile.deleteOnExit()
            secondFile.deleteOnExit()
            firstFile.writeText(SyntheticSuiteFixtures.musicAppPlayCommandWithXIncludeXml(secondFile.absolutePath))
            secondFile.writeText(SyntheticSuiteFixtures.musicAppPlayCommandWithXIncludeXml(firstFile.absolutePath))

            val result: IngestResult = service.ingest("Music", firstFile)

            assertTrue(result is IngestResult.Success, "recursive XInclude cycle must not crash ingest, got $result")
            assertTrue(
                service.snapshot().isApplicationCommand("Music", "play"),
                "Music play command must still be indexed when a recursive XInclude is skipped",
            )
        }

    @Test
    fun ingestScriptingAdditionsPlacesDoShellScriptInStdCommandIndex() =
        runTest {
            val service = newService(testScheduler)
            val xml = SyntheticSuiteFixtures.standardAdditionsMinimalXml()
            val file = SyntheticSuiteFixtures.writeToTempFile("std-additions", xml)

            // Apple SDEF rule: applicationName must equal SCRIPTING_ADDITIONS_LIBRARY
            // (= "Scripting Additions" per ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY) to
            // trigger the std-command-set population branch in parseDictionaryFile.
            val result: IngestResult = service.ingest("Scripting Additions", file)

            assertTrue(result is IngestResult.Success, "expected Success, got $result")
            val snapshot: SdefIndexSnapshot = service.snapshot()
            assertTrue(
                snapshot.isStdCommand("do shell script"),
                "do shell script must be present in std command index after Scripting Additions ingest",
            )
            assertTrue(
                snapshot.isStdLibClass("application"),
                "application class must be present in std class index after Scripting Additions ingest",
            )
        }

    @Test
    fun snapshotReturnsDefensiveImmutableCopy() =
        runTest {
            val service = newService(testScheduler)
            val firstFile =
                SyntheticSuiteFixtures.writeToTempFile(
                    "music-snapshot-first",
                    SyntheticSuiteFixtures.musicAppPlayCommandXml(),
                )
            service.ingest("Music", firstFile)
            val firstSnapshot: SdefIndexSnapshot = service.snapshot()
            assertTrue(firstSnapshot.isApplicationCommand("Music", "play"))

            // Ingest a different app; original snapshot must NOT reflect the new state — it is an
            // immutable defensive copy taken at the time of the snapshot() call.
            val secondFile =
                SyntheticSuiteFixtures.writeToTempFile(
                    "music-snapshot-second-app",
                    SyntheticSuiteFixtures.standardAdditionsMinimalXml(),
                )
            service.ingest("Scripting Additions", secondFile)
            assertTrue(
                firstSnapshot.isApplicationCommand("Music", "play"),
                "original snapshot must still report Music/play",
            )
            assertFalse(
                firstSnapshot.isStdCommand("do shell script"),
                "original snapshot must NOT contain commands ingested after it was taken",
            )

            // The fresh snapshot reflects the cumulative state.
            val secondSnapshot = service.snapshot()
            assertTrue(secondSnapshot.isStdCommand("do shell script"))
            assertTrue(secondSnapshot.isApplicationCommand("Music", "play"))
        }

    @Test
    fun ingestNonExistentFileReturnsFailedWithCause() =
        runTest {
            val service = newService(testScheduler)
            val missing = java.io.File("/tmp/__phase-04-05-this-file-does-not-exist__.sdef")
            assertFalse(missing.exists(), "preflight: temp path must not exist for this test")

            val result: IngestResult = service.ingest("MissingApp", missing)

            // parseDictionaryFile returns false for a non-existent file (IOException caught inside;
            // logs a warning and returns false). The suspend ingest wrapper interprets that as Failed.
            assertTrue(result is IngestResult.Failed, "expected Failed for missing file, got $result")
        }

    /**
     * Sealed-type exhaustive-`when` compile gate. If a future plan adds a new variant to
     * [IngestResult] without updating callers, this test will fail to compile (compile-time
     * regression for the D-05 sealed contract).
     */
    @Test
    fun ingestResultExhaustiveWhenCompiles() =
        runTest {
            val service = newService(testScheduler)
            val file =
                SyntheticSuiteFixtures.writeToTempFile(
                    "exhaustive-when",
                    SyntheticSuiteFixtures.emptySuiteXml(),
                )

            val message: String =
                when (val result = service.ingest("ExhaustiveWhen", file)) {
                    is IngestResult.Success -> "ok (${result.suitesIngested}/${result.commandsIndexed})"
                    is IngestResult.Partial -> "partial (${result.suitesIngested}, skipped=${result.skipped})"
                    is IngestResult.Failed -> "failed (${result.reason})"
                }
            assertTrue(message.startsWith("ok"))
        }

    /**
     * Sealed-type exhaustive-`when` compile gate for [LookupResult]. Same regression-lock
     * pattern as [ingestResultExhaustiveWhenCompiles].
     */
    @Test
    fun lookupResultExhaustiveWhenCompiles() {
        val example: LookupResult = LookupResult.Miss
        val description: String =
            when (example) {
                LookupResult.Hit -> "hit"
                LookupResult.Miss -> "miss"
                LookupResult.Stale -> "stale"
            }
        assertEquals("miss", description)
    }
}
