# Changelog

All notable changes to AppleScript-IDEA will be documented in this file.

## [1.0.0] - 2026-05-22

The plugin is now compatible with modern JetBrains IDEs (2024.3+) and is fully rewritten in Kotlin.

### Compatibility

- Targets IntelliJ Platform 2024.3 and newer (`sinceBuild = 243`, `untilBuild` open).
- Verified against IntelliJ IDEA Community 2024.3, 2025.1, and 2025.2.
- Plugin id stays `com.intellij.plugin.applescript`, so existing 0.130 users get an in-IDE upgrade prompt.

### Fixed

- Plugin loads on current JetBrains IDEs (the 0.130 series did not).
- SDEF dictionary parsing for application bundles is more robust on macOS Ventura and newer (XXE-hardened SAXBuilder configuration while still honouring Apple's `sdef.dtd` DOCTYPE).
- Resolver no longer trips a known nullability mismatch around the dictionary root tag during the first SDEF parse.
- Scripts targeting an application whose dictionary isn't loaded (missing `.app`, fresh project, stale snapshot) no longer cascade into spurious `end`/`on error`/`tell` parser errors. The parser now accepts two-word composite identifiers like `album artist of x` or `library playlist 1 whose id is N` as bareword references, leaving unresolved-name reporting to the annotator. Single-token chains (`count of items`) keep their previous behaviour.

### Changed

- Entire plugin source migrated from Java to Kotlin (one hand-written Grammar-Kit helper kept in Java by design).
- Built with the modern IntelliJ Platform Gradle Plugin 2.x toolchain on Gradle 9.5.
- Kotlin 2.3 / JVM target 17.

### Known issues

- Idiomatic Kotlin API surface (property syntax across hand-written PSI/SDEF interfaces, sealed hierarchies, structured concurrency for SDEF loading) is planned for the 1.1 milestone — for 1.0 the API shape mirrors the original Java 1:1 to keep the rewrite reviewable.
