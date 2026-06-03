package com.intellij.plugin.applescript.test.sdef

import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.SuiteImpl
import com.intellij.psi.xml.XmlTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * SDEF-11 regression lock for `SuiteImpl.addCommand` (moved from plan
 * 02-06 to 02-04 per checker warning 5 — the fix lives in the same
 * method this plan restructures via D-01 Hybrid wiring).
 *
 * Pre-fix shape (`SuiteImpl.kt:96`) was:
 * ```
 * return commandDefinitions.add(command) && dictionaryCommandMap.put(name, command) != null
 * ```
 * which returned `false` on first insert (Map.put returns the OLD value
 * — `null` on first insert — so `null != null` is `false`) and `true`
 * on overwrite — the exact INVERSE of the documented contract.
 *
 * The correct convention (matching `ApplicationDictionaryImpl.addCommand`
 * at line 191) is `== null` — true on first insert, false on duplicate.
 *
 * Both tests use JDK dynamic proxies for `ApplicationDictionary`, `XmlTag`
 * and `AppleScriptCommand` so `SuiteImpl` can be exercised without spinning
 * up the full IntelliJ Platform fixture (BasePlatformTestCase ~30 s + 247
 * application scans). The proxy stubs intercept the minimum surface
 * `SuiteImpl.addCommand` touches (`AppleScriptCommand.getName`,
 * `AppleScriptCommand.getCode`) and otherwise return `null`.
 */
class SuiteAddCommandTest {
    @Test
    fun testFirstAddReturnsTrue() {
        val suite = buildSuite()
        val cmd = stubCommand(name = "play", code = "play")
        assertTrue(
            "First insert of a (name, command) pair must return true (matches ApplicationDictionaryImpl convention)",
            suite.addCommand(cmd),
        )
    }

    @Test
    fun testDuplicateAddReturnsFalse() {
        val suite = buildSuite()
        val cmd = stubCommand(name = "play", code = "play")
        suite.addCommand(cmd)
        assertFalse(
            "Duplicate insert of an existing name must return false",
            suite.addCommand(cmd),
        )
    }

    @Test
    fun testDifferentNamesBothReturnTrue() {
        val suite = buildSuite()
        assertTrue(suite.addCommand(stubCommand(name = "play", code = "play")))
        assertTrue(suite.addCommand(stubCommand(name = "stop", code = "stop")))
    }

    private fun buildSuite(): SuiteImpl =
        SuiteImpl(
            dictionary = stubDictionary(),
            code = "core",
            name = "Standard Suite",
            hidden = false,
            description = null,
            xmlTagSuite = stubXmlTag(),
        )

    private fun stubDictionary(): ApplicationDictionary =
        Proxy.newProxyInstance(
            ApplicationDictionary::class.java.classLoader,
            arrayOf(ApplicationDictionary::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "ApplicationDictionaryStub@${System.identityHashCode(proxy)}"
                else -> null
            }
        } as ApplicationDictionary

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

    private fun stubCommand(
        name: String,
        code: String,
    ): AppleScriptCommand =
        Proxy.newProxyInstance(
            AppleScriptCommand::class.java.classLoader,
            arrayOf(AppleScriptCommand::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "getName" -> name
                "getCode" -> code
                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "AppleScriptCommandStub($name)"
                else -> null
            }
        } as AppleScriptCommand
}
