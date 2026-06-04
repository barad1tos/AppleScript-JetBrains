package com.intellij.plugin.applescript.smoke

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import java.io.File

private const val FIXTURE_ROOT_PROP: String = "applescript.smoke.fixtureRoot"

internal val SMOKE_LOG: Logger = Logger.getInstance("#${AppleScriptSmokeStarter::class.java.name}")

/**
 * Headless smoke command entry point.
 * Delegates project bootstrap and assertions to [AppleScriptSmokeRunner].
 * Invoked by starting the IDE with the `applescript-smoke` command. Reads fixture root from
 * `-Dapplescript.smoke.fixtureRoot=<path>` (set by the `runIdeHeadlessSmoke` Gradle task to
 * `src/test/resources/testData/runIde`).
 */
class AppleScriptSmokeStarter : ApplicationStarter {
    override fun main(args: List<String>) {
        val fixtureRoot = System.getProperty(FIXTURE_ROOT_PROP)
        val fixtureDir = fixtureDirectoryOrExit(fixtureRoot) ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val runner = AppleScriptSmokeRunner(fixtureRoot.orEmpty(), fixtureDir)
            exitSmoke(runner.run())
        }
    }

    private fun fixtureDirectoryOrExit(fixtureRoot: String?): File? {
        val fixtureDir = fixtureRoot?.takeIf { it.isNotBlank() }?.let(::File)
        val failureMessage =
            when {
                fixtureRoot.isNullOrBlank() -> "-D$FIXTURE_ROOT_PROP not set"
                fixtureDir?.isDirectory != true -> "fixture root is not a directory: $fixtureRoot"
                else -> null
            }
        if (failureMessage != null) {
            SMOKE_LOG.error("[applescript-smoke] FAIL: $failureMessage")
            exitSmoke(1)
        }

        return fixtureDir?.takeIf { failureMessage == null }
    }

    private fun exitSmoke(code: Int) {
        try {
            ApplicationManager.getApplication().exit(true, true, false, code)
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: IllegalStateException) {
            SMOKE_LOG.warn("ApplicationManager.exit threw", exception)
        } catch (exception: IllegalArgumentException) {
            SMOKE_LOG.warn("ApplicationManager.exit threw", exception)
        }
    }
}
