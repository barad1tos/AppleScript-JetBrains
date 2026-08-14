package com.intellij.plugin.applescript.test.concurrency

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.plugin.applescript.lang.dictionary.index.SdefIndexService
import com.intellij.plugin.applescript.lang.dictionary.persistence.DictionaryInfo
import com.intellij.plugin.applescript.lang.dictionary.project.AppleScriptProjectDictionaryService
import com.intellij.plugin.applescript.lang.dictionary.readiness.DictionaryReadinessTracker
import com.intellij.plugin.applescript.lang.ide.actions.DictionaryLoadEffects
import com.intellij.plugin.applescript.lang.ide.actions.DictionaryLoadQueue
import com.intellij.plugin.applescript.lang.ide.actions.loadSelectedDictionaries
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assume
import java.lang.reflect.Proxy
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Proves the index lookup guards return immediately with `emptyList()` on the EDT.
 * Without them, a future EDT caller would block the IDE for 2 seconds on the bounded
 * readiness bridge.
 *
 * Heavy-gated because it exercises IntelliJ threading behavior.
 */
class EdtBridgeGuardTest : BasePlatformTestCase() {
    private lateinit var testScope: TestScope

    override fun setUp() {
        Assume.assumeTrue(
            "EdtBridgeGuardTest only runs with -PincludeHeavyTests=true",
            System.getProperty("includeHeavyTests") == "true",
        )
        super.setUp()
        testScope = TestScope()
        val readiness = DictionaryReadinessTracker()
        Disposer.register(testRootDisposable) { testScope.cancel() }
        ApplicationManager.getApplication().replaceService(
            DictionaryReadinessTracker::class.java,
            readiness,
            testRootDisposable,
        )
        ApplicationManager.getApplication().replaceService(
            AppleScriptSystemDictionaryRegistryService::class.java,
            AppleScriptSystemDictionaryRegistryService(
                testScope,
                StandardTestDispatcher(testScope.testScheduler),
                readiness = readiness,
            ),
            testRootDisposable,
        )
    }

    fun testStdLookupReturnsOnEdt() {
        var resultFromEdt: Collection<*>? = null
        val elapsedMillis =
            measureTimeMillis {
                ApplicationManager.getApplication().invokeAndWait {
                    assertTrue(
                        "Pre-check: must be on EDT here",
                        ApplicationManager.getApplication().isDispatchThread,
                    )
                    resultFromEdt = SdefIndexService.getInstance().findStdCommands(project, "anything")
                }
            }
        assertTrue(
            "EDT standard lookup must return before the 2s readiness timeout; elapsed=${elapsedMillis}ms",
            elapsedMillis < MAX_EDT_LOOKUP_MILLIS,
        )
        assertNotNull(resultFromEdt)
        assertTrue(
            "EDT guard must return emptyList() to avoid 2s freeze",
            resultFromEdt!!.isEmpty(),
        )
    }

    fun testAppLookupReturnsOnEdt() {
        var resultFromEdt: List<*>? = null
        val elapsedMillis =
            measureTimeMillis {
                ApplicationManager.getApplication().invokeAndWait {
                    assertTrue(
                        "Pre-check: must be on EDT here",
                        ApplicationManager.getApplication().isDispatchThread,
                    )
                    resultFromEdt = SdefIndexService.getInstance().findApplicationCommands(project, "Music", "play")
                }
            }
        assertTrue(
            "EDT application lookup must return before the 2s readiness timeout; elapsed=${elapsedMillis}ms",
            elapsedMillis < MAX_EDT_LOOKUP_MILLIS,
        )
        assertNotNull(resultFromEdt)
        assertTrue(
            "EDT guard must return emptyList() to avoid 2s freeze",
            resultFromEdt!!.isEmpty(),
        )
    }

    fun testManualLoadThreading() {
        val application = ApplicationManager.getApplication()
        assertTrue("fixture must invoke the action boundary on EDT", application.isDispatchThread)
        val dictionaryFile = FileUtil.createTempFile("manual-load", ".sdef", true)
        dictionaryFile.writeText("<dictionary/>")
        val loadThread = CompletableFuture<Boolean>()
        val loadedName = CompletableFuture<String>()
        val publishThread = CompletableFuture<Boolean>()
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)

