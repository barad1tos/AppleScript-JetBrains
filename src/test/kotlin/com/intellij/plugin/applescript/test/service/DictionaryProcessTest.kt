package com.intellij.plugin.applescript.test.service

import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.files.waitForProcess
import junit.framework.TestCase
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class DictionaryProcessTest : TestCase() {
    fun testTimeoutKillsTree() {
        if (!SystemInfo.isUnix) return

        val processTree = startProcessTree()
        try {
            assertFalse(
                "Timed-out dictionary command must report incomplete execution",
                waitForProcess(processTree.parent, 100, TimeUnit.MILLISECONDS),
            )
            assertTreeStopped(processTree)
        } finally {
            processTree.stop()
        }
    }

    fun testInterruptKillsTree() {
        if (!SystemInfo.isUnix) return

        val processTree = startProcessTree()
        try {
            Thread.currentThread().interrupt()
            val thrown =
                runCatching {
                    waitForProcess(processTree.parent, 30, TimeUnit.SECONDS)
                }.exceptionOrNull()

            assertTrue("Interrupted dictionary command must propagate interruption", thrown is InterruptedException)
            assertTreeStopped(processTree)
        } finally {
            Thread.interrupted()
            processTree.stop()
        }
    }

    fun testFallbackKillsTree() {
        if (!SystemInfo.isUnix) return

        val processTree = startProcessTree()
        try {
            assertFalse(
                "Fallback must preserve the timed-out result",
                waitForProcess(processTree.parent, 100, TimeUnit.MILLISECONDS) { false },
            )
            assertTreeStopped(processTree)
        } finally {
            processTree.stop()
        }
    }

    private fun startProcessTree(): TestProcessTree {
        val childPidFile = Files.createTempFile("dictionary-child-", ".pid").toFile()
        val parent =
            ProcessBuilder(
                "/bin/bash",
                "-c",
                $$"""sleep 30 & child=$!; printf '%s' "$child" > "$1"; wait""",
                "dictionary-process-test",
                childPidFile.path,
            ).start()

        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (childPidFile.length() == 0L && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }
            assertTrue("Child process PID must be published", childPidFile.length() > 0L)

            val childPid = childPidFile.readText().trim().toLong()
            val child = ProcessHandle.of(childPid).orElseThrow()
            return TestProcessTree(parent, child, childPidFile)
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
        while ((processTree.parent.isAlive || processTree.child.isAlive) && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }

        assertFalse("Dictionary command parent must stop", processTree.parent.isAlive)
        assertFalse("Dictionary command child must stop", processTree.child.isAlive)
    }

    private data class TestProcessTree(
        val parent: Process,
        val child: ProcessHandle,
        val childPidFile: File,
    ) {
        fun stop() {
            child.destroyForcibly()
            parent.destroyForcibly()
            childPidFile.delete()
        }
    }
}
