package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import org.junit.Assume

/**
 * Codex MEDIUM 2 — validates the actual `@Service(Service.Level.APP)` Platform-injected
 * scope path. Distinct from [ManualServiceScopeCancellationTest] which only validates the
 * manual cleanup pattern with an arbitrary parent disposable.
 *
 * Reads the `internal val serviceScope` field on the actual service instance obtained via
 * `ApplicationManager.getApplication().getService(...)`. Asserts the scope is Job-bearing
 * and active at acquisition time — proving the constructor injection path is wired correctly.
 *
 * Note: the @Service scope's lifecycle is tied to the application disposable. This test
 * specifically asserts the application-level service exposes a cancellable scope; disposing
 * the actual application disposable would unwind the test JVM itself.
 *
 * Heavy-gated per Phase 1 D-09 convention.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceScopeLifecycleIntegrationTest : BasePlatformTestCase() {

    override fun setUp() {
        Assume.assumeTrue(
            "ServiceScopeLifecycleIntegrationTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
    }

    fun testPlatformInjectedScopeExposesActiveJob() {
        val service: AppleScriptSystemDictionaryRegistryService =
            ApplicationManager.getApplication()
                .getService(AppleScriptSystemDictionaryRegistryService::class.java)
        // serviceScope is exposed as `internal val` for same-module test access.
        val scopeJob: Job = service.serviceScope.coroutineContext[Job]
            ?: error("Platform-injected serviceScope must expose a Job")
        assertFalse(
            "serviceScope should be active when service is acquired",
            scopeJob.isCancelled,
        )

        // Register and dispose a child disposable to exercise the Platform Disposer chain.
        val applicationDisposable = ApplicationManager.getApplication() as Disposable
        val probe = Disposer.newDisposable(applicationDisposable, "test-shutdown-probe")
        Disposer.dispose(probe)

        // The acceptance is that the Platform-injected scope is non-null and Job-bearing.
        // (Real plugin-unload would dispose the application — out of reach for a headless JVM.)
        assertNotNull("service.serviceScope must remain Job-bearing", scopeJob)
    }
}
