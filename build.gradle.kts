import java.time.Duration
import org.gradle.api.file.ConfigurableFileCollection
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
// Phase 4 SERVICE-10: imports for the bundled grammarkit task types from IPGP 2.16.0.
// The bundled variant lives under org.jetbrains.intellij.platform.gradle.tasks,
// NOT under org.jetbrains.grammarkit.tasks (that's the standalone plugin).
import org.jetbrains.intellij.platform.gradle.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.tasks.GenerateParserTask

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
    // Phase 4 SERVICE-10: bundled grammarkit plugin from IntelliJ Platform Gradle Plugin 2.16.0.
    // Provides `generateLexer` / `generateParser` tasks that the `verifyGeneratedSourcesMatch`
    // task below configures to write into a tmp dir for drift diffing against committed
    // src/main/gen. The id requires its own version even though the plugin is bundled with
    // IPGP — the version string must match the IPGP version (verified against intellij-sdk-docs).
    id("org.jetbrains.intellij.platform.grammarkit") version "2.16.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn",
        )
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

sourceSets {
    named("main") {
        java.srcDirs("src/main/java", "src/main/gen")
        kotlin.srcDirs("src/main/kotlin")
        resources {
            srcDirs("src/main/resources")
            exclude("**.bnf", "**.flex")
        }
    }
    named("test") {
        kotlin.srcDirs("src/test/kotlin")
    }
}

dependencies {
    // v1.2 (Phase 3): kotlinx-coroutines-core is compileOnly-only.
    // IntelliJ Platform 2024.3+ ships a patched fork (1.8.0-intellij-NN /
    // 1.10.1-intellij-NN) merged into lib/app.jar. Shading our own copy
    // triggers NoSuchMethodError: CancellableContinuation.tryResume — see
    // Phase 7 incident. verifyNoBundledCoroutines enforces this at build time.
    compileOnly(libs.kotlinx.coroutines.core)

    implementation(libs.commons.imaging)
    implementation(libs.proxy.vole)

    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion"),
        )
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.JUnit5)
    }

    testImplementation(libs.junit4)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    // D-05 — TestDispatcher only; core stripped by configurations block below
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Plan 03-10 (Option A — version skew, not shadow problem): supply Kotlin 2.x stdlib at
    // test runtime. Compiler is 2.3.21, emits bytecode calling kotlin/coroutines/jvm/internal/
    // SpillingKt (introduced in stdlib 2.0+). kotlinx-coroutines-test:1.8.0 transitively pulls
    // kotlin-stdlib:1.9.21 which predates the helper -> background serviceScope.launch in the
    // test environment crashes with NoClassDefFoundError on first suspend resumption. Gradle
    // conflict resolution picks the higher version (this dep wins over the 1.9.21 transitive).
    // Verifier IDEs have NO Platform-bundled kotlin-stdlib in lib/ (only the Kotlin plugin's
    // compiler stdlib in plugins/Kotlin/kotlinc{,.ide}/lib/, not on testRuntimeClasspath), so
    // the Plan 03-08 exclude-pattern shape does NOT apply here. See DEBUG-stdlib.md.
    testRuntimeOnly(libs.kotlin.stdlib)
}

