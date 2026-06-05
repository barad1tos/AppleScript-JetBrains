import io.gitlab.arturbosch.detekt.Detekt
import java.time.Duration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
// Phase 4 SERVICE-10: imports for the bundled grammarkit task types from IPGP 2.16.0.
// The bundled variant lives under org.jetbrains.intellij.platform.gradle.tasks,
// NOT under org.jetbrains.grammarkit.tasks (that's the standalone plugin).
import org.jetbrains.intellij.platform.gradle.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.tasks.GenerateParserTask

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
    // Phase 7 CLEANUP-04 / D-07: detekt + ktlint static analysis. Staged here (version-catalog
    // aliases) and wired into `check` (see the `named("check")` block below — plan 07-04) so the
    // grandfather baseline reflects the cleaned tree. detekt runs SOURCE-ONLY
    // (no `classpath`) because 1.23.8 bundles Kotlin compiler 2.0.0 and cannot read the
    // project's Kotlin 2.3.21 metadata (RESEARCH Pitfall 2). Build-time-only; never in the ZIP.
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
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
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
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

tasks.named<ProcessResources>("processResources") {
    from(rootProject.layout.projectDirectory.file("LICENSE")) {
        into("META-INF")
    }
    from(rootProject.layout.projectDirectory.file("NOTICE")) {
        into("META-INF")
    }
    from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
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
    constraints {
        implementation(libs.commons.lang3) {
            because("CVE-2025-48924 affects commons-lang3 before 3.18.0")
        }
    }

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

// Phase 7 CLEANUP-04 / D-07: detekt + ktlint static analysis, staged source-only.
// SOURCE-ONLY mode — NO `classpath` / type-resolution is set on the detekt task. detekt
// 1.23.8 bundles Kotlin compiler 2.0.0 which cannot read this project's Kotlin 2.3.21
// metadata; type-resolution would flood false positives + emit the
// "was compiled with an incompatible version of Kotlin" warning (RESEARCH Pitfall 2).
// The baseline (detekt-baseline.xml) grandfathers existing findings so only NEW code gates.
// Wired into `check` (plan 07-04, see the `named("check")` block below); the regenerated
// baseline reflects the cleaned tree.
detekt {
    config.setFrom(files("detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

// Frozen-surface guard: src/main/gen is on the main sourceSet (build.gradle.kts:51), so detekt
// scans the ~250 generated parser/PSI files without this exclusion. The generated surface is
// regenerated from BNF/Flex and must never be pressured by a code-smell gate (project rules).
tasks.withType<Detekt>().configureEach {
    exclude("**/gen/**")
}

// ktlint formatting uses the standard ruleset plus project policy from .editorconfig.
// Do NOT run a tree-wide ktlintFormat (RESEARCH Pitfall 5 — would churn 100+ files);
// existing violations are grandfathered via the ktlint baseline (config/ktlint/baseline.xml).
// Keep ktlint-only keys in Gradle because IntelliJ flags them as unsupported in .editorconfig.
ktlint {
    additionalEditorconfig.set(
        mapOf(
            "ktlint_code_style" to "ktlint_official",
            "ktlint_standard_function-expression-body" to "disabled",
        ),
    )
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "com.intellij.plugin.applescript.lang.parser.AppleScriptParser*",
                )
            }
        }

        total {
            xml {
                onCheck = false
                xmlFile = layout.buildDirectory.file("reports/kover/report.xml")
            }
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
        // Pull the current release section out of CHANGELOG.md so the Marketplace listing stays in sync.
        // The plugin verifier expects HTML, so the conversion is intentionally minimal — Marketplace
        // accepts the resulting markdown-flavoured HTML, and the CHANGELOG.md remains the source of truth.
        changeNotes =
            providers.fileContents(layout.projectDirectory.file("CHANGELOG.md")).asText.map { raw ->
                val startMarker = "## [$version]"
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
            privateKeyFile =
                layout.projectDirectory.file(
                    providers.environmentVariable("PRIVATE_KEY_PATH").get(),
                )
            password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
        }
    }

    publishing {
        // Token from the JetBrains Marketplace dev hub (1Password) via PUBLISH_TOKEN env var.
        // Stable channel only; the first Marketplace upload can still be hidden in the admin UI.
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
            "-Didea.suppress.statistics.report=true",
            "-Didea.is.internal=false",
            "-Dapplescript.smoke.fixtureRoot=${file("src/test/resources/testData/runIde").absolutePath}",
        )
        args("applescript-smoke")
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
        // Phase 7 CLEANUP-03 / D-01: the heavy fixture suite now runs by DEFAULT so CI
        // catches parser/concurrency/service/codeinsight regressions on every PR. Opt OUT
        // for fast local fix-loops with `-PskipHeavyTests=true` (light suite only: lexer/
        // persistence/sdef/parser/psi). This inverts the former `-PincludeHeavyTests` opt-IN
        // gate. BasePlatformTestCase still boots a full fixture (~30s) + scans /Applications;
        // the 11 baseline-RED drifted parsing methods are disabled in-method (Phase 8 /
        // PARSE-07) and the 2 host-state tests stay excluded by omission (see end of filter).
        //
        // The `includeHeavyTests` SYSTEM property is still set when heavy runs, because the
        // concurrency.* tests self-gate on `System.getProperty("includeHeavyTests") == "true"`
        // via an in-setUp `Assume.assumeTrue(...)`. Dropping it would let the filter select
        // those classes but then skip them internally — heavy-by-default would be a no-op for
        // the concurrency suite. Keeping the system property keyed to `!skipHeavy` preserves
        // the in-test gate without touching ~10 test files (out of D-01 scope).
        val skipHeavy = providers.gradleProperty("skipHeavyTests").orNull == "true"
        if (!skipHeavy) {
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
            // Parser tests include the generated parser-util JVM signature guard. No
            // BasePlatformTestCase, no fixture boot — fast enough to run on every CI build.
            includeTestsMatching("com.intellij.plugin.applescript.test.parser.*")
            // Phase 5 PSI-03 (plan 05-01): PsiGetterJvmSignatureTest is the reflection-only
            // guard for Java-visible getter names (getX/isX/setX) produced by converting
            // GROUP A interface getters to Kotlin properties. No BasePlatformTestCase, no
            // fixture boot — runs in <100ms. Unconditional so a property conversion that
            // drops/renames a Java-reachable accessor trips on every CI run, not after a
            // runtime NoSuchMethodError.
            includeTestsMatching("com.intellij.plugin.applescript.test.psi.*")
            if (!skipHeavy) {
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.DictionariesRandomParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.TellApplicationMusicTest")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.ParserRegressionTest")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.FallbackCommandParameterParserTest")
                // Phase 8 PARSE-01 (plan 08-01): RealWorldCorpusTest is the v2.0
                // "corpus is the contract" harness — realistic production-shaped
                // scripts asserting zero PsiErrorElement. BasePlatformTestCase boots
                // a full fixture (~30s), so it belongs in the heavy-by-default gate
                // next to ParserRegressionTest (Phase 7 CLEANUP-03 opt-OUT model).
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.RealWorldCorpusTest")
                // Phase 8 PARSE-04 (plan 08-05): StandardAdditionsTokensTest gates the
                // explicit Standard Additions productions (current date / ASCII character /
                // ASCII number / path to <constant>) to zero PsiErrorElement. Same
                // BasePlatformTestCase heavy-by-default model as RealWorldCorpusTest above.
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.StandardAdditionsTokensTest")
                // Phase 8 PARSE-03 (plan 08-06): ApplicationObjectReferenceTest gates the generic
                // application_object_reference production (library playlist N / current track /
                // track N of …) to zero PsiErrorElement, SDEF-independently. Same heavy-by-default
                // BasePlatformTestCase model as the corpus tests above.
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.ApplicationObjectReferenceTest")
                // Phase 9 broad object/property specifier recovery: compact cold parser
                // fixture for direct constructors, contextual keyword terms, and path domains.
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.ObjectPropertySpecifierTest")
                // Phase 8 PARSE-02/06 (plan 08-07): whose-clause / nested-tell / on-error
                // hardening harnesses, verify-first against the migrated GK 2023.3 parser.
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.WhoseClauseTest")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.TellBlockHardeningTest")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.OnErrorBlockTest")
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
                // Phase 6 D-03 (plan 06-03): AppleScriptCodeInsightTest is BasePlatformTestCase-heavy
                // (boots a full fixture, scans the bundled StandardAdditions/CocoaStandard SDEF for
                // completion). It ran in NO CI filter before Phase 6 — wiring it here is what makes the
                // D-03 content-anchor redesign actually execute. Gated under includeHeavy so the
                // default suite stays fast.
                includeTestsMatching("com.intellij.plugin.applescript.test.codeinsight.*")
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
            val badTest =
                testRuntime.filter {
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
            productionKotlin.asFile
                .walkTopDown()
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
    register("verifyBundledCoroutinesVersions") {
        group = "verification"
        description = "Fails if the bundled kotlinx-coroutines fork version drifts " +
            "between releases. Strategy B (filesystem walk of verifier IDE lib/ dirs): " +
            "infers core version from standalone kotlinx-coroutines-slf4j-<VERSION>.jar " +
            "(core itself is merged into app.jar in 2024.3+/2025.x — verified)."
        // `verifyPlugin` materialises the verifier IDE directories that this task inspects.
        // Keep this dependency local to the release/CI drift gate instead of wiring the task into
        // `check`, where clean builds would otherwise pay for full plugin verification.
        dependsOn("verifyPlugin")
        val snapshotFile = layout.projectDirectory.file("gradle/coroutinesBundledVersions.json")
        inputs.file(snapshotFile)
        doLast {
            val snapshotJson =
                groovy.json
                    .JsonSlurper()
                    .parse(snapshotFile.asFile)
            check(snapshotJson is Map<*, *>) {
                "Bundled coroutines snapshot must be a JSON object: ${snapshotFile.asFile}"
            }
            val snapshotVersions = snapshotJson["versions"]
            check(snapshotVersions is Map<*, *>) {
                "Bundled coroutines snapshot must contain a 'versions' object: ${snapshotFile.asFile}"
            }
            val expected: Map<String, String> =
                snapshotVersions
                    .mapKeys { it.key.toString() }
                    .mapValues { it.value.toString() }

            // Access verifyPlugin's getIdes() ConfigurableFileCollection via
            // reflection — direct Kotlin property access conflicts with the
            // intellijPlatform extension's `PluginVerification.Ides` type
            // inside the tasks { ... } block scope.
            val verifyTask = project.tasks.named("verifyPlugin").get()
            val idesMethod =
                verifyTask::class.java.methods
                    .firstOrNull { it.name == "getIdes" && it.parameterCount == 0 }
                    ?: error(
                        "verifyPlugin task does not expose getIdes() — " +
                            "IntelliJ Platform Gradle Plugin API changed?",
                    )
            val ideFiles = (idesMethod.invoke(verifyTask) as ConfigurableFileCollection).files
            val ideDirs = ideFiles.filter { it.isDirectory }

            val slf4jRegex = Regex("""kotlinx-coroutines-slf4j-(\d+\.\d+\.\d+(?:-intellij(?:-\d+)?)?)\.jar""")
            val ideKeyRegex = Regex("""ideaIC-(\d+\.\d+(?:\.\d+(?:\.\d+)?)?)(?:-|$)""")
            val resolved: Map<String, String> =
                ideDirs
                    .mapNotNull { ideDir ->
                        val ideKey = ideKeyRegex.find(ideDir.name)?.groupValues?.get(1) ?: return@mapNotNull null
                        val libDir = ideDir.resolve("lib").takeIf { it.isDirectory } ?: return@mapNotNull null
                        val slf4jJar =
                            libDir
                                .listFiles()
                                ?.firstOrNull { file ->
                                    file.name.startsWith("kotlinx-coroutines-slf4j-") &&
                                        file.name.endsWith(".jar")
                                }
                                ?: return@mapNotNull null
                        val match = slf4jRegex.find(slf4jJar.name) ?: return@mapNotNull null
                        // Strip the build counter (-intellij-NN -> -intellij) so the snapshot
                        // pins to the API-stable form rather than the patch counter.
                        val version = match.groupValues[1].replace(Regex("""-intellij-\d+$"""), "-intellij")
                        ideKey to version
                    }.toMap()

            val drift =
                expected.entries
                    .filter { (ide, expectedVer) ->
                        val actual = resolved[ide]
                        actual == null || actual != expectedVer
                    }.joinToString("\n") { (ide, expectedVer) ->
                        "  $ide expected=$expectedVer actual=${resolved[ide] ?: "MISSING"}"
                    }

            if (drift.isNotEmpty()) {
                error(
                    "Bundled kotlinx-coroutines version drift detected:\n$drift\n" +
                        "Fix: review the version change against PITFALLS section 3.1 signature " +
                        "drift catalog, then update gradle/coroutinesBundledVersions.json IF compatible.",
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Plan 04-01 (v1.3 / SERVICE-10 + SERVICE-11): Wave 1 verification
    // scaffolding for the service-decomposition phase.
    //   - verifyServiceDependencyGraph: DFS cycle detection over dictionary
    //     service classes and their extracted helper files. Modelled on verifyNoRunBlocking.
    //   - verifyGeneratedSourcesMatch: re-runs grammarkit/jflex into a tmp
    //     directory and diffs against committed src/main/gen. Drift gate.
    // Both wired into `check` so every CI run gates on them.
    // ---------------------------------------------------------------------

    val verifyServiceDependencyGraph by registering {
        group = "verification"
        description = "Fails if the SDEF service dependency graph contains a cycle. " +
            "Scans `service<X>()` / `X.getInstance()` references between dictionary service " +
            "classes and owned helpers. DFS with WHITE/GRAY/BLACK colouring. " +
            "Phase 4 SERVICE-11."

        fun serviceWithOwnedFiles(
            service: String,
            vararg ownedFiles: String,
        ): Map<String, String> = (listOf(service) + ownedFiles).associateWith { service }

        val serviceOwnerByFile =
            serviceWithOwnedFiles("SdefFileTypeRegistrar") +
                serviceWithOwnedFiles("SdefPersistenceService") +
                serviceWithOwnedFiles("ApplicationDiscoveryService") +
                serviceWithOwnedFiles("XcodeDetectionService") +
                serviceWithOwnedFiles(
                    "SdefFileProvider",
                    "ScriptingAdditionsMerger",
                    "SdefDictionaryFileGenerator",
                    "SdefFileResources",
                ) +
                serviceWithOwnedFiles(
                    "SdefIndexService",
                    "SdefIndexStore",
                ) +
                serviceWithOwnedFiles(
                    "AppleScriptSystemDictionaryRegistryService",
                    "DictionaryInitializationCoordinator",
                    "DictionaryRegistries",
                    "StandardDictionaryInitializer",
                )
        val services = serviceOwnerByFile.values.distinct()
        val filesOutsideAppServiceGraph =
            setOf(
                // Project-level dictionary cache reads the app-level registry, while SdefIndexService
                // can consult project dictionaries from query paths. Keep that lifecycle boundary
                // explicit instead of modelling project and app services as one cycle graph.
                "src/main/kotlin/com/intellij/plugin/applescript/lang/dictionary/project/" +
                    "AppleScriptProjectDictionaryService.kt",
            )

        fun serviceLookupPatterns(dep: String): List<String> =
            listOf(
                "service<$dep>",
                "$dep.getInstance",
                "getService($dep::class.java)",
            )

        val serviceLookupPatternsByService = services.associateWith(::serviceLookupPatterns)
        val serviceAnnotationPattern = Regex("""(^|\s)@Service(\s|\()""")

        // Phase 4 SERVICE-02 (Wave 2) data-hop allowlist. Pairs of (owner, dep) where the
        // back-edge from a service to the facade is a DATA dependency (reading state.X), not
        // a service-graph dependency. RESEARCH §5 calls this out explicitly: "the back-edge
        // from a service to the facade is modelled as a data hop, not a service hop" — the
        // facade owns the @State-tagged PersistedState field, and the typed-API service reads
        // it. Without this allowlist Pattern A (RESEARCH §2) is impossible to model.
        //
        // Add new entries here as later waves (3-5) introduce more services that read state
        // via the facade. Never add an entry for a service-to-service edge that is a real
        // service<X>() lookup hop — those are real service dependencies and MUST be modelled
        // as graph edges so cycles are caught.
        val dataHopAllowlist =
            setOf(
                "SdefPersistenceService" to "AppleScriptSystemDictionaryRegistryService",
                // Wave 3 (Phase 4 SERVICE-03, plan 04-03): SdefPersistenceService.isInUnknownList
                // is a back-compat shim that forwards to ApplicationDiscoveryService — the not-found
                // list moved to the discovery service (its rightful owner; it's a session-only
                // discovery artifact, NOT a persistence artifact). The forwarder preserves the
                // public surface of SdefPersistenceServiceTest (Wave 2) without violating the
                // single-source-of-truth invariant. This is conceptually a session-data forwarder,
                // NOT a service-graph dependency. Without this entry the cycle detector flags
                // `SdefPersistenceService -> ApplicationDiscoveryService -> SdefPersistenceService`
                // (ApplicationDiscoveryService consults SdefPersistenceService.isNotScriptable
                // during discovery — that direction IS a real service dependency and remains
                // tracked in the graph).
                "SdefPersistenceService" to "ApplicationDiscoveryService",
                // Wave 4 (Phase 4 SERVICE-04, plan 04-04): SdefFileProvider reaches back into the
                // facade for two narrow data-hop reads:
                //   1. AppleScriptSystemDictionaryRegistryService.getDictionaryInfoByNameInternal(name) —
                //      O(1) lookup against the persisted @State-tagged dictionaryInfoMap. The facade is
                //      the persisted-state owner (Pattern A — annotation tied to COMPONENT_NAME by
                //      class identity; cannot move without breaking existing user caches per
                //      PITFALLS 4.1). Wave 4 reads through this typed accessor rather than copying
                //      the snapshot on every fetch.
                //   2. AppleScriptSystemDictionaryRegistryService.initializeDictionaryFromInfoInternal —
                //      delegates the parse step (parseDictionaryFile + map population) back to the
                //      facade because the parser-index map cluster is Wave 5 SdefIndexService
                //      territory; Wave 4 only owns file-generation.
                //   3. AppleScriptSystemDictionaryRegistryService.newSecureSaxBuilderInternal —
                //      XXE-hardened SAXBuilder factory. The other consumer (parseDictionaryFile) still
                //      lives on the facade; co-location with the file-provider's mergeScriptingAdditions
                //      moves with the parseDictionaryFile extraction in Wave 5.
                // All three are DATA reads — the facade does not depend on SdefFileProvider's
                // session-only file-generation state. Wave 5 may eliminate this allowlist entry once
                // parseDictionaryFile + the parser map cluster migrate to SdefIndexService.
                "SdefFileProvider" to "AppleScriptSystemDictionaryRegistryService",
            )
        val serviceSourceRoots =
            listOf(
                layout.projectDirectory.dir("src/main/kotlin/com/intellij/plugin/applescript/lang/ide/sdef"),
                layout.projectDirectory.dir("src/main/kotlin/com/intellij/plugin/applescript/lang/dictionary"),
            )
        serviceSourceRoots.forEach { inputs.dir(it) }

        doLast {
            val adjacency = mutableMapOf<String, MutableSet<String>>()
            services.forEach { adjacency[it] = mutableSetOf() }

            serviceSourceRoots.forEach { serviceSourceRoot ->
                serviceSourceRoot.asFile
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .forEach { file ->
                        val body = file.readText()
                        val owner = serviceOwnerByFile[file.nameWithoutExtension]
                        if (owner == null) {
                            val relativePath = file.relativeTo(projectDir).invariantSeparatorsPath
                            val hasTrackedServiceLookup =
                                serviceLookupPatternsByService.values.any { patterns ->
                                    patterns.any { body.contains(it) }
                                }
                            val declaresService = serviceAnnotationPattern.containsMatchIn(body)
                            if (
                                relativePath !in filesOutsideAppServiceGraph &&
                                (declaresService || hasTrackedServiceLookup)
                            ) {
                                error(
                                    "Unowned service-graph file detected: ${file.relativeTo(projectDir)}\n" +
                                        "Fix: add it to serviceOwnerByFile, move it outside scanned roots, " +
                                        "or document it in filesOutsideAppServiceGraph.",
                                )
                            }
                            return@forEach
                        }
                        services.forEach { dep ->
                            if (dep == owner) return@forEach
                            // Skip data-hop edges (RESEARCH §5).
                            if (owner to dep in dataHopAllowlist) return@forEach
                            val patterns = serviceLookupPatternsByService.getValue(dep)
                            if (patterns.any { body.contains(it) }) {
                                adjacency[owner]!!.add(dep)
                            }
                        }
                    }
            }

            val white = 0
            val gray = 1
            val black = 2
            val color = services.associateWith { white }.toMutableMap()

            fun dfs(
                node: String,
                path: MutableList<String>,
            ): List<String>? {
                color[node] = gray
                path.add(node)
                var cycle: List<String>? = null
                for (neighbor in adjacency[node]!!) {
                    when (color[neighbor]) {
                        gray -> {
                            cycle = path.dropWhile { it != neighbor } + neighbor
                        }

                        white -> {
                            cycle = dfs(neighbor, path)
                        }

                        else -> Unit
                    }
                    if (cycle != null) break
                }
                if (cycle == null) {
                    color[node] = black
                    path.removeAt(path.size - 1)
                }
                return cycle
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
            if (dataHopAllowlist.isNotEmpty()) {
                logger.lifecycle("Data-hop edges (allowlisted — NOT counted as service-graph edges):")
                dataHopAllowlist.forEach { (owner, dep) ->
                    logger.lifecycle("  $owner --data--> $dep")
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
    //
    // Bootstrap-regen procedure (08-04 toolchain migration). `./gradlew build` does NOT regenerate
    // the committed `src/main/gen` — it is a static artifact. To regenerate it on the bundled
    // toolchain (JFlex 1.9.2 + Grammar-Kit 2023.3), resolving the chicken-and-egg where Grammar-Kit
    // must read the COMPILED Kotlin psiImplUtil before it can emit gen:
    //   1. With committed gen present, run `compileKotlin compileJava` so `build/classes/kotlin/main`
    //      + `build/classes/java/main` exist (the classpath the `generateParser` task appends below,
    //      so Grammar-Kit discovers the @JvmStatic AppleScriptPsiImplUtil methods → 0 skip-warnings).
    //   2. Run `generateLexer` + `generateParser` to emit the new gen.
    //   3. Assemble both generated trees into `src/main/gen` (gen is on the main source set, line ~59).
    // This is a one-time bootstrap: `targetRootOutputDir` below stays pinned at
    // `build/verifyGeneratedSourcesMatch/tmp-gen` so `verifyGeneratedSourcesMatch` keeps diffing a
    // fresh regen against committed gen (the drift gate). Retarget to `src/main/gen` only transiently
    // during the bootstrap regen, then restore.
    named<GenerateLexerTask>("generateLexer") {
        sourceFile.set(layout.projectDirectory.file("src/main/resources/_AppleScriptLexer.flex"))
        // SEPARATE tmp dir from generateParser. BOTH the JFlex and Grammar-Kit tasks purge their
        // targetRootOutputDir at the start of each run, so they cannot share one dir (whichever runs
        // last wipes the other's output → false MISSING drift). The lexer regen goes to tmp-gen-lexer;
        // the parser/PSI regen goes to tmp-gen (below). verifyGeneratedSourcesMatch resolves each
        // committed file against BOTH dirs.
        targetRootOutputDir.set(layout.buildDirectory.dir("verifyGeneratedSourcesMatch/tmp-gen-lexer"))
    }
    named<GenerateParserTask>("generateParser") {
        sourceFile.set(layout.projectDirectory.file("src/main/resources/AppleScript.bnf"))
        targetRootOutputDir.set(layout.buildDirectory.dir("verifyGeneratedSourcesMatch/tmp-gen"))
        // The classpath(...) append below consumes compileKotlin + compileJava outputs
        // (build/classes/{kotlin,java}/main), so declare the dependency explicitly — Gradle 9.5
        // fails the build otherwise (implicit-dependency validation). This also makes the
        // bootstrap-regen order automatic: compile (over committed gen) runs before generateParser,
        // so the classpath holds the compiled AppleScriptPsiImplUtil. No cycle: generateParser writes
        // to tmp-gen, never src/main/gen, so compile (which reads committed src/main/gen) does not
        // depend back on it.
        dependsOn("compileKotlin", "compileJava")
        // GenerateParserTask extends JavaExec — APPEND the compiled main classes to the inherited
        // task classpath via the JavaExec classpath(..) method (NOT the FileCollection-property
        // setter form, which fails because that property is a plain FileCollection, not appendable).
        // This lets Grammar-Kit 2023.3 load
        // the compiled Kotlin `AppleScriptPsiImplUtil` object and discover its @JvmStatic methods at
        // generation time, eliminating the "methods are not found in AppleScriptPsiImplUtil" skip-
        // warnings (and the 20 downstream Unresolved-reference compile errors). Requires step 1 of
        // the bootstrap-regen procedure above to have produced these dirs first.
        classpath(
            layout.buildDirectory.dir("classes/kotlin/main"),
            layout.buildDirectory.dir("classes/java/main"),
        )
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
        val lexerRegen = layout.buildDirectory.dir("verifyGeneratedSourcesMatch/tmp-gen-lexer")
        inputs.dir(committedGen)
        inputs.dir(tmpRegen)
        inputs.dir(lexerRegen)

        doLast {
            val committed = committedGen.asFile
            val tmpDir = tmpRegen.get().asFile
            val lexerDir = lexerRegen.get().asFile
            val differences = mutableListOf<String>()

            fun File.generatedFilePaths(): Set<String> =
                walkTopDown()
                    .filter { it.isFile && (it.extension == "java" || it.extension == "flex") }
                    .map { it.relativeTo(this).path }
                    .toSet()

            val committedFiles = committed.generatedFilePaths()
            val regeneratedFiles = tmpDir.generatedFilePaths() + lexerDir.generatedFilePaths()

            committedFiles.forEach { rel ->
                // generateLexer and generateParser write to separate tmp dirs (each purges its
                // own root), so resolve each committed file against whichever regen dir holds it.
                val regenerated = tmpDir.resolve(rel).takeIf { it.exists() } ?: lexerDir.resolve(rel)
                val committedFile = committed.resolve(rel)
                when {
                    !regenerated.exists() -> differences += "MISSING in regen: $rel"
                    regenerated.readText() != committedFile.readText() -> differences += "DIFFERS: $rel"
                }
            }

            (regeneratedFiles - committedFiles).forEach { rel ->
                differences += "MISSING in committed src/main/gen: $rel"
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
            verifyServiceDependencyGraph,
            // Phase 7 CLEANUP-04 / D-07: detekt + ktlint JOIN the check-safe verify gates in
            // `check` (they do NOT replace them — the verify* tasks inspect the runtime classpath
            // that detekt's source-only mode cannot see). Both are plugin-provided tasks, wired by
            // string name. detekt runs source-only against detekt-baseline.xml (grandfathered
            // findings); ktlintCheck runs the standard ruleset. The frozen generated surface is
            // excluded via the Detekt-typed configureEach exclude("**/gen/**") above.
            "detekt",
            "ktlintCheck",
            // Phase 8 (plan 08-04): verifyGeneratedSourcesMatch is now LIVE in `check`, closing the
            // long-deferred SERVICE-10 drift baseline. The committed src/main/gen was regenerated on
            // the bundled toolchain (JFlex 1.9.2 + Grammar-Kit 2023.3) under 08-04 — the dedicated
            // parser-regression pass the Wave-1 deferral demanded — so the gate now diffs committed
            // gen against a fresh bundled-toolchain regen and passes cleanly (no drift). generateLexer
            // and generateParser write to separate tmp dirs (each purges its own root) and the gate
            // resolves every committed file against both. Any future BNF/flex edit that is not
            // accompanied by a matching gen regen now fails `check`.
            verifyGeneratedSourcesMatch,
        )
    }
}
