package com.intellij.plugin.applescript.test.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.plugin.applescript.lang.dictionary.discovery.DeveloperToolsNotInstalledException
import com.intellij.plugin.applescript.lang.dictionary.discovery.DictionaryLoadResult
import com.intellij.plugin.applescript.lang.dictionary.discovery.NotScriptableApplicationException
import com.intellij.plugin.applescript.lang.dictionary.files.SdefDictionaryFileGenerator
import com.intellij.plugin.applescript.lang.dictionary.files.SdefFileProvider
import com.intellij.plugin.applescript.lang.dictionary.files.serializeDictionaryPathForApplication
import com.intellij.plugin.applescript.lang.dictionary.persistence.SdefPersistenceService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class DictionaryLoadPolicyTest : BasePlatformTestCase() {
    fun testMalformedSdefReturnsNull() {
        val applicationName = "SyntheticMalformed_${System.nanoTime()}"
        val dictionaryFile = Files.createTempFile("provider-malformed-", ".sdef").toFile()
        val generatedFile = File(serializeDictionaryPathForApplication(applicationName))
        dictionaryFile.writeText("<dictionary><suite>")

        try {
            assertNull(
                "Legacy file interface must collapse initialization failure to null",
                SdefFileProvider.getInstance().createAndInitializeInfo(dictionaryFile, applicationName),
            )
        } finally {
            cleanApplication(applicationName)
            dictionaryFile.delete()
            generatedFile.delete()
        }
    }

    fun testLoadReportsIoFailure() {
        val applicationName = "SyntheticIoFailure_${System.nanoTime()}"
        val applicationFile = tempApplication(applicationName)
        val failure = IOException("Synthetic I/O failure")

        try {
            val result =
                SdefFileProvider.getInstance().loadDictionary(
                    applicationFile,
                    applicationName,
                    generateDictionaryFile = { _, _ -> throw failure },
                )

            assertEquals(
                DictionaryLoadResult.Failed(
                    applicationName,
                    "Dictionary file I/O error: ${failure.message}",
                    failure,
                ),
                result,
            )
        } finally {
            applicationFile.parentFile.deleteRecursively()
        }
    }

    fun testLoadPreservesInterrupt() {
        if (!SystemInfo.isMac) return

        val applicationName = "SyntheticInterruptedLoad_${System.nanoTime()}"
        val dictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "provider-interrupt",
                SyntheticSuiteFixtures.emptySuiteXml(),
            )
        val generatedFile = File(serializeDictionaryPathForApplication(applicationName))
        val future =
            ApplicationManager.getApplication().executeOnPooledThread {
                Thread.currentThread().interrupt()
                try {
                    val result = SdefFileProvider.getInstance().loadDictionary(dictionaryFile, applicationName)

                    assertTrue(
                        "Interrupted generation must return a typed failure; got $result",
                        result is DictionaryLoadResult.Failed,
                    )
                    assertTrue(
                        "Interrupted generation must preserve its cause",
                        (result as DictionaryLoadResult.Failed).cause is InterruptedException,
                    )
                    assertTrue(
                        "Interrupted generation must restore the worker flag",
                        Thread.currentThread().isInterrupted,
                    )
                } finally {
                    Thread.interrupted()
                }
            }

        try {
            future.get(5, TimeUnit.SECONDS)
            assertFalse("Interrupted generation must remove its partial cache file", generatedFile.exists())
        } finally {
            cleanApplication(applicationName)
            dictionaryFile.delete()
            generatedFile.delete()
        }
    }

    fun testLoadRecoversBundledFile() {
        val applicationName = "SyntheticBundledRecovery_${System.nanoTime()}"
        val applicationFile = tempApplication(applicationName)
        val dictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "provider-recovery",
                SyntheticSuiteFixtures.musicAppPlayCommandXml(),
            )
        val failure = DeveloperToolsNotInstalledException()

        try {
            val result =
                SdefFileProvider.getInstance().loadDictionary(
                    applicationFile,
                    applicationName,
                    generateDictionaryFile = { _, _ -> throw failure },
                    recoverDictionaryFile = { _, _ -> dictionaryFile },
                )

            assertTrue(
                "Bundled recovery must produce a loaded dictionary; got $result",
                result is DictionaryLoadResult.Loaded,
            )
            val info = (result as DictionaryLoadResult.Loaded).info
            assertEquals(applicationName, info.getApplicationName())
            assertEquals(dictionaryFile.path, info.getDictionaryFile().path)
            assertTrue("Recovered dictionary must be initialized", info.initialized)
        } finally {
            cleanApplication(applicationName)
            applicationFile.parentFile.deleteRecursively()
            dictionaryFile.delete()
        }
    }

    fun testMissingRecoveryFailsLoad() {
        val applicationName = "SyntheticMissingRecovery_${System.nanoTime()}"
        val applicationFile = tempApplication(applicationName)
        val failure = DeveloperToolsNotInstalledException()

        try {
            val result =
                SdefFileProvider.getInstance().loadDictionary(
                    applicationFile,
                    applicationName,
                    generateDictionaryFile = { _, _ -> throw failure },
                    recoverDictionaryFile = { _, _ -> null },
                )

            assertEquals(
                DictionaryLoadResult.Failed(
                    applicationName,
                    "Developer Tools not installed (sdef CLI unavailable)",
                    failure,
                ),
                result,
            )
            assertTrue(
                "Unrecoverable application must update persisted discovery state",
                SdefPersistenceService.getInstance().isNotScriptable(applicationName),
            )
        } finally {
            cleanApplication(applicationName)
            applicationFile.parentFile.deleteRecursively()
        }
    }

    fun testRecoveryCopyFailure() {
        val applicationName = "SyntheticRecoveryCopyFailure_${System.nanoTime()}"
        val applicationFile = tempApplication(applicationName)
        val resources = File(applicationFile, "Contents/Resources")
        val bundledDictionary = File(resources, "Synthetic.sdef")
        assertTrue("Synthetic resources directory must be created", resources.mkdirs())
        bundledDictionary.writeText(SyntheticSuiteFixtures.emptySuiteXml())
        val generationFailure = DeveloperToolsNotInstalledException()

        try {
            val result =
                SdefFileProvider.getInstance().loadDictionary(
                    applicationFile,
                    applicationName,
                    generateDictionaryFile = { _, _ -> throw generationFailure },
                    recoverDictionaryFile = { name, file ->
                        SdefDictionaryFileGenerator.recoverDictionaryFile(
                            name,
                            file,
                            copyDictionaryFile = { _, _, _, _ -> false },
                        )
                    },
                )

            assertTrue(
                "Cache-copy failure must return a typed failure; got $result",
                result is DictionaryLoadResult.Failed,
            )
            assertTrue(
                "Cache-copy failure must preserve its I/O category",
                (result as DictionaryLoadResult.Failed).cause is IOException,
            )
            assertEquals(
                "Dictionary file I/O error: Failed to cache bundled dictionary for $applicationName",
                result.reason,
            )
            assertFalse(
                "Transient cache-copy failure must not blacklist the application",
                SdefPersistenceService.getInstance().isNotScriptable(applicationName),
            )
        } finally {
            cleanApplication(applicationName)
            applicationFile.parentFile.deleteRecursively()
        }
    }

    fun testCancellationEscapesLoad() {
        val applicationName = "SyntheticCancelledLoad_${System.nanoTime()}"
        val applicationFile = tempApplication(applicationName)
        val cancellation = CancellationException("Synthetic cancellation")

        try {
            val thrown =
                runCatching {
                    SdefFileProvider.getInstance().loadDictionary(
                        applicationFile,
                        applicationName,
                        generateDictionaryFile = { _, _ -> throw cancellation },
                    )
                }.exceptionOrNull()

            assertSame("Cancellation must escape the loading module", cancellation, thrown)
        } finally {
            applicationFile.parentFile.deleteRecursively()
        }
    }

    fun testIllegalStateEscapes() {
        val applicationName = "SyntheticIllegalState_${System.nanoTime()}"
        val applicationFile = tempApplication(applicationName)
        val failure = IllegalStateException("Synthetic programming error")

        try {
            val thrown =
                runCatching {
                    SdefFileProvider.getInstance().loadDictionary(
                        applicationFile,
                        applicationName,
                        generateDictionaryFile = { _, _ -> throw failure },
                    )
                }.exceptionOrNull()

            assertSame("Programming errors must escape the shared load path", failure, thrown)
        } finally {
            applicationFile.parentFile.deleteRecursively()
        }
    }

    fun testGeneratorThrowsFailure() {
        val applicationName = "SyntheticGeneratorFailure_${System.nanoTime()}"
        val dictionaryFile =
            SyntheticSuiteFixtures.writeToTempFile(
                "provider-generator-failure",
                SyntheticSuiteFixtures.emptySuiteXml(),
            )
        val failure = NotScriptableApplicationException(applicationName, "Synthetic failure")

        try {
            val thrown =
                runCatching {
                    SdefDictionaryFileGenerator.generateDictionaryFile(applicationName, dictionaryFile) {
                        throw failure
                    }
                }.exceptionOrNull()

            assertSame("Generator must preserve the original failure", failure, thrown)
            assertFalse(
                "Generator must not own persistence policy",
                SdefPersistenceService.getInstance().isNotScriptable(applicationName),
            )
        } finally {
            cleanApplication(applicationName)
            dictionaryFile.delete()
        }
    }

    fun testNotScriptableLoadFails() {
        val applicationName = "SyntheticNotScriptable_${System.nanoTime()}"
        val applicationFile = tempApplication(applicationName)
        val failure = NotScriptableApplicationException(applicationName, "Synthetic failure")

        try {
            val result =
                SdefFileProvider.getInstance().loadDictionary(
                    applicationFile,
                    applicationName,
                    generateDictionaryFile = { _, _ -> throw failure },
                )

            assertEquals(
                DictionaryLoadResult.Failed(applicationName, "Application is not scriptable", failure),
                result,
            )
            assertTrue(
                "Not-scriptable failure must update persisted discovery state",
                SdefPersistenceService.getInstance().isNotScriptable(applicationName),
            )
        } finally {
            cleanApplication(applicationName)
            applicationFile.parentFile.deleteRecursively()
        }
    }

    private fun cleanApplication(applicationName: String) {
        val persistence = SdefPersistenceService.getInstance()
        persistence.removeDictionaryInfoByNameForTests(applicationName)
        persistence.removeNotScriptable(applicationName)
    }

    private fun tempApplication(applicationName: String): File {
        val parent = Files.createTempDirectory("sdef-provider-app-").toFile()
        return File(parent, "$applicationName.app").also { applicationFile ->
            assertTrue("Synthetic application directory must be created", applicationFile.mkdir())
        }
    }
}
