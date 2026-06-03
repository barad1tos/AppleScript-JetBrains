package com.intellij.plugin.applescript.test.service

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.plugin.applescript.lang.ide.sdef.SdefFileTypeRegistrar
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * Phase 4 SERVICE-01 (plan 04-01) unit test for [SdefFileTypeRegistrar].
 *
 * Extends [BasePlatformTestCase] because [SdefFileTypeRegistrar.register] calls
 * `ApplicationManager.getApplication().runWriteAction { FileTypeManager.associateExtension(...) }`,
 * which requires a real Application instance. The PlatformTestCase-style boot is necessary for
 * the FileTypeManager lookup to succeed; the test does NOT use `myFixture`.
 *
 * Why not a fully hermetic test (no BasePlatformTestCase): [register] is pure Platform-API
 * delegation. There is no internal logic to test in isolation. The hermetic-test recipe
 * (RECURRING_PITFALLS.md Pattern I + Phase 3 CoroutineColdStartTest) applies to the
 * `SdefIndexService.ingest()` suspend seam in Wave 5, where the suspend boundary IS the
 * unit-test seam — not to file-type registration.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SdefFileTypeRegistrarTest : BasePlatformTestCase() {
    /**
     * Calling [SdefFileTypeRegistrar.register] twice succeeds without throwing.
     * `FileTypeManager.associateExtension` is documented as a no-op when the association
     * already exists, so the second call must be safe.
     */
    fun testRegisterIsIdempotent() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            @Suppress("UnusedPrivateProperty")
            val scope = TestScope(dispatcher)
            val registrar = SdefFileTypeRegistrar(serviceScope = scope, edtDispatcher = dispatcher)

            registrar.register()
            registrar.register()

            // After registration, .sdef MUST be recognised as XML (the production behaviour that
            // makes `LoadDictionaryAction`'s file picker filter on .sdef). Independent end-to-end
            // assertion: it does not rely on test-internal state.
            val xmlType = FileTypeManager.getInstance().getFileTypeByExtension("xml")
            assertEquals(xmlType, FileTypeManager.getInstance().getFileTypeByExtension("sdef"))
        }

    /**
     * [SdefFileTypeRegistrar.getInstance] resolves the Platform-registered Light Service
     * (auto-discovered via the `@Service(Service.Level.APP)` annotation — no plugin.xml entry).
     */
    fun testGetInstanceReturnsRegisteredService() {
        val service = SdefFileTypeRegistrar.getInstance()
        assertNotNull(service)
    }
}
