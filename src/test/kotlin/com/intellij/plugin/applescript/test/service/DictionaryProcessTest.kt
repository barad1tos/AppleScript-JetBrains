package com.intellij.plugin.applescript.test.service

import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.files.waitForProcess
import junit.framework.TestCase
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class DictionaryProcessTest : TestCase() {
    fun testCompletionSucceeds() {
        if (!SystemInfo.isUnix) return

        val process = ProcessBuilder("/bin/bash", "-c", "exit 0").start()
        var killCalled = false
        try {
            assertTrue(
                "Completed dictionary command must report successful execution",
                waitForProcess(process, 5, TimeUnit.SECONDS) {
                    killCalled = true
                    false
                },
            )
            assertFalse("Completed dictionary command must not be terminated", killCalled)
        } finally {
            process.destroyForcibly()
        }
    }

    fun testTimeoutKillsTree() {
        if (!SystemInfo.isUnix) return

        val processTree = startProcessTree()
        try {
            val startedAt = System.nanoTime()
            assertFalse(
                "Timed-out dictionary command must report incomplete execution",
                waitForProcess(processTree.parent, 100, TimeUnit.MILLISECONDS),
            )
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            assertTrue("Dictionary command timeout took ${elapsedMillis}ms", elapsedMillis < 2_000)
            assertTreeStopped(processTree)
        } finally {
            processTree.stop()
        }
    }

    fun testInterruptKillsTree() {
        if (!SystemInfo.isUnix) return

        val processTree = startProcessTree()
        val workerStarted = CountDownLatch(1)
        val thrown = AtomicReference<Throwable?>()
        val cleanupFailure = IllegalStateException("Synthetic interrupt cleanup failure")
        val worker =
            Thread {
                workerStarted.countDown()
                thrown.set(
                    runCatching {
                        waitForProcess(processTree.parent, 30, TimeUnit.SECONDS) { throw cleanupFailure }
                    }.exceptionOrNull(),
                )
            }
        try {
            worker.start()
            assertTrue("Dictionary command worker must start", workerStarted.await(5, TimeUnit.SECONDS))
            awaitBlocked(worker)
            worker.interrupt()
            worker.join(TimeUnit.SECONDS.toMillis(5))

            assertFalse("Interrupted dictionary command worker must stop", worker.isAlive)
            val interruption = thrown.get()
            assertTrue(
                "Interrupted dictionary command must propagate interruption",
                interruption is InterruptedException,
            )
            assertSame("Cleanup failure must be suppressed", cleanupFailure, interruption?.suppressed?.single())
            assertTreeStopped(processTree)
        } finally {
            worker.interrupt()
            worker.join(TimeUnit.SECONDS.toMillis(5))
            processTree.stop()
        }
    }

    fun testKillFallback() {
        if (!SystemInfo.isUnix) return

        assertKillFallback { false }
        assertKillFallback { throw IllegalStateException("Synthetic platform kill failure") }
    }

    fun testReparentedChildKilled() {
        if (!SystemInfo.isUnix) return

        val processTree = startProcessTree()
        try {
            assertFalse(
                "Fallback must preserve the timed-out result",
                waitForProcess(processTree.parent, 100, TimeUnit.MILLISECONDS) { process ->
                    process.destroyForcibly()
                    assertTrue("Platform kill fixture must stop the parent", process.waitFor(5, TimeUnit.SECONDS))
                    false
                },
            )
            assertTreeStopped(processTree)
        } finally {
            processTree.stop()
        }
    }

    private fun assertKillFallback(killTree: (Process) -> Boolean) {
        val processTree = startProcessTree()
        try {
            assertFalse(
                "Fallback must preserve the timed-out result",
                waitForProcess(processTree.parent, 100, TimeUnit.MILLISECONDS, killTree),
            )
            assertTreeStopped(processTree)
        } finally {
            processTree.stop()
        }
    }

    private fun startProcessTree(): TestProcessTree {
        val childPidFile = Files.createTempFile("dictionary-child-", ".pid").toFile()
        val childScript =
            $$"""sleep 30 & grandchild=$!; printf "%s %s" "$BASHPID" "$grandchild" > "$1"; wait"""
        val parentScript = """bash -c '$childScript' dictionary-child "$1" & wait"""
        val parent =
            ProcessBuilder(
                "/bin/bash",
                "-c",
                parentScript,
                "dictionary-process-test",
                childPidFile.path,
            ).start()

        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (childPidFile.length() == 0L && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }
            assertTrue("Child process PID must be published", childPidFile.length() > 0L)

            val descendants =
                childPidFile
                    .readText()
                    .trim()
                    .split(' ')
                    .map { ProcessHandle.of(it.toLong()).orElseThrow() }
            return TestProcessTree(parent, descendants, childPidFile)
        } catch (failure: Throwable) {
            val cleanupFailure =
                runCatching {
                    parent.descendants().use { descendants ->
                        descendants.forEach(ProcessHandle::destroyForcibly)
                    }
                    parent.destroyForcibly()
                }.exceptionOrNull()
            if (cleanupFailure != null) failure.addSuppressed(cleanupFailure)
            childPidFile.delete()
            throw failure
        }
    }

    private fun assertTreeStopped(processTree: TestProcessTree) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (
            (processTree.parent.isAlive || processTree.descendants.any(ProcessHandle::isAlive)) &&
            System.nanoTime() < deadline
        ) {
            Thread.sleep(10)
        }

        assertFalse("Dictionary command parent must stop", processTree.parent.isAlive)
        assertTrue(
            "Dictionary command descendants must stop",
            processTree.descendants.none(ProcessHandle::isAlive),
        )
    }

    private fun awaitBlocked(worker: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (worker.isAlive && !isWaiting(worker) && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }

        assertTrue("Dictionary command worker must block while waiting", worker.isAlive)
        assertTrue(
            "Dictionary command worker must enter a waiting state; got ${worker.state}",
            isWaiting(worker),
        )
    }

    private fun isWaiting(worker: Thread): Boolean =
        worker.state == Thread.State.WAITING || worker.state == Thread.State.TIMED_WAITING

    private data class TestProcessTree(
        val parent: Process,
        val descendants: List<ProcessHandle>,
        val childPidFile: File,
    ) {
        fun stop() {
            descendants.asReversed().forEach(ProcessHandle::destroyForcibly)
            parent.destroyForcibly()
            childPidFile.delete()
        }
    }
}
