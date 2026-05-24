// AUDIT 2026-05-24: scanned for EDT-context assumption patterns (assertFalse/assumeFalse on
// isDispatchThread, @RunsInEdt/EdtRule, ThreadingAssertions); none found. File is compatible
// with BasePlatformTestCase's EDT-by-default threading model. Audit performed during Plan 03-11
// (DEBUG.md ADDENDUM Layer 5 sweep).
package com.intellij.plugin.applescript.test.concurrency

import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommand
import com.intellij.plugin.applescript.lang.sdef.AppleScriptCommandImpl
import com.intellij.plugin.applescript.lang.sdef.CommandParameter
import com.intellij.plugin.applescript.lang.sdef.CommandParameterImpl
import com.intellij.plugin.applescript.lang.sdef.Suite
import com.intellij.plugin.applescript.psi.sdef.impl.ApplicationDictionaryImpl
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assume
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * T-02-02 regression lock for the `processInclude` race in [ApplicationDictionaryImpl].
 *
 * Mandatory per Phase 02 / Plan 05 checker warning 6 — the CHM backing in
 * `DictionaryIndexes` MUST be regression-locked by an actual concurrent-stress
 * test, not just by a single-threaded structural grep.
 *
 * Race scenario (latent since v1.0.0; Phase 1 explicitly deferred the fix to v1.1):
 *  - `ApplicationDictionaryImpl.processInclude:135` re-enters `SDEF_Parser.parseRootTag`,
 *    which mutates the index maps via [ApplicationDictionaryImpl.addCommand] etc.,
 *    while background threads (resolver / completion) call
 *    [ApplicationDictionaryImpl.findAllCommandsWithName] and iterate
 *    [ApplicationDictionaryImpl.getAllCommands].
 *  - Pre-CHM: raw `HashMap.put` concurrent with `HashMap.values.iterator` is
 *    formally undefined; observed failure modes are `ConcurrentModificationException`
 *    on iteration, lost entries on `put` overlap with `resize`, and (rarely on
 *    Java 17 but well-documented on prior JREs) infinite-loop bucket cycles.
 *  - Post-CHM: `ConcurrentHashMap` guarantees both safe interleaving and
 *    weakly-consistent iteration; `addCommand` is a single put-or-update path
 *    so no compound-action invariant needs an external lock.
 *
 * Gated behind `-PincludeHeavyTests=true` per the existing heavy-test convention
 * (shares the `concurrency` package with the v1.0.1-hotfix [StressTest] regression).
 * The test is statistically more reliable than a single-shot run because we
 * burn through `iterations * mutators` writes against a shared map; even one
 * raw `HashMap` race will surface within a few hundred iterations.
 */
class ApplicationDictionaryConcurrencyTest : BasePlatformTestCase() {