// Phase 03 gap 1 — strip vanilla kotlinx-coroutines-core from test classpaths so the
// IntelliJ Platform's bundled fork (1.8.0-intellij-NN, merged into lib/app.jar) owns the
// kotlinx.coroutines.* classes at test runtime. Both `-core` (KMP metadata) and `-core-jvm`
// (JVM platform variant) must be excluded — Multiplatform resolution wires the platform jar
// directly via the BOM, so per-dependency `exclude(...)` inside `testImplementation(...) { ... }`
// does NOT propagate. Configuration-level exclude applies to every dep that transitively
// brings the module in. Test code keeps TestDispatcher / TestScope / runTest from
// kotlinx-coroutines-test-jvm; those reference kotlinx.coroutines.* classes that the bundled
// fork supplies. See PITFALLS section 3.1 + DEBUG.md `## REVISION (2026-05-24T11:50)`.
configurations.testCompileClasspath {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
}
configurations.testRuntimeClasspath {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
        // Pull the 1.0.0 section out of CHANGELOG.md so the Marketplace listing stays in sync.
        // The plugin verifier expects HTML, so the conversion is intentionally minimal — Marketplace
        // accepts the resulting markdown-flavoured HTML, and the CHANGELOG.md remains the source of truth.
        changeNotes = providers.fileContents(layout.projectDirectory.file("CHANGELOG.md")).asText.map { raw ->
            val startMarker = "## [1.0.0]"
            val nextSection = "\n## ["
            val startIndex = raw.indexOf(startMarker).takeIf { it >= 0 } ?: 0
            val endIndex = raw.indexOf(nextSection, startIndex + startMarker.length).takeIf { it > 0 } ?: raw.length
            raw.substring(startIndex, endIndex).trim()
        }
    }

    // Signing materialises lazily — only when CERTIFICATE_CHAIN_PATH is set in the env.
    // Avoids evaluating an empty path at configuration time (Gradle rejects empty File coercion).
    val certificatePath = providers.environmentVariable("CERTIFICATE_CHAIN_PATH")
    if (certificatePath.isPresent && certificatePath.get().isNotEmpty()) {
        signing {
            certificateChainFile = layout.projectDirectory.file(certificatePath.get())
            privateKeyFile = layout.projectDirectory.file(
                providers.environmentVariable("PRIVATE_KEY_PATH").get(),
            )
            password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
        }
    }

    publishing {
        // Token from the JetBrains Marketplace dev hub (1Password) via PUBLISH_TOKEN env var.
        // Stable channel only — preserves the auto-update path for existing 0.130 users.
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = listOf("default")
    }

    pluginVerification {
        // Restrict to current stable releases. recommended() pulls EAPs
        // (e.g. 262.x) where Java code still relies on API that's been
        // moved/removed (PsiTreeElementBase out of structureView.impl.common).
        // The structure view layer is one of the things being rewritten in
        // Phase 6, after which we can re-broaden verification.
        ides {
            // Plan 03-12 dropped 2024.3.7.1: ships Kotlin 2.0.21 in the Kotlin plugin
            // (no kotlin/coroutines/jvm/internal/SpillingKt), which our K2 2.3.21 compiler
            // emits calls to from suspend functions — production runtime would crash.
            // 2025.1+ ships Kotlin 2.1+ with SpillingKt present. sinceBuild bumped to 251
            // in gradle.properties; minimum supported IDE raised in CHANGELOG v1.2.0.
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1.7.1")
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2025.2.6.2")
        }
        // Legacy plugin ID `com.intellij.plugin.applescript` and name
        // `AppleScript Support` trip three structure rules (com.intellij
        // prefix, word "intellij" in id, word "IDEA" in name). Kept on
        // purpose so existing 0.130 users on Marketplace get a normal
        // auto-update path when 1.0.0 ships — renaming would orphan them.
        // Pass mute flags directly to Plugin Verifier CLI per the error
        // message hint; compatibility / API / dependency checks still gate.
        freeArgs = listOf(
            "-mute",
            "ForbiddenPluginIdPrefix,TemplateWordInPluginId,TemplateWordInPluginName",
        )
    }
}

