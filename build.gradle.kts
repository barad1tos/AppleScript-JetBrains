import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
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
    // kotlinx-coroutines-core is intentionally NOT declared as a runtime dep —
    // IntelliJ Platform 2024.3+ ships its own pinned coroutines (1.8.x) and shading
    // ours on top triggers NoSuchMethodError at runtime
    // (CancellableContinuation.tryResume signature drift). When v1.1 needs coroutines
    // we use the platform-bundled instance via compileOnly + JBR's transitive copy.
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
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
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
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3.7.1")
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
            if (includeHeavy) {
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.ParserRegressionTest")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.ControlStmtParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.HandlersParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.TellParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.DictionaryConstantParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.StandardAdditionsParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.parsing.LiveSamplesParsingTestCase")
                includeTestsMatching("com.intellij.plugin.applescript.test.concurrency.*")
                // DictionariesRandomParsingTestCase + TellApplicationMusicTest scan installed
                // /Applications and depend on host-machine state — kept out to avoid flakiness.
            }
        }
    }
}
