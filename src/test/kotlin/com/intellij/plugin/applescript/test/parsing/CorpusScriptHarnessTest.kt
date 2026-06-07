package com.intellij.plugin.applescript.test.parsing

import junit.framework.TestCase
import java.nio.file.Files
import java.nio.file.Path

class CorpusScriptHarnessTest : TestCase() {
    private val root: Path = Path.of(System.getProperty("user.dir")).toAbsolutePath()

    fun testClassifyRejectsRepositoryOutputBeforeDeleting() {
        val unsafeOutput = Files.createTempDirectory(root.resolve("build"), "unsafe-corpus-output")
        val marker = unsafeOutput.resolve("marker.txt")
        Files.writeString(marker, "keep")
        val input = Files.createTempDirectory("applescript-corpus-input")

        val result =
            runScript(
                script = "scripts/corpus/classify.sh",
                args = listOf(input),
                env = mapOf("CORPUS_OUT" to unsafeOutput.toString()),
            )

        assertEquals(result.output, 4, result.exitCode)
        assertTrue("unsafe output directory must not be deleted", Files.exists(marker))
    }

    fun testClassifyCopiesOnlyCompilerValidSourcesToValidDirectory() {
        val workspace = Files.createTempDirectory("applescript-corpus-script-test")
        val input = Files.createDirectory(workspace.resolve("input"))
        val output = workspace.resolve("out")
        val fakeBin = Files.createDirectory(workspace.resolve("bin"))
        val fakeCompiler = fakeBin.resolve("osacompile")
        Files.writeString(fakeCompiler, "#!/bin/sh\nexit 0\n")
        assertTrue(fakeCompiler.toFile().setExecutable(true))
        Files.writeString(input.resolve("valid.applescript"), "display dialog \"ok\"\n")

        val result =
            runScript(
                script = "scripts/corpus/classify.sh",
                args = listOf(input),
                env =
                    mapOf(
                        "CORPUS_OUT" to output.toString(),
                        "PATH" to "$fakeBin:${System.getenv("PATH")}",
                    ),
            )

        assertEquals(result.output, 0, result.exitCode)
        assertTrue(
            "raw decompiled source should be retained for diagnostics",
            hasAnyFile(output.resolve("src")),
        )
        assertTrue(
            "compiler-valid sources should be copied into the parser input subset",
            hasAnyFile(output.resolve("valid")),
        )

        val manifest = Files.readString(output.resolve("manifest.tsv"))
        assertTrue(manifest, manifest.contains("VALID_HERE\t${output.resolve("valid")}"))
    }

    fun testScanUsesValidSubsetAndFailsWhenReportIsMissing() {
        val scanScript = Files.readString(root.resolve("scripts/corpus/scan.sh"))

        assertTrue(scanScript.contains("APPLESCRIPT_CORPUS_DIR=\"\$VALID_DIR\""))
        assertTrue(scanScript.contains("parser scan failed; see"))
        assertTrue(scanScript.contains("parser scan completed without differential report"))
    }

    private fun runScript(
        script: String,
        args: List<Path>,
        env: Map<String, String>,
    ): CommandResult {
        val command = mutableListOf("bash", root.resolve(script).toString())
        command += args.map(Path::toString)
        val processBuilder =
            ProcessBuilder(command)
                .directory(root.toFile())
                .redirectErrorStream(true)
        processBuilder.environment().putAll(env)
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        return CommandResult(process.waitFor(), output)
    }

    private fun hasAnyFile(directory: Path): Boolean =
        Files.list(directory).use { stream -> stream.findAny().isPresent }

    private data class CommandResult(
        val exitCode: Int,
        val output: String,
    )
}
