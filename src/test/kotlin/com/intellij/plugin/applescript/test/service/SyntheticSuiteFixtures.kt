package com.intellij.plugin.applescript.test.service

import java.io.File

private fun appleScriptCode(vararg chars: Char): String = chars.concatToString()

private val APPLICATION_CLASS_CODE = appleScriptCode('c', 'a', 'p', 'p')
private val DO_SHELL_SCRIPT_COMMAND_CODE = appleScriptCode('s', 'y', 's', 't', 's', 'h', 'e', 'l')
private val EMPTY_SUITE_CODE = appleScriptCode('e', 'm', 'p', 't')
private val MUSIC_PLAY_COMMAND_CODE = appleScriptCode('h', 'o', 'o', 'k', 'p', 'l', 'a', 'y')
private val MUSIC_SUITE_CODE = appleScriptCode('m', 'u', 's', 'c')
private val TASK_LIST_SUITE_CODE = appleScriptCode('t', 'a', 's', 'k')
private val TASK_LIST_SHOW_COMMAND_CODE = appleScriptCode('s', 'h', 'o', 'w')
private val TASK_LIST_MAKE_COMMAND_CODE = appleScriptCode('m', 'a', 'k', 'e')
private val TASK_LIST_NAME_PROPERTY_CODE = appleScriptCode('p', 'n', 'a', 'm')
private val TASK_LIST_TO_DO_CLASS_CODE = appleScriptCode('t', 'o', 'd', 'o')
private val TASK_LIST_LIST_CLASS_CODE = appleScriptCode('l', 'i', 's', 't')
private val TRACK_CLASS_CODE = appleScriptCode('c', 'T', 'r', 'k')
private val TRACK_NAME_PROPERTY_CODE = appleScriptCode('p', 'n', 'a', 'm')
private val STANDARD_ADDITIONS_SUITE_CODE = appleScriptCode('s', 't', 'd', 'a')

/**
 * Phase 4 SERVICE-09 + CLEANUP-03 enabler: shared synthetic SDEF XML fixtures.
 *
 * Used by [SdefIndexServiceTest] (this phase) + v1.5 TESTPORT-04 + v1.6 CLEANUP-03 (the
 * planned promotion of heavy tests to default CI).
 *
 * Hermetic — no `/Applications` scan, no `sdef` CLI invocation, no platform fixture base class.
 *
 * RESEARCH §4 Assumption A2 closure: the Phase 2
 * [com.intellij.plugin.applescript.lang.sdef.Suite] type is an interface (NOT a data class)
 * and the live XML pipeline consumes raw JDOM `Element` instances, not pre-built `Suite`
 * objects. This object accordingly produces temp `.sdef` XML files matching the real Apple
 * SDEF schema — the same input shape that production `SdefIndexService.parseDictionaryFile`
 * consumes during init. Documented as a Wave 5 deviation from the plan's `List<Suite>` sketch.
 *
 * Fixture XML schemas follow the Apple SDEF reference (`/System/Library/DTDs/sdef.dtd`).
 * Each fixture writes a `<dictionary><suite>...</suite></dictionary>` document with the
 * minimum elements needed to exercise the corresponding index path.
 */
