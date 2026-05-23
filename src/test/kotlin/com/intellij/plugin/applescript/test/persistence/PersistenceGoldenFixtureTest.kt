package com.intellij.plugin.applescript.test.persistence

import com.intellij.openapi.util.JDOMUtil
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService.PersistedState
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Persistence wire-format golden-fixture regression fence.
 *
 * SDEF-13 / D-14 / D-16: the v1.0 persistence schema (`AppleScriptSystemDictionaryRegistryService`
 * cache: 3 frozen fields on `DictionaryInfo.State` plus 2 collection wrappers on `PersistedState`)
 * is committed to disk as `appleScriptCachedDictionariesInfo.xml` under `RoamingType.PER_OS` and
 * cannot be renamed / type-changed without losing every existing user's cache on upgrade.
 *
 * This test reads a hand-crafted v1.0 fixture, deserialises it through `XmlSerializer`,
 * re-serialises the in-memory state, and asserts byte-equality against the fixture.
 * Any accidental `@JvmField` / `@Tag` / `@OptionTag` / `@Attribute` / `@CollectionBean`
 * / `@AbstractCollection` annotation drift during the v1.1 data-model refactor
 * (Phase 2 onwards) trips this test on the next run — the regression fence required
 * BEFORE any code touches `PersistedState` or `DictionaryInfo.State`.
 *
 * Extends `BasePlatformTestCase` (not `@Test`) to match the project's existing
 * baseline ([com.intellij.plugin.applescript.test.parsing.ParserRegressionTest]).
 */
class PersistenceGoldenFixtureTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(PERSISTENCE_DIR).absolutePath

    fun testV1_0_StateLoadsCleanly() {
        val fixtureFile = File(testDataPath, "v1.0.xml")
        assertTrue("Fixture missing: ${fixtureFile.absolutePath}", fixtureFile.exists())
        val fixtureBytes = fixtureFile.readBytes()
        assertTrue("Fixture is empty: ${fixtureFile.absolutePath}", fixtureBytes.isNotEmpty())

        // Load root <application>, find <component name="AppleScriptSystemDictionaryRegistryComponent">.
        val rootElement = JDOMUtil.load(String(fixtureBytes, StandardCharsets.UTF_8))
        val componentElement = rootElement.children
            .firstOrNull { child ->
                child.name == "component" &&
                    child.getAttributeValue("name") == "AppleScriptSystemDictionaryRegistryComponent"
            }
            ?: error("Component element not found in fixture: ${fixtureFile.absolutePath}")

        // Deserialise `PersistedState` in-place.
        val state = PersistedState()
        XmlSerializer.deserializeInto(state, componentElement)

        // Sanity-assert the in-memory state matches the fixture body.
        assertTrue(
            "Expected 2-3 dictionariesInfo entries, got ${state.dictionariesInfo.size}",
            state.dictionariesInfo.size in 2..3,
        )
        assertEquals(mutableListOf("Calculator", "Stickies"), state.notScriptableApplications)

        // Re-serialise the in-memory state and byte-compare against the fixture.
        val emittedComponent = XmlSerializer.serialize(state).apply {
            setAttribute("name", "AppleScriptSystemDictionaryRegistryComponent")
            name = "component"
        }
        val emittedRoot = Element("application").apply { addContent(emittedComponent) }
        val serializedBytes = JDOMUtil.write(emittedRoot).toByteArray(StandardCharsets.UTF_8)

        assertEquals(
            "Persistence wire format drifted — D-16 invariant breach. " +
                "If this assertion fires, an annotation on PersistedState or DictionaryInfo.State " +
                "changed since the v1.0 schema was frozen. Revert the annotation change OR " +
                "introduce a migration path before regenerating the fixture.",
            String(fixtureBytes, StandardCharsets.UTF_8).trim(),
            String(serializedBytes, StandardCharsets.UTF_8).trim(),
        )
    }

    companion object {
        private const val PERSISTENCE_DIR = "src/test/resources/testData/persistence"
    }
}
