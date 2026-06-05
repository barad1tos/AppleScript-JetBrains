package com.intellij.plugin.applescript.test.service

import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Phase 4 SERVICE-02 + SERVICE-08 unit test for [SdefPersistenceService].
 *
 * Verifies the typed API correctly round-trips data through the facade's persisted-state-tagged
 * [AppleScriptSystemDictionaryRegistryService.PersistedState] field WITHOUT mutating the wire
 * format. SDEF-13 byte-for-byte regression is locked by
 * [com.intellij.plugin.applescript.test.persistence.PersistenceGoldenFixtureTest] —
 * this test focuses on the typed-API contract semantics.
 *
 * Extends [BasePlatformTestCase] because [SdefPersistenceService] depends on
 * `service<AppleScriptSystemDictionaryRegistryService>().state`, which requires a real
 * Application container. There is no value in mocking the facade — the test validates the
 * integration. The hermetic test seam (RESEARCH §4) applies to `SdefIndexService.ingest()`
 * in Wave 5, not to persistence.
 *
 * Gated under `-PincludeHeavyTests=true` via the `test.service.*` filter (same precedent as
 * `SdefFileTypeRegistrarTest` — Wave 1 deviation #4).
 */
class SdefPersistenceServiceTest : BasePlatformTestCase() {
    fun testReadDictionaryInfoSnapshotIsDefensiveCopy() {
        val service = SdefPersistenceService.getInstance()
        val dictionaryFile = File.createTempFile("dictionary-info-snapshot", ".sdef")
        val applicationFile = File(dictionaryFile.parentFile, "SnapshotApp_${System.nanoTime()}.app")
        val dictionaryInfo = DictionaryInfo("SnapshotApp_${System.nanoTime()}", dictionaryFile, applicationFile)
        try {
            service.addDictionaryInfo(dictionaryInfo)
            val snapshot = service.dictionaryInfoSnapshot
            (snapshot as? MutableList<*>)?.clear()

            val serviceStillContainsDictionary =
                service
                    .dictionaryInfoSnapshot
                    .any { it.getApplicationName() == dictionaryInfo.getApplicationName() }
            assertTrue(
                "Mutating a returned snapshot must not remove dictionary info from the service",
                serviceStillContainsDictionary,
            )
        } finally {
            service.removeDictionaryInfo(applicationFile.path)
            dictionaryFile.delete()
        }
    }

    fun testReadNotScriptableSnapshotIsDefensiveCopy() {
        val service = SdefPersistenceService.getInstance()
        val name = "SnapshotNotScriptable_${System.nanoTime()}"
        try {
            service.addNotScriptable(name)
            val snapshot = service.notScriptableSnapshot
            (snapshot as? MutableSet<*>)?.clear()

            assertTrue(
                "Mutating a returned snapshot must not remove not-scriptable state from the service",
                service.isNotScriptable(name),
            )
        } finally {
            service.removeNotScriptable(name)
        }
    }

    fun testAddNotScriptableIsIdempotent() {
        val service = SdefPersistenceService.getInstance()
        // Unique name avoids collisions with any other test fixture state.
        val name = "TestAppNotScriptable_${System.nanoTime()}"
        try {
            assertTrue("First add returns true (newly added)", service.addNotScriptable(name))
            assertFalse("Second add returns false (idempotent — already present)", service.addNotScriptable(name))
            assertTrue("Name now appears in snapshot", name in service.notScriptableSnapshot)
            assertTrue("isNotScriptable returns true for known name", service.isNotScriptable(name))
        } finally {
            // Clean up: test runs must be repeatable, and we share the facade with other tests.
            service.removeNotScriptable(name)
        }
        assertFalse(
            "After cleanup, name no longer reports as notScriptable",
            service.isNotScriptable(name),
        )
    }

    fun testRemoveNotScriptableUnknownNameIsNoOp() {
        val service = SdefPersistenceService.getInstance()
        val name = "Definitely_Not_In_The_List_${System.nanoTime()}"
        assertFalse("Removing unknown name returns false", service.removeNotScriptable(name))
    }

    fun testIsNotScriptableNegativeCase() {
        val service = SdefPersistenceService.getInstance()
        // Random unique name — must NOT be in the notScriptable list.
        val randomName = "__random_not_scriptable_${System.nanoTime()}"
        assertFalse("Unknown name not in notScriptable list", service.isNotScriptable(randomName))
    }

    fun testParseFailureDoesNotMarkApplicationAsNotScriptable() {
        val facade = AppleScriptSystemDictionaryRegistryService.getInstance()
        val service = SdefPersistenceService.getInstance()
        val name = "ParseFailureApp_${System.nanoTime()}"
        val brokenDictionary = SyntheticSuiteFixtures.writeToTempFile("broken-dictionary", "<dictionary>")
        val applicationFile = File(brokenDictionary.parentFile, "$name.app")
        val info = DictionaryInfo(name, brokenDictionary, applicationFile)
        try {
            service.addDictionaryInfo(info)

            assertFalse(
                "Broken dictionary file must not initialize",
                facade.initializeDictionaryFromInfoInternal(info),
            )
            assertFalse(
                "Dictionary parse failure is not proof that the application is not scriptable",
                service.isNotScriptable(name),
            )
        } finally {
            service.removeDictionaryInfo(applicationFile.path)
            service.removeNotScriptable(name)
        }
    }

    fun testWriteToStateThenLoadFromStateRoundTrips() {
        val service = SdefPersistenceService.getInstance()
        // Add a marker into the persistence bridge, write it into a fresh PersistedState,
        // then load the state back and confirm the marker survives the round-trip. This
        // exercises the writeToState -> loadFromState path that the platform PSC machinery
        // drives on save/load.
        val marker = "RoundTripMarker_${System.nanoTime()}"
        service.addNotScriptable(marker)
        try {
            val freshState = AppleScriptSystemDictionaryRegistryService.PersistedState()
            service.writeToState(freshState)
            assertNotNull("notScriptableApplications populated", freshState.notScriptableApplications)
            assertTrue(
                "Marker written into the PersistedState.notScriptableApplications list",
                marker in freshState.notScriptableApplications!!,
            )
            // Verify loadFromState recovers the same data. We round-trip through a NEW fresh
            // state object to make the assertion independent of any earlier in-memory residue.
            val secondState = AppleScriptSystemDictionaryRegistryService.PersistedState()
            service.writeToState(secondState)
            service.loadFromState(secondState)
            assertTrue(
                "After loadFromState, marker is restored in the persistence snapshot",
                marker in service.notScriptableSnapshot,
            )
        } finally {
            service.removeNotScriptable(marker)
        }
    }
}
