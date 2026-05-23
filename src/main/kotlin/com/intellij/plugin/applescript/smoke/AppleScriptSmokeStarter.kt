package com.intellij.plugin.applescript.smoke

import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import kotlin.system.exitProcess

/**
 * Headless smoke entry point for SDEF-14. Boots IDEA, opens fixtures, asserts three Phase 8
 * invariants (composite 2-token fallback, BASIC completion on `play `, and `WEAK_WARNING`
 * (not `WARNING`) severity for unresolved app references).
 *
 * Invoked via `-Didea.ApplicationStarter.command=applescript-smoke`. Reads fixture root
 * from `-Dapplescript.smoke.fixtureRoot=<path>`.
 *
 * NOTE (RED phase): the assertion bodies are intentionally absent — this file currently
 * registers the starter SPI and command name; the three assertions are implemented in the
 * follow-up GREEN commit. Boot today causes a fail-fast exit code 1 so the wired Gradle
 * task surfaces the missing implementation rather than passing silently.
 */
class AppleScriptSmokeStarter : ApplicationStarter {

    override val commandName: String = COMMAND_NAME

    override fun main(args: List<String>) {
        val log = Logger.getInstance("#${AppleScriptSmokeStarter::class.java.name}")
        log.error("[applescript-smoke] FAIL: assertions not yet implemented (RED gate placeholder)")
        exitProcess(1)
    }

    private companion object {
        const val COMMAND_NAME: String = "applescript-smoke"
    }
}
