package com.intellij.plugin.applescript.test.sdef

import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandImpl
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameterData
import com.intellij.plugin.applescript.lang.sdef.CommandParameterImpl
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.psi.sdef.impl.ApplicationDictionaryImpl
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.lang.reflect.Proxy

private const val PLAY_COMMAND_NAME = "play"
private const val EMPTY_DICTIONARY_XML = "<dictionary title=\"TestApp\"></dictionary>"

/**
 * SDEF-02 regression fence for the D-02 TODO closure at
 * `ApplicationDictionaryImpl:177`.
 *
 * The pre-fix shape returned `0..1` from `findAllCommandsWithName` (HashMap-keyed
 * by name only — overloads collapsed). The fix at plan 02-04 introduces a
 * secondary `dictionaryCommandListMap: MutableMap<String, MutableList<…>>` keyed
 * by name → list-of-commands. Dedupe inside the list is by `CommandData`
 * structural equality so:
 *
 *  - Two genuinely-overloaded commands (same name, DIFFERENT parameter
 *    signatures) co-exist as 2 entries in the list — `testOverloadReturnsTwo`.
 *  - Re-ingest of a structurally-equal command (same name + code + parameters
 *    + result + description) collapses to 1 entry — `testDuplicateInsertDedupes`.
 *  - First-insert addCommand still returns true (matches
 *    ApplicationDictionaryImpl.addCommand contract from line 191) —
 *    `testFirstInsertReturnsTrue`.
 *
 * Uses `BasePlatformTestCase` (slower than the pure-JUnit pattern in
 * `LeafDataClassTest` / `SuiteAddCommandTest`) because `ApplicationDictionaryImpl`
 * extends `FakePsiElement` and its constructor wants a real `Project` + `XmlFile`
 * to feed `SdefParser.parse`. We supply an empty-`<dictionary/>` XmlFile so the
 * parser no-ops and we can then drive `addCommand` directly with stub commands.
 */
class ApplicationDictionaryOverloadTest : BasePlatformTestCase() {
    fun testFirstInsertReturnsTrue() {
        val dict = buildDictionary()
        val cmd = newPlayCommand(parameters = listOf("track"))
        assertTrue("First insert must return true", dict.addCommand(cmd))
    }

    fun testDuplicateInsertDedupes() {
        val dict = buildDictionary()
        val cmd1 = newPlayCommand(parameters = listOf("track"))
        val cmd2 = newPlayCommand(parameters = listOf("track"))
        // Both have structurally-equal CommandData → second insert is a
        // duplicate by name AND by structural equality.
        assertTrue(dict.addCommand(cmd1))
        // Second insert: same name → returns false (duplicate-by-name contract).
        assertFalse("Duplicate name insert must return false", dict.addCommand(cmd2))
        // List-keyed map: structural-equality dedupe collapses to 1 entry.
        val hits = dict.findAllCommandsWithName(PLAY_COMMAND_NAME)
        assertEquals("Structurally-equal commands must dedupe to 1 entry", 1, hits.size)
    }

    fun testOverloadReturnsTwo() {
        val dict = buildDictionary()
        // Two commands with same name but DIFFERENT parameter signatures —
        // CommandData.equals returns false → list keeps both entries.
        val cmd1 = newPlayCommand(parameters = listOf("track"))
        val cmd2 = newPlayCommand(parameters = listOf("track", "from"))
        dict.addCommand(cmd1)
        dict.addCommand(cmd2)
        val hits = dict.findAllCommandsWithName(PLAY_COMMAND_NAME)
        assertEquals(
            "Genuinely overloaded commands (different parameter lists) must keep both entries",
            2,
            hits.size,
        )
    }

    fun testFindAllCommandsWithNameEmptyOnMiss() {
        val dict = buildDictionary()
        assertTrue("Missing name must return empty list", dict.findAllCommandsWithName("missing").isEmpty())
    }

    fun testCommandCocoaClassIsParsedFromSdef() {
        val dict =
            buildDictionary(
                """
                <dictionary title="TestApp">
                    <suite name="Test Suite" code="test">
                        <command name="close" code="clos">
                            <cocoa class="NSCloseCommand"/>
                        </command>
                    </suite>
                </dictionary>
                """.trimIndent(),
            )

        val command = dict.findAllCommandsWithName("close").single()

        assertEquals("NSCloseCommand", command.cocoaClassName)
    }

    /**
     * Build a real `ApplicationDictionaryImpl` over an empty in-memory XmlFile.
     * `SdefParser.parse` walks the empty document tree without inserting any
     * commands, leaving the dictionary ready for direct `addCommand` calls.
     */
    private fun buildDictionary(xmlText: String = EMPTY_DICTIONARY_XML): ApplicationDictionaryImpl {
        val xmlFile =
            PsiFileFactory
                .getInstance(project)
                .createFileFromText(
                    "empty.sdef",
                    com.intellij.lang.xml.XMLLanguage.INSTANCE,
                    xmlText,
                ) as XmlFile
        return ApplicationDictionaryImpl(
            project = project,
            dictionaryXmlFile = xmlFile,
            applicationName = "TestApp",
            applicationBundleFile = null,
        )
    }

    /**
     * Build a real `AppleScriptCommandImpl` with a stub Suite + the dictionary's
     * own root XmlTag. The Suite is a Proxy stub because `AppleScriptCommandImpl`
     * only needs it for `getSuite()` (not exercised by `addCommand` /
     * `findAllCommandsWithName` paths).
     */
    private fun newPlayCommand(parameters: List<String>): AppleScriptCommand {
        val suiteStub = stubSuite()
        val xmlTagStub = stubXmlTag()
        val cmd = AppleScriptCommandImpl(suiteStub, PLAY_COMMAND_NAME, PLAY_COMMAND_NAME, xmlTagStub)
        // setParameters routes through the builder so `data: CommandData` is
        // build-frozen with the right parameter list (and hashCode is stable).
        val params: List<CommandParameter> =
            parameters.map { pName ->
                CommandParameterImpl(
                    cmd,
                    CommandParameterData(name = pName, code = "----", type = "text"),
                    xmlTagStub,
                )
            }
        cmd.parameters = params
        return cmd
    }

    private fun stubSuite(): Suite =
        Proxy.newProxyInstance(
            Suite::class.java.classLoader,
            arrayOf(Suite::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "SuiteStub@${System.identityHashCode(proxy)}"
                else -> null
            }
        } as Suite

    private fun stubXmlTag(): XmlTag =
        Proxy.newProxyInstance(
            XmlTag::class.java.classLoader,
            arrayOf(XmlTag::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "XmlTagStub@${System.identityHashCode(proxy)}"
                else -> null
            }
        } as XmlTag
}
