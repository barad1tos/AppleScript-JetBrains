package com.intellij.plugin.applescript.test.parsing

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.testFramework.ParsingTestCase
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Abstract base for the parsing-fixture subclasses (handlers, tell, dictionary_constant,
 * standard_additions, live_samples, control statements). Boots a [BasePlatformTestCase]
 * fixture, scans the per-subclass target directory for `*.scpt` files in [setUp], and
 * exposes [doParseAllInPackageTest] / [doParseScriptInPackageTest] which delegate to
 * [ParsingTestCase.doCheckResult] against the on-disk golden `.txt` fixtures.
 *
 * Heavy-suite only (`-PincludeHeavyTests=true`): each subclass boots a full fixture and
 * the dictionary registry scans installed applications.
 */
abstract class AbstractParsingFixtureTestCase : BasePlatformTestCase() {
    // The Java original initialised this field eagerly:
    //   String myTargetTestDataDir = getMyTestDataDir() + "/" + getMyTargetDirectoryPath();
    // That calls the overridable getMyTargetDirectoryPath() during construction. A naive Kotlin
    // property initializer at the same spot runs before the subclass override is live; `by lazy`
    // defers resolution to first access (inside setUp), after the override is installed.
    private val myTargetTestDataDir: String by lazy { "${getMyTestDataDir()}/${getMyTargetDirectoryPath()}" }
    private val myPsiFiles = mutableListOf<PsiFile>()

    protected abstract fun getMyTargetDirectoryPath(): String

    protected fun getMyTestDataDir(): String = TEST_DATA_DIR

    override fun getTestDataPath(): String = File(getMyTestDataDir()).absolutePath

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val targetDir = File(myTargetTestDataDir)
        check(targetDir.isDirectory) { "$targetDir is not a directory" }

        targetDir
            .listFiles { pathname -> pathname.name.endsWith("scpt") }
            ?.forEach { file -> myPsiFiles.add(myFixture.configureByFile(file.canonicalPath)) }
    }

    protected fun doParseAllInPackageTest() {
        LOG.info("Parsing files in the package: " + getMyTargetDirectoryPath())
        for (psiFile in myPsiFiles) {
            LOG.info("File: " + psiFile.name)
            ParsingTestCase.doCheckResult(
                myTargetTestDataDir,
                psiFile,
                checkAllPsiRoots(),
                psiFile.virtualFile.nameWithoutExtension,
                SKIP_SPACES,
                PRINT_RANGES,
            )
        }
    }

    protected fun doParseScriptInPackageTest(fileNameWithoutExtension: String) {
        for (psiFile in myPsiFiles) {
            val dataName = psiFile.virtualFile.nameWithoutExtension
            if (dataName == fileNameWithoutExtension) {
                ParsingTestCase.doCheckResult(
                    myTargetTestDataDir,
                    psiFile,
                    checkAllPsiRoots(),
                    dataName,
                    SKIP_SPACES,
                    PRINT_RANGES,
                )
            }
        }
    }

    protected open fun checkAllPsiRoots(): Boolean = true

    companion object {
        private const val TEST_DATA_DIR = "src/test/resources/testData/parse"
        private const val SKIP_SPACES = false
        private const val PRINT_RANGES = true
        private val LOG = Logger.getInstance("#" + AbstractParsingFixtureTestCase::class.java.name)
    }
}