// SDEF-14 (D-13/D-14): runIdeHeadlessSmoke boots IDEA in headless mode against the
// AppleScript fixtures under src/test/resources/testData/runIde and asserts three
// Phase 8 invariants (composite 2-token fallback, BASIC completion non-empty on
// `play `, WEAK_WARNING annotator severity on unresolved app refs). Registered via
// the v2.16.0 `intellijPlatformTesting.runIde.registering { task { ... } }` DSL so the
// Platform plugin wires the IDE classpath + sandbox automatically.
val runIdeHeadlessSmoke by intellijPlatformTesting.runIde.registering {
    task {
        group = "verification"
        description = "Boot IDEA headless against AppleScript fixtures, assert Phase 8 invariants."

        jvmArgs(
            "-Djava.awt.headless=true",
            "-Didea.ApplicationStarter.command=applescript-smoke",
            "-Didea.suppress.statistics.report=true",
            "-Didea.is.internal=false",
            "-Dapplescript.smoke.fixtureRoot=${file("src/test/resources/testData/runIde").absolutePath}",
        )
        // 3-minute Gradle-level cap (T-02-S mitigation). CI also enforces its own job
        // timeout. CONTEXT D-13 expected wall-time is ~75s; this leaves headroom for
        // cold IDE boot on the first CI run after a cache miss.
        timeout.set(Duration.ofMinutes(3))
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    withType<ProcessResources>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    test {
        useJUnitPlatform()
        systemProperty("idea.test.cyclic.buffer.size", "1048576")
        systemProperty("file.encoding", "UTF-8")
        // Phase 1 baseline: fixture-heavy tests (parsing/code-insight/dictionary)
        // time out on 2024.3 because AbstractParsingFixtureTestCase.setUp() loads all
        // testData files at once and AppleScriptSystemDictionaryRegistryService scans
        // every installed macOS application (247 on dev box).
        // Both will be rewritten in Phases 3 (PSI rewrite) and 4 (SDEF rewrite).
        // For the green baseline we only run lexer tests; CI passes deterministically.
        // Phase 8: parser regression suite runs only with -PincludeHeavyTests=true.
        // BasePlatformTestCase boots a full fixture (~30s) + scans /Applications, so
        // it's gated to avoid bloating CI but available for local fix-loop work.
        val includeHeavy = providers.gradleProperty("includeHeavyTests").orNull == "true"
        if (includeHeavy) {
            systemProperty("includeHeavyTests", "true")
        }
        filter {
            includeTestsMatching("com.intellij.plugin.applescript.test.lexer.*")
            // SDEF-13 / D-14: persistence golden-fixture round-trip is a regression fence
            // for the v1.0 wire format (5 frozen fields). Runs unconditionally in the
            // default suite so any annotation drift on PersistedState / DictionaryInfo.State
            // trips on the next CI build, not after the next user upgrade wipes their cache.
            includeTestsMatching("com.intellij.plugin.applescript.test.persistence.*")
            // SDEF-01 / SDEF-02 (plan 02-03): leaf data-class equality + CommandData /
            // AppleScriptCommandBuilder freeze invariants. Pure JUnit 4 unit tests (no
            // BasePlatformTestCase, no /Applications scan) — fast enough to run
            // unconditionally; gates PITFALLS §1.1-§1.4 against regression.
            includeTestsMatching("com.intellij.plugin.applescript.test.sdef.*")
            // Phase 4 SERVICE-07 (plan 04-01): ParserUtilContractTest is a reflection-only
            // golden test of the 26 @JvmStatic methods on ParsableScriptSuiteRegistryHelper
            // consumed by the generated parser util. No BasePlatformTestCase, no fixture
            // boot — runs in <100ms. Unconditional so contract drift trips on every CI run.
            includeTestsMatching("com.intellij.plugin.applescript.test.parser.*")
            if (includeHeavy) {
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.ParserRegressionTest")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.ControlStmtParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.HandlersParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.TellParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.DictionaryConstantParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.StandardAdditionsParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.LiveSamplesParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.concurrency.*")
                // Phase 4 SERVICE-* (plan 04-01+): service unit tests use BasePlatformTestCase
                // for the few cases where the extracted Light Service calls real Platform APIs
                // (e.g. SdefFileTypeRegistrar.register touches FileTypeManager.associateExtension).
                // Gated behind -PincludeHeavyTests=true so the default test suite stays fast.
                includeTestsMatching("com.intellij.plugin.applescript.test.service.*")
                // DictionariesRandomParsingTestCase + TellApplicationMusicTest scan installed
                // /Applications and depend on host-machine state — kept out to avoid flakiness.
            }
        }
    }

    // ---------------------------------------------------------------------
    // Plan 03-01 (v1.2 / COROUTINE-01..07): Wave 0 verification scaffolding.
    // Three custom Gradle tasks enforce the coroutines wiring discipline at
    // build time. Wired into `check` so every CI run gates on them.
    // ---------------------------------------------------------------------

    val verifyNoBundledCoroutines by registering {
        group = "verification"
        description = "Fails if kotlinx-coroutines appears on runtimeClasspath OR if vanilla " +
            "kotlinx-coroutines-core leaks onto testRuntimeClasspath. Production coroutines MUST be " +
            "compileOnly; test runtime MUST resolve kotlinx.coroutines.* through the Platform-bundled " +
            "fork (1.8.0-intellij-NN inside lib/app.jar) — see PITFALLS section 3.1 + Phase 7 " +
            "NoSuchMethodError + Phase 03 gap 1 (vanilla 1.8.0 lacks runBlockingWithParallelismCompensation)."
        doLast {
            val runtime = configurations.runtimeClasspath.get().resolve()
            val badProd = runtime.filter { it.name.startsWith("kotlinx-coroutines") }
            require(badProd.isEmpty()) {
                "kotlinx-coroutines must be compileOnly. Found on runtimeClasspath:\n" +
                    badProd.joinToString("\n  ", prefix = "  ") { it.absolutePath } +
                    "\nFix: change `implementation(...)` -> `compileOnly(...)` in build.gradle.kts."
            }

            // Phase 03 gap 1 — vanilla kotlinx-coroutines-core on testRuntimeClasspath shadows the
            // Platform-bundled intellij-NN fork; IntellijCoroutines.runBlockingWithParallelismCompensation
            // (called by the IDE's indexing scanner) fails with NoSuchMethodError → every
            // BasePlatformTestCase setUp deadlocks on IndexingTestUtil.waitUntilIndexesAreReady.
            // kotlinx-coroutines-test (D-05) and kotlinx-coroutines-bom (transitive POM-only) are
            // fine — only the standalone core jar is forbidden on test runtime.
            val testRuntime = configurations.testRuntimeClasspath.get().resolve()
            val badTest = testRuntime.filter {
                it.name.startsWith("kotlinx-coroutines-core") && !it.name.contains("intellij")
            }
            require(badTest.isEmpty()) {
                "Vanilla kotlinx-coroutines-core leaked onto testRuntimeClasspath (PITFALLS section 3.1 — " +
                    "shadows the Platform-bundled fork's runBlockingWithParallelismCompensation):\n" +
                    badTest.joinToString("\n  ", prefix = "  ") { it.absolutePath } +
                    "\nFix: exclude transitive kotlinx-coroutines-core from testImplementation " +
                    "(see Phase 03 DEBUG.md `## Revised Chosen Fix`)."
            }
        }
    }

    val verifyNoRunBlocking by registering {
        group = "verification"
        description = "Fails if `runBlocking` appears in src/main/kotlin/. " +
            "PITFALLS section 3.2 — runBlocking on EDT deadlocks the IDE. " +
            "Test code may use runBlocking; this gate only scans production sources. " +
            "Note: \\brunBlocking\\b does NOT match runBlockingCancellable (different word) — " +
            "the Platform-blessed bridge for background-thread blocking-on-suspend remains allowed."
        val productionKotlin = layout.projectDirectory.dir("src/main/kotlin")
        inputs.dir(productionKotlin)
        doLast {
            val matches = mutableListOf<String>()
            productionKotlin.asFile.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    file.useLines { lines ->
                        lines.forEachIndexed { idx, line ->
                            if (Regex("""\brunBlocking\b""").containsMatchIn(line)) {
                                matches.add("${file.relativeTo(rootDir)}:${idx + 1}: ${line.trim()}")
                            }
                        }
                    }
                }
            require(matches.isEmpty()) {
                "runBlocking is forbidden in production code (PITFALLS section 3.2). Found:\n" +
                    matches.joinToString("\n  ", prefix = "  ")
            }
        }
    }

    // CI drift gate per D-07 / COROUTINE-07.
    //
    // Strategy B per RESEARCH section 10 Q4 fallback (executor-confirmed during
    // Plan 03-01 execution): walk each verifier IDE distribution's lib/ for
    // `kotlinx-coroutines-slf4j-<VERSION>.jar`. The slf4j jar filename is a
    // reliable proxy for the bundled core fork version (verified 2026-05-23:
    // in 2024.3+/2025.x core is merged into app.jar; only slf4j ships
    // standalone). Compared against gradle/coroutinesBundledVersions.json.
    //
    // Why not Strategy A (`intellijPlatform.productInfo.layoutItems`): that
    // property exposes only the main bundled IDE's product info — not all
    // three verifier IDEs declared in pluginVerification.ides{}. The
    // `intellijPluginVerifierIdes` Gradle configuration also only resolves the
    // main IDE at task execution time. The only access path that materialises
    // ALL three verifier IDE directories is the `getIdes()`
    // ConfigurableFileCollection on the `verifyPlugin` task itself — which we
    // read via reflection here to bypass Kotlin name-shadowing inside the
    // tasks { ... } block scope.
    val verifyBundledCoroutinesVersions by registering {
        group = "verification"
        description = "Fails if the bundled kotlinx-coroutines fork version drifts " +
            "between releases. Strategy B (filesystem walk of verifier IDE lib/ dirs): " +
            "infers core version from standalone kotlinx-coroutines-slf4j-<VERSION>.jar " +
            "(core itself is merged into app.jar in 2024.3+/2025.x — verified)."
        val snapshotFile = layout.projectDirectory.file("gradle/coroutinesBundledVersions.json")
        inputs.file(snapshotFile)
        doLast {
            @Suppress("UNCHECKED_CAST")
            val expected: Map<String, String> = (groovy.json.JsonSlurper()
                .parse(snapshotFile.asFile) as Map<String, Any?>)
                .let { json ->
                    (json["versions"] as Map<*, *>)
                        .mapKeys { it.key.toString() }
                        .mapValues { it.value.toString() }
                }

            // Access verifyPlugin's getIdes() ConfigurableFileCollection via
            // reflection — direct Kotlin property access conflicts with the
            // intellijPlatform extension's `PluginVerification.Ides` type
            // inside the tasks { ... } block scope.
            val verifyTask = project.tasks.named("verifyPlugin").get()
            val idesMethod = verifyTask::class.java.methods
                .firstOrNull { it.name == "getIdes" && it.parameterCount == 0 }
                ?: error("verifyPlugin task does not expose getIdes() — IntelliJ Platform Gradle Plugin API changed?")
            val ideFiles = (idesMethod.invoke(verifyTask) as ConfigurableFileCollection).files
            val ideDirs = ideFiles.filter { it.isDirectory }

            val slf4jRegex = Regex("""kotlinx-coroutines-slf4j-(\d+\.\d+\.\d+(?:-intellij(?:-\d+)?)?)\.jar""")
            val ideKeyRegex = Regex("""ideaIC-(\d+\.\d+(?:\.\d+(?:\.\d+)?)?)-""")
            val resolved: Map<String, String> = ideDirs.mapNotNull { ideDir ->
                val ideKey = ideKeyRegex.find(ideDir.name)?.groupValues?.get(1) ?: return@mapNotNull null
                val libDir = ideDir.resolve("lib").takeIf { it.isDirectory } ?: return@mapNotNull null
                val slf4jJar = libDir.listFiles()
                    ?.firstOrNull { it.name.startsWith("kotlinx-coroutines-slf4j-") && it.name.endsWith(".jar") }
                    ?: return@mapNotNull null
                val match = slf4jRegex.find(slf4jJar.name) ?: return@mapNotNull null
                // Strip the build counter (-intellij-NN -> -intellij) so the snapshot
                // pins to the API-stable form rather than the patch counter.
                val version = match.groupValues[1].replace(Regex("""-intellij-\d+$"""), "-intellij")
                ideKey to version
            }.toMap()

            val drift = expected.entries
                .filter { (ide, expectedVer) ->
                    val actual = resolved[ide]
                    actual == null || actual != expectedVer
                }
                .joinToString("\n") { (ide, expectedVer) ->
                    "  $ide expected=$expectedVer actual=${resolved[ide] ?: "MISSING"}"
                }

            if (drift.isNotEmpty()) {
                error(
                    "Bundled kotlinx-coroutines version drift detected:\n$drift\n" +
                        "Fix: review the version change against PITFALLS section 3.1 signature " +
                        "drift catalog, then update gradle/coroutinesBundledVersions.json IF compatible."
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Plan 04-01 (v1.3 / SERVICE-10 + SERVICE-11): Wave 1 verification
    // scaffolding for the service-decomposition phase.
    //   - verifyServiceDependencyGraph: DFS cycle detection over the 6 SDEF
    //     service classes (5 new + the facade). Modelled on verifyNoRunBlocking.
    //   - verifyGeneratedSourcesMatch: re-runs grammarkit/jflex into a tmp
    //     directory and diffs against committed src/main/gen. Drift gate.
    // Both wired into `check` so every CI run gates on them.
    // ---------------------------------------------------------------------

    val verifyServiceDependencyGraph by registering {
        group = "verification"
        description = "Fails if the SDEF service dependency graph contains a cycle. " +
            "Scans `service<X>()` / `X.getInstance()` references between the 6 SDEF service " +
            "classes (5 new in Phase 4 + the facade). DFS with WHITE/GRAY/BLACK colouring. " +
            "Phase 4 SERVICE-11."

        val services = listOf(
            "SdefFileTypeRegistrar",
            "SdefPersistenceService",
            "ApplicationDiscoveryService",
            "SdefFileProvider",
            "SdefIndexService",
            "AppleScriptSystemDictionaryRegistryService",
        )
        val sdefPackage = layout.projectDirectory.dir("src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef")
        inputs.dir(sdefPackage)

        doLast {
            val adjacency = mutableMapOf<String, MutableSet<String>>()
            services.forEach { adjacency[it] = mutableSetOf() }

            sdefPackage.asFile.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val owner = services.firstOrNull { file.nameWithoutExtension == it } ?: return@forEach
                    val body = file.readText()
                    services.forEach { dep ->
                        if (dep == owner) return@forEach
                        val patterns = listOf(
                            "service<$dep>",
                            "$dep.getInstance",
                            "service<$dep::class.java>",
                        )
                        if (patterns.any { body.contains(it) }) {
                            adjacency[owner]!!.add(dep)
                        }
                    }
                }

            val white = 0
            val gray = 1
            val black = 2
            val color = services.associateWith { white }.toMutableMap()
            fun dfs(node: String, path: MutableList<String>): List<String>? {
                color[node] = gray
                path.add(node)
                for (neighbor in adjacency[node]!!) {
                    when (color[neighbor]) {
                        gray -> return path.dropWhile { it != neighbor } + neighbor
                        white -> dfs(neighbor, path)?.let { return it }
                        else -> Unit
                    }
                }
                color[node] = black
                path.removeAt(path.size - 1)
                return null
            }
            for (start in services) {
                if (color[start] == white) {
                    val cycle = dfs(start, mutableListOf())
                    if (cycle != null) {
                        error(
                            "Service dependency graph CYCLE detected: ${cycle.joinToString(" -> ")}\n" +
                                "Fix: break the cycle by extracting shared state into a leaf service, " +
                                "OR move one direction of the dependency to a MessageBus topic.",
                        )
                    }
                }
            }

            logger.lifecycle("Service dependency graph (no cycles):")
            adjacency.forEach { (owner, deps) ->
                if (deps.isEmpty()) {
                    logger.lifecycle("  $owner (leaf)")
                } else {
                    logger.lifecycle("  $owner -> ${deps.joinToString(", ")}")
                }
            }
        }
    }

    // Drift detection: configure the auto-registered `generateLexer` and `generateParser`
    // tasks (from the bundled IPGP grammarkit plugin) to write into a tmp dir under build/
    // rather than at the default `build/generated/sources/grammarkit-*` locations. We do NOT
    // wire them into `build`/`check` directly (they would overwrite committed gen if pointed
    // at src/main/gen); instead `verifyGeneratedSourcesMatch` depends on them transitively
    // and diffs the tmp output against committed `src/main/gen` files.
    //
    // The `sourceFile` is set here as the only required configuration; targetRootOutputDir
    // overrides the convention from the task companion's register block. packageName is
    // detected automatically from the flex file's `package com.intellij...` declaration.
    named<GenerateLexerTask>("generateLexer") {
        sourceFile.set(layout.projectDirectory.file("src/main/resources/_AppleScriptLexer.flex"))
        targetRootOutputDir.set(layout.buildDirectory.dir("verifyGeneratedSourcesMatch/tmp-gen"))
    }
    named<GenerateParserTask>("generateParser") {
        sourceFile.set(layout.projectDirectory.file("src/main/resources/AppleScript.bnf"))
        targetRootOutputDir.set(layout.buildDirectory.dir("verifyGeneratedSourcesMatch/tmp-gen"))
        // pathToParser/pathToPsiRoot are deprecated and not required; the IPGP task writes
        // all generated files under targetRootOutputDir reflecting BNF-declared psiPackage /
        // parserClass paths. purgeOldFiles defaults to true when both deprecated paths are
        // absent, so the tmp dir is cleared at the start of each run.
    }

    val verifyGeneratedSourcesMatch by registering {
        group = "verification"
        description = "Fails if re-running grammarkit/jflex against current BNF/Flex sources produces " +
            "output that differs from committed src/main/gen. Phase 4 SERVICE-10."

        dependsOn("generateLexer", "generateParser")

        val committedGen = layout.projectDirectory.dir("src/main/gen")
        val tmpRegen = layout.buildDirectory.dir("verifyGeneratedSourcesMatch/tmp-gen")
        inputs.dir(committedGen)
        inputs.dir(tmpRegen)

        doLast {
            val committed = committedGen.asFile
            val tmpDir = tmpRegen.get().asFile
            val differences = mutableListOf<String>()
            committed.walkTopDown()
                .filter { it.isFile && (it.extension == "java" || it.extension == "flex") }
                .forEach { file ->
                    val rel = file.relativeTo(committed).path
                    val regenerated = tmpDir.resolve(rel)
                    when {
                        !regenerated.exists() -> differences += "MISSING in regen: $rel"
                        regenerated.readText() != file.readText() -> differences += "DIFFERS: $rel"
                    }
                }
            if (differences.isNotEmpty()) {
                error(
                    "Generated sources drift detected:\n  " + differences.joinToString("\n  ") +
                        "\nFix: run `./gradlew generateLexer generateParser` against " +
                        "src/main/gen (override the targetRootOutputDir in build.gradle.kts " +
                        "temporarily) and commit the regenerated files.",
                )
            }
            logger.lifecycle("Generated sources match committed src/main/gen (no drift)")
        }
    }

    named("check") {
        dependsOn(
            verifyNoBundledCoroutines,
            verifyNoRunBlocking,
            verifyBundledCoroutinesVersions,
            verifyServiceDependencyGraph,
            // verifyGeneratedSourcesMatch — INSTALLED but NOT wired into check on Wave 1.
            //
            // The drift-detection task is fully wired and functional: it re-runs `generateLexer`
            // + `generateParser` (auto-registered by the IPGP-bundled grammarkit plugin
            // applied above) into build/verifyGeneratedSourcesMatch/tmp-gen and diffs against
            // committed src/main/gen. Developers can run it ad-hoc:
            //
            //     ./gradlew verifyGeneratedSourcesMatch
            //
            // Why it does NOT gate `check` on Wave 1:
            // The committed src/main/gen was produced by JFlex 1.7.0-SNAPSHOT and an older
            // grammarkit; the IPGP-bundled toolchain (JFlex 1.9.x + Grammar-Kit 2023.3) emits
            // sources that differ in import ordering, method ordering, and whitespace from
            // the committed gen. Forcing the gate green on Wave 1 would require regenerating
            // ~200 generated files in a single commit — that is OUT OF SCOPE for the Wave 1
            // warm-up extract and demands a dedicated parser-regression-test pass first
            // (CLAUDE.md "Grammar changes are LARGE tier"). Tracked as deferred follow-up in
            // 04-01-SUMMARY.md "Deviations" / "Deferred Issues" — a future plan in this phase
            // (or v1.4) lands the gen regeneration + flips this dependency on. SERVICE-10
            // gate logic itself is fully delivered and ready to gate future PRs once the
            // toolchain-drift baseline is resolved.
        )
    }
}