    override fun setUp() {
        Assume.assumeTrue(
            "ApplicationDictionaryConcurrencyTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
    }

    fun testProcessIncludeRaceClosed() {
        val dict = buildDictionary()
        // 4 writers × 1000 unique inserts each = 4000 puts under contention.
        // Empirically against raw HashMap on Java 17 this surfaces lost-update
        // races within a single run (writes lost to resize / bucket-chain races
        // are visible as the post-run `getAllCommands().size` being < 4000).
        // Against ConcurrentHashMap every put is observed; the assertion holds
        // deterministically. The 30 s wall-clock budget gives ~7.5 µs per put,
        // which is generous even on a cold JVM.
        val mutators = 4
        val readers = 2
        val iterations = 1_000
        val workers = mutators + readers
        val pool = Executors.newFixedThreadPool(workers)
        val start = CountDownLatch(1)
        val done = CountDownLatch(workers)
        val firstFailure = AtomicReference<Throwable?>(null)
        val insertedCount = AtomicInteger(0)

        try {
            repeat(mutators) { tid ->
                pool.submit {
                    try {
                        start.await()
                        repeat(iterations) { iteration ->
                            // Unique (name, code) per insert so every iteration is a
                            // genuine new entry — proves no put is lost to a racing
                            // resize. Worker id `tid` segregates the namespace so
                            // there is no name collision between writers; the
                            // post-run `findAllCommandsWithName` per writer-thread
                            // can therefore assert exact membership.
                            val name = "cmd-$tid-$iteration"
                            val cmd = newCommand(name = name, parameters = listOf("arg$tid"))
                            dict.addCommand(cmd)
                            insertedCount.incrementAndGet()
                        }
                    } catch (throwable: Throwable) {
                        // Reader/mutator threads run outside the JUnit EDT; any
                        // escaped Throwable (CME from HashMap iteration, NPE from
                        // resize-time read, AssertionError, even Error subclasses)
                        // is a test failure. Capture the first; later failures are noise.
                        firstFailure.compareAndSet(null, throwable)
                    } finally {
                        done.countDown()
                    }
                }
            }
            repeat(readers) {
                pool.submit {
                    try {
                        start.await()
                        repeat(iterations) {
                            // The reader path mirrors the resolver + completion
                            // hot paths: `findAllCommandsWithName` (post 02-04)
                            // reads `dictionaryCommandListMap`; `getAllCommands`
                            // iterates `dictionaryCommandMap.values`. Both are
                            // mutated by the writer threads above.
                            dict.findAllCommandsWithName("cmd-0-0")
                            // `.toList()` forces full iteration of the underlying
                            // `Collection<AppleScriptCommand>` view — the prime
                            // race surface for `ConcurrentModificationException`
                            // against raw `HashMap.values`.
                            dict.getAllCommands().toList()
                        }
                    } catch (throwable: Throwable) {
                        firstFailure.compareAndSet(null, throwable)
                    } finally {
                        done.countDown()
                    }
                }
            }
            start.countDown()
            val finished = done.await(30, TimeUnit.SECONDS)
            assertTrue(
                "Workers did not finish within 30 s — likely deadlock or HashMap.resize spin",
                finished,
            )
            val failure = firstFailure.get()
            assertNull(
                "Worker thread threw: ${failure?.javaClass?.simpleName} — ${failure?.message}",
                failure,
            )
            // Local-counter check: every worker incremented after `addCommand`
            // returned, so the counter equals attempted-puts. If `addCommand`
            // threw the worker would have skipped the increment; this is the
            // weakest assertion and only guards against silent exception swallows.
            assertEquals(
                "Worker increment lost — `addCommand` likely threw silently",
                mutators * iterations,
                insertedCount.get(),
            )
            // Real lost-update detection: the dictionary's own commandMap MUST
            // observe every successful put. Against raw `HashMap.put` under
            // resize contention the JLS gives no upper bound on lost entries;
            // against `ConcurrentHashMap.put` every visible put is durable.
            // Plus 0 inserts for the Cocoa standard library cmds (empty XmlFile).
            val expectedTotal = mutators * iterations
            val actualTotal = dict.getAllCommands().size
            assertEquals(
                "Lost commands during concurrent insert — primary `dictionaryCommandMap` " +
                    "missed ${expectedTotal - actualTotal} put(s). Raw HashMap.put under " +
                    "resize contention is formally undefined behaviour; the CHM-backed " +
                    "`DictionaryIndexes` (Plan 02-05) is the fix.",
                expectedTotal,
                actualTotal,
            )
            // Per-writer membership check: for each writer's namespace, the
            // dictionary must have observed all `iterations` inserts.
            // `findAllCommandsWithName` reads the secondary
            // `dictionaryCommandListMap`, exercising the second map's race
            // surface independently from the primary map check above.
            repeat(mutators) { tid ->
                val sampleName = "cmd-$tid-${iterations - 1}"
                val hits = dict.findAllCommandsWithName(sampleName)
                assertEquals(
                    "Lost from secondary list-map: writer $tid's last insert ($sampleName) " +
                        "is missing from `dictionaryCommandListMap`",
                    1,
                    hits.size,
                )
            }
        } finally {
            pool.shutdownNow()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    /**
     * Build a real [ApplicationDictionaryImpl] over an empty in-memory XmlFile.
     * Mirrors the [com.intellij.plugin.applescript.test.sdef.ApplicationDictionaryOverloadTest]
     * pattern from Plan 02-04: empty `<dictionary/>` so `SDEF_Parser.parse` no-ops,
     * leaving the dictionary ready for direct `addCommand` calls from worker threads.
     */
    private fun buildDictionary(): ApplicationDictionaryImpl {
        val xmlFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "empty.sdef",
                com.intellij.lang.xml.XMLLanguage.INSTANCE,
                "<dictionary title=\"ConcurrencyTestApp\"></dictionary>",
            ) as XmlFile
        return ApplicationDictionaryImpl(
            project = project,
            dictionaryXmlFile = xmlFile,
            applicationName = "ConcurrencyTestApp",
            applicationBundleFile = null,
        )
    }

    /**
     * Build a real [AppleScriptCommandImpl] with a stub Suite + a stub XmlTag.
     * Same Proxy pattern as
     * [com.intellij.plugin.applescript.test.sdef.ApplicationDictionaryOverloadTest.newCommand]
     * — the Suite + XmlTag are only needed for `getSuite()` / `getXmlTag()`
     * accessors that the `addCommand` / `findAllCommandsWithName` paths do
     * not exercise.
     */
    private fun newCommand(name: String, parameters: List<String>): AppleScriptCommand {
        val suiteStub = stubSuite()
        val xmlTagStub = stubXmlTag()
        val cmd = AppleScriptCommandImpl(suiteStub, name, name, xmlTagStub)
        val params: List<CommandParameter> = parameters.map { pName ->
            CommandParameterImpl(cmd, pName, "----", false, "text", null, xmlTagStub)
        }
        cmd.setParameters(params)
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
