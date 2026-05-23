package com.intellij.plugin.applescript.test.sdef

import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary
import com.intellij.plugin.applescript.lang.sdef.extensionSupported
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SDEF-06 / D-09 regression lock for the [extensionSupported] relocation.
 *
 * The companion-bound `ApplicationDictionaryImpl.extensionSupported` was a UI
 * action's reach-through into a PSI impl; v1.1 moves the predicate to a
 * top-level Kotlin function in the same package as
 * [ApplicationDictionary.SUPPORTED_DICTIONARY_EXTENSIONS] so callers no longer
 * import a PSI impl class just to ask a string question (Pattern 12).
 *
 * Behaviour is preserved verbatim: null in → false, case-insensitive contains
 * over [ApplicationDictionary.SUPPORTED_DICTIONARY_EXTENSIONS]. The
 * `supportedExtensionsAllReturnTrue` test pins the contract to the list so a
 * future addition to `SUPPORTED_DICTIONARY_EXTENSIONS` is automatically
 * exercised without touching this test.
 */
class ExtensionSupportedTest {

    @Test
    fun nullExtensionIsNotSupported() {
        assertFalse(extensionSupported(null))
    }

    @Test
    fun txtIsNotSupported() {
        assertFalse(extensionSupported("txt"))
    }

    @Test
    fun sdefIsSupported() {
        assertTrue(extensionSupported("sdef"))
    }

    @Test
    fun caseInsensitive() {
        // Uppercase, mixed-case, and lowercase all collapse via .lowercase()
        // before the list contains-check.
        assertTrue(extensionSupported("SDEF"))
        assertTrue(extensionSupported("Sdef"))
        assertTrue(extensionSupported("APP"))
    }

    @Test
    fun emptyStringIsNotSupported() {
        // Pattern L: source-regex regression lock for the defensive branch —
        // the predicate must not collapse `""` into a supported extension
        // even if the list ever gains a `""` entry by accident.
        assertFalse(extensionSupported(""))
    }

    @Test
    fun supportedExtensionsAllReturnTrue() {
        ApplicationDictionary.SUPPORTED_DICTIONARY_EXTENSIONS.forEach {
            assertTrue("extension '$it' should be supported", extensionSupported(it))
        }
    }
}