        try {
            loadSelectedDictionaries(
                project = project,
                files = listOf(LightVirtualFile("Calendar.sdef", "<dictionary/>")),
                singleApplicationName = "Calendar",
                effects =
                    DictionaryLoadEffects(
                        loadInfo = { applicationName, _ ->
                            loadThread.complete(application.isDispatchThread)
                            loadedName.complete(applicationName)
                            DictionaryInfo(applicationName, dictionaryFile, null).also { it.setInitialized(true) }
                        },
                        afterPublish = {
                            publishThread.complete(application.isDispatchThread)
                        },
                    ),
            )

            waitForCompletion("dictionary load did not finish", loadedName)
            assertEquals("Calendar", loadedName.getNow(null))
            assertFalse("manual dictionary loading must run outside EDT", loadThread.getNow(true))
            waitForCompletion("dictionary publish callback did not finish", publishThread)
            assertTrue("loaded dictionary PSI must be published on EDT", publishThread.getNow(false))
            assertNotNull(
                "manual load must publish into the project cache",
                projectDictionaries.getDictionary("Calendar"),
            )
        } finally {
            projectDictionaries.clearCachedDictionariesForTests()
            dictionaryFile.delete()
        }
    }

    fun testManualLoadCancellation() {
        val finished = CompletableFuture<Boolean>()

        loadSelectedDictionaries(
            project = project,
            files = listOf(LightVirtualFile("Calendar.sdef", "<dictionary/>")),
            singleApplicationName = "Calendar",
            effects =
                DictionaryLoadEffects(
                    loadInfo = { _, _ -> throw CancellationException("test cancellation") },
                    afterPublish = {
                        finished.complete(true)
                    },
                ),
        )

        waitForCompletion("cancelled dictionary load did not finish", finished)
        assertTrue(finished.getNow(false))
    }

    fun testLoadFailureAddsContext() {
        val firstFile = LightVirtualFile("SuccessfulRequest.app", "")
        val failingFile = LightVirtualFile("BrokenRequest.app", "")
        val failure = IllegalArgumentException("broken registry")
        var queuedTask: Task.Backgroundable? = null
        val loadQueue = DictionaryLoadQueue { task -> queuedTask = task }
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)
        val projectProxy =
            Proxy.newProxyInstance(
                Project::class.java.classLoader,
                arrayOf(Project::class.java),
            ) { _, method, arguments ->
                when (method.name) {
                    "isDisposed" -> false
                    "getService" ->
                        when (arguments?.single()) {
                            AppleScriptProjectDictionaryService::class.java -> projectDictionaries
                            DictionaryLoadQueue::class.java -> loadQueue
                            else -> null
                        }

                    else -> error("Unexpected project call: ${method.name}")
                }
            } as Project

        loadSelectedDictionaries(
            project = projectProxy,
            files = listOf(firstFile, failingFile),
            singleApplicationName = null,
            effects =
                DictionaryLoadEffects(
                    loadInfo = { applicationName, _ ->
                        if (applicationName == failingFile.nameWithoutExtension) throw failure
                        null
                    },
                ),
        )

        val thrown =
            runCatching {
                requireNotNull(queuedTask).run(EmptyProgressIndicator())
            }.exceptionOrNull()
        if (thrown !is IllegalStateException) {
            fail("Expected IllegalStateException, got ${thrown?.javaClass?.name}")
            return
        }
        assertEquals(
            "Failed to load dictionary 'BrokenRequest' from ${failingFile.path}",
            thrown.message,
        )
        assertSame(failure, thrown.cause)
    }

    fun testPartialLoadPublishes() {
        val dictionaryFile = FileUtil.createTempFile("partial-manual-load", ".sdef", true)
        dictionaryFile.writeText("<dictionary/>")
        val loadedApplication = "SyntheticPartialSuccess_${System.nanoTime()}"
        val failedApplication = "SyntheticPartialFailure_${System.nanoTime()}"
        val attemptedApplications = CopyOnWriteArrayList<String>()
        val finished = CompletableFuture<Boolean>()
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)

        try {
            loadSelectedDictionaries(
                project = project,
                files =
                    listOf(
                        LightVirtualFile("$loadedApplication.app", ""),
                        LightVirtualFile("$failedApplication.app", ""),
                    ),
                singleApplicationName = null,
                effects =
                    DictionaryLoadEffects(
                        loadInfo = { applicationName, _ ->
                            attemptedApplications.add(applicationName)
                            if (applicationName == loadedApplication) {
                                DictionaryInfo(applicationName, dictionaryFile, null).also { it.setInitialized(true) }
                            } else {
                                null
                            }
                        },
                        afterPublish = { finished.complete(true) },
                    ),
            )

            waitForCompletion("partial dictionary load did not finish", finished)
            assertEquals(listOf(loadedApplication, failedApplication), attemptedApplications)
            assertNotNull(
                "a successful dictionary must remain available when another selected file fails",
                projectDictionaries.getDictionary(loadedApplication),
            )
            assertNull(projectDictionaries.getDictionary(failedApplication))
        } finally {
            projectDictionaries.clearCachedDictionariesForTests()
            dictionaryFile.delete()
        }
    }

    fun testDisposedLoadSkipsPublish() {
        val dictionaryFile = FileUtil.createTempFile("disposed-manual-load", ".sdef", true)
        dictionaryFile.writeText("<dictionary/>")
        val loadStarted = CountDownLatch(1)
        val allowLoadToFinish = CountDownLatch(1)
        val publishFinished = CompletableFuture<Boolean>()
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)
        var daemonRestarts = 0
        var isDisposed = false
        var queuedTask: Task.Backgroundable? = null
        val loadQueue = DictionaryLoadQueue { task -> queuedTask = task }
        val projectProxy =
            Proxy.newProxyInstance(
                Project::class.java.classLoader,
                arrayOf(Project::class.java),
            ) { _, method, arguments ->
                when (method.name) {
                    "isDisposed" -> isDisposed
                    "getService" ->
                        when (arguments?.single()) {
                            AppleScriptProjectDictionaryService::class.java -> projectDictionaries
                            DictionaryLoadQueue::class.java -> loadQueue
                            else -> null
                        }

                    else -> error("Unexpected project call: ${method.name}")
                }
            } as Project

        try {
            loadSelectedDictionaries(
                project = projectProxy,
                files = listOf(LightVirtualFile("Calendar.app", "")),
                singleApplicationName = null,
                effects =
                    DictionaryLoadEffects(
                        loadInfo = { applicationName, _ ->
                            loadStarted.countDown()
                            assertTrue(allowLoadToFinish.await(5, TimeUnit.SECONDS))
                            DictionaryInfo(applicationName, dictionaryFile, null).also { it.setInitialized(true) }
                        },
                        restartDaemon = { daemonRestarts++ },
                        afterPublish = { publishFinished.complete(true) },
                    ),
            )

            val task = requireNotNull(queuedTask)
            val worker =
                ApplicationManager.getApplication().executeOnPooledThread {
                    task.run(EmptyProgressIndicator())
                }
            assertTrue("dictionary load did not start", loadStarted.await(5, TimeUnit.SECONDS))
            isDisposed = true
            allowLoadToFinish.countDown()
            worker.get(5, TimeUnit.SECONDS)
            task.onFinished()

            assertTrue(publishFinished.getNow(false))
            assertEquals("disposed loads must not restart highlighting", 0, daemonRestarts)
            assertNull(
                "a dictionary loaded after project disposal must not enter the project cache",
                projectDictionaries.getDictionary("Calendar"),
            )
        } finally {
            allowLoadToFinish.countDown()
            projectDictionaries.clearCachedDictionariesForTests()
            dictionaryFile.delete()
        }
    }

    fun testManualRequestOrder() {
        val firstDictionaryFile = FileUtil.createTempFile("manual-load-first", ".sdef", true)
        val secondDictionaryFile = FileUtil.createTempFile("manual-load-second", ".sdef", true)
        firstDictionaryFile.writeText("<dictionary/>")
        secondDictionaryFile.writeText("<dictionary/>")
        val secondLoadStarted = CountDownLatch(1)
        val allowFirstReturn = CountDownLatch(1)
        val publishedLoads = CopyOnWriteArrayList<String>()
        val completedLoads = CompletableFuture<Boolean>()
        val projectDictionaries = project.getService(AppleScriptProjectDictionaryService::class.java)

        try {
            loadSelectedDictionaries(
                project = project,
                files = listOf(LightVirtualFile("Calendar.sdef", "<dictionary/>")),
                singleApplicationName = "Calendar",
                effects =
                    DictionaryLoadEffects(
                        loadInfo = { applicationName, _ ->
                            if (secondLoadStarted.await(250, TimeUnit.MILLISECONDS)) {
                                assertTrue(allowFirstReturn.await(5, TimeUnit.SECONDS))
                            }
                            DictionaryInfo(applicationName, firstDictionaryFile, null).also { it.setInitialized(true) }
                        },
                        afterPublish = {
                            publishedLoads.add("first")
                            if (publishedLoads.size == 2) completedLoads.complete(true)
                        },
                    ),
            )
            loadSelectedDictionaries(
                project = project,
                files = listOf(LightVirtualFile("Calendar.sdef", "<dictionary/>")),
                singleApplicationName = "Calendar",
                effects =
                    DictionaryLoadEffects(
                        loadInfo = { applicationName, _ ->
                            secondLoadStarted.countDown()
                            DictionaryInfo(applicationName, secondDictionaryFile, null).also { it.setInitialized(true) }
                        },
                        afterPublish = {
                            publishedLoads.add("second")
                            allowFirstReturn.countDown()
                            if (publishedLoads.size == 2) completedLoads.complete(true)
                        },
                    ),
            )

            waitForCompletion("manual dictionary loads did not finish", completedLoads)
            assertEquals(listOf("first", "second"), publishedLoads)
        } finally {
            allowFirstReturn.countDown()
            projectDictionaries.clearCachedDictionariesForTests()
            firstDictionaryFile.delete()
            secondDictionaryFile.delete()
        }
    }

    fun testManualLoadQueueOrder() {
        val startedTasks = mutableListOf<String>()
        val taskCompletions = mutableMapOf<String, () -> Unit>()
        val loadQueue = DictionaryLoadQueue { task -> startedTasks.add(task.title) }

        fun submitTask(title: String) {
            loadQueue.submit { taskFinished ->
                taskCompletions[title] = taskFinished
                object : Task.Backgroundable(project, title, true) {
                    override fun run(indicator: ProgressIndicator) = Unit
                }
            }
        }

        submitTask("first")
        submitTask("second")
        assertEquals(listOf("first"), startedTasks)

        taskCompletions.getValue("first").invoke()
        assertEquals(listOf("first", "second"), startedTasks)
        taskCompletions.getValue("second").invoke()
    }

    fun testQueueDropsDisposedLoads() {
        var isDisposed = false
        val disposedProject =
            Proxy.newProxyInstance(
                Project::class.java.classLoader,
                arrayOf(Project::class.java),
            ) { _, method, _ ->
                check(method.name == "isDisposed") { "Unexpected project call: ${method.name}" }
                isDisposed
            } as Project
        val startedTasks = mutableListOf<String>()
        val taskCompletions = mutableMapOf<String, () -> Unit>()
        val loadQueue = DictionaryLoadQueue { task -> startedTasks.add(task.title) }

        fun submitTask(title: String) {
            loadQueue.submit { taskFinished ->
                taskCompletions[title] = taskFinished
                object : Task.Backgroundable(disposedProject, title, true) {
                    override fun run(indicator: ProgressIndicator) = Unit
                }
            }
        }

        submitTask("first")
        submitTask("second")
        isDisposed = true
        taskCompletions.getValue("first").invoke()

        assertEquals(listOf("first"), startedTasks)
    }

    private fun waitForCompletion(
        message: String,
        completion: CompletableFuture<*>,
    ) {
        PlatformTestUtil.waitWithEventsDispatching(message, completion::isDone, 5)
    }

    companion object {
        private const val MAX_EDT_LOOKUP_MILLIS = 1_000L
    }
}
