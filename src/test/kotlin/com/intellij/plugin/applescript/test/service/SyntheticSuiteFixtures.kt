package com.intellij.plugin.applescript.test.service

import java.io.File

/**
 * Phase 4 SERVICE-09 + CLEANUP-03 enabler: shared synthetic SDEF XML fixtures.
 *
 * Used by [SdefIndexServiceTest] (this phase) + v1.5 TESTPORT-04 + v1.6 CLEANUP-03 (the
 * planned promotion of heavy tests to default CI).
 *
 * Hermetic — no `/Applications` scan, no `sdef` CLI invocation, no [BasePlatformTestCase].
 *
 * RESEARCH §4 Assumption A2 closure: the Phase 2
 * [com.intellij.plugin.applescript.lang.sdef.Suite] type is an interface (NOT a data class)
 * and the live XML pipeline consumes raw JDOM `Element` instances, not pre-built `Suite`
 * objects. This object accordingly produces temp `.sdef` XML files matching the real Apple
 * SDEF schema — the same input shape that production [com.intellij.plugin.applescript.lang.ide.sdef.SdefIndexService.parseDictionaryFile]
 * consumes during init. Documented as a Wave 5 deviation from the plan's `List<Suite>` sketch.
 *
 * Fixture XML schemas follow the Apple SDEF reference (`/System/Library/DTDs/sdef.dtd`).
 * Each fixture writes a `<dictionary><suite>...</suite></dictionary>` document with the
 * minimum elements needed to exercise the corresponding index path.
 */
object SyntheticSuiteFixtures {

    /**
     * Minimal Standard Additions suite with one command (`do shell script`). Triggers the
     * `parseSuiteElementForScriptingAdditions` branch in [SdefIndexService.parseDictionaryFile]
     * because the applicationName must equal `ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY`
     * (= "Standard Additions").
     */
    fun standardAdditionsMinimalXml(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <dictionary title="Standard Additions Terminology">
            <suite name="Standard Additions" code="stda" description="Standard Additions">
                <command name="do shell script" code="systshel" description="Execute a shell script">
                    <direct-parameter type="text"/>
                </command>
            </suite>
        </dictionary>
    """.trimIndent()

    /**
     * Music.app-shaped suite for app-command tests. One command (`play`) + one class (`track`).
     * The applicationName at parseDictionaryFile time MUST be "Music" to populate the
     * applicationNameToCommandNameSet entry with key "Music".
     */
    fun musicAppPlayCommandXml(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <dictionary title="Music Terminology">
            <suite name="Music Suite" code="musc" description="Classes and commands for Music">
                <command name="play" code="hookplay" description="Play the current track"/>
                <class name="track" code="cTrk" description="A track in a playlist"/>
            </suite>
        </dictionary>
    """.trimIndent()

    /** Suite with no commands — edge case for empty-suite handling. */
    fun emptySuiteXml(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <dictionary title="Empty">
            <suite name="Empty" code="empt" description="Empty suite for edge cases"/>
        </dictionary>
    """.trimIndent()

    /** Writes [xml] into a fresh temp file with `.sdef` suffix; the file is delete-on-exit. */
    fun writeToTempFile(name: String, xml: String): File {
        val file = File.createTempFile("synthetic-${name}-", ".sdef")
        file.deleteOnExit()
        file.writeText(xml)
        return file
    }
}
