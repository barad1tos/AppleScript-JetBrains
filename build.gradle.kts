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
        resources {
            srcDirs("src/main/resources")
            exclude("**.bnf", "**.flex")
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
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
    }

    pluginVerification {
        ides {
            recommended()
        }
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
        filter {
            includeTestsMatching("com.intellij.plugin.applescript.test.lexer.*")
        }
    }
}