object SyntheticSuiteFixtures {
    /**
     * Minimal Standard Additions suite with one command (`do shell script`). Triggers the
     * `parseSuiteElementForScriptingAdditions` branch in `SdefIndexService.parseDictionaryFile`
     * because the applicationName must equal `ApplicationDictionary.SCRIPTING_ADDITIONS_LIBRARY`
     * (= "Scripting Additions").
     */
    fun standardAdditionsMinimalXml(): String =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <dictionary title="Standard Additions Terminology">
            <suite name="Standard Additions" code="$STANDARD_ADDITIONS_SUITE_CODE" description="Standard Additions">
                <class name="application" code="$APPLICATION_CLASS_CODE" description="Application"/>
                <command name="do shell script" code="$DO_SHELL_SCRIPT_COMMAND_CODE" description="Execute a shell script">
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
    fun musicAppPlayCommandXml(): String =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <dictionary title="Music Terminology">
            <suite name="Music Suite" code="$MUSIC_SUITE_CODE" description="Classes and commands for Music">
                <command name="play" code="$MUSIC_PLAY_COMMAND_CODE" description="Play the current track"/>
                <class name="track" code="$TRACK_CLASS_CODE" description="A track in a playlist">
                    <property name="name" code="$TRACK_NAME_PROPERTY_CODE" type="text"/>
                </class>
            </suite>
        </dictionary>
        """.trimIndent()

    fun musicAppPlayCommandWithXIncludeXml(includeHref: String?): String {
        val includeTag =
            if (includeHref == null) {
                "<xi:include/>"
            } else {
                "<xi:include href=\"$includeHref\"/>"
            }
        return listOf(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<dictionary title=\"Music Terminology\">",
            "    <suite xmlns:xi=\"http://www.w3.org/2003/XInclude\"",
            "        name=\"Music Suite\"",
            "        code=\"$MUSIC_SUITE_CODE\"",
            "        description=\"Classes and commands for Music\">",
            "        $includeTag",
            "        <command",
            "            name=\"play\"",
            "            code=\"$MUSIC_PLAY_COMMAND_CODE\"",
            "            description=\"Play the current track\"",
            "        />",
            "    </suite>",
            "</dictionary>",
        ).joinToString(separator = "\n")
    }

    /**
     * Real macOS `sdef` output includes this Apple DTD declaration. The parser must allow the
     * declaration while still refusing external DTD/entity loading.
     */
    fun musicAppPlayCommandWithAppleDoctypeXml(): String =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE dictionary SYSTEM "file://localhost/System/Library/DTDs/sdef.dtd">
        <dictionary title="Music Terminology">
            <suite name="Music Suite" code="$MUSIC_SUITE_CODE" description="Classes and commands for Music">
                <command name="play" code="$MUSIC_PLAY_COMMAND_CODE" description="Play the current track"/>
                <class name="track" code="$TRACK_CLASS_CODE" description="A track in a playlist">
                    <property name="name" code="$TRACK_NAME_PROPERTY_CODE" type="text"/>
                </class>
            </suite>
        </dictionary>
        """.trimIndent()

    fun taskListAppXml(): String =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <dictionary title="Task List Terminology">
            <suite name="Task List Suite" code="$TASK_LIST_SUITE_CODE" description="Task list commands">
                <command name="show" code="$TASK_LIST_SHOW_COMMAND_CODE" description="Show a list">
                    <direct-parameter type="specifier"/>
                </command>
                <command name="make" code="$TASK_LIST_MAKE_COMMAND_CODE" description="Make a new item">
                    <parameter name="new" code="kocl" type="type"/>
                    <parameter name="with properties" code="prdt" type="record"/>
                    <parameter name="at" code="insh" type="specifier"/>
                </command>
                <class name="list" code="$TASK_LIST_LIST_CLASS_CODE" description="A task list">
                    <property name="name" code="$TASK_LIST_NAME_PROPERTY_CODE" type="text"/>
                </class>
                <class name="to do" plural="to dos" code="$TASK_LIST_TO_DO_CLASS_CODE" description="A task">
                    <property name="name" code="$TASK_LIST_NAME_PROPERTY_CODE" type="text"/>
                </class>
            </suite>
        </dictionary>
        """.trimIndent()

    /** Suite with no commands — edge case for empty-suite handling. */
    fun emptySuiteXml(): String =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <dictionary title="Empty">
            <suite name="Empty" code="$EMPTY_SUITE_CODE" description="Empty suite for edge cases"/>
        </dictionary>
        """.trimIndent()

    /** Writes [xml] into a fresh temp file with `.sdef` suffix; the file is delete-on-exit. */
    fun writeToTempFile(
        name: String,
        xml: String,
    ): File {
        val file = File.createTempFile("synthetic-$name-", ".sdef")
        file.deleteOnExit()
        file.writeText(xml)
        return file
    }
}
