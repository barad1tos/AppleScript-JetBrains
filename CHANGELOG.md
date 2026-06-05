# Changelog

All notable changes to AppleScript-IDEA will be documented in this file.

## [2.0.1] - 2026-06-05

### Fixed

- Capped published Marketplace compatibility to IntelliJ Platform 2025.1 and 2025.2 so Plugin Verifier does not treat unsupported 2025.3+ IDE lines as part of this release.

### Compatibility

- Verified against IntelliJ IDEA Community 2025.1 and 2025.2 (`sinceBuild = 251`, `untilBuild = 252.*`).

## [2.0.0] - 2026-06-03

### Added

- Standard Additions object tokens — `ASCII character N`, `ASCII number C`, `current date`, and `path to <folder>` — are now understood as valid expressions instead of being flagged as errors.
- Application-specific object references such as `library playlist N` and `current track` are recognised for any scriptable application, even before its dictionary has loaded.
- Non-ASCII comparison and math operators (≥, ≤, ≠, ÷) are recognised in expressions.

### Fixed

- `whose` filter clauses, including compound boolean conditions with parentheses, are no longer reported as incomplete.
- `tell application "Name" … end tell` blocks nested inside script handlers no longer produce spurious errors.
- `try … on error errMsg number errNum … end try` handlers now parse cleanly.

### Compatibility

- Verified against IntelliJ IDEA Community 2025.1 and 2025.2. Published compatibility is capped at 2025.2 (`sinceBuild = 251`, `untilBuild = 252.*`); earlier 2024.x releases are not supported.
- Prepared as a separate paid Marketplace listing under the new plugin id `software.barad1tos.applescript.toolkit`.

## [1.6.0] - 2026-06-03

### Changed

- Final maintenance release of the internal-modernization series. No user-facing changes to completion, navigation, documentation, or run-configuration behavior.

### Compatibility

- Minimum supported IDE remains 2025.1 (`sinceBuild = 251`); earlier 2024.x releases are not supported. (Unchanged from 1.2.)

## [1.5.0] - 2026-06-03

### Changed

- Internal maintenance and stability improvements. This is a maintenance release with no user-facing changes to completion, navigation, documentation, or run-configuration behavior.

### Compatibility

- Minimum supported IDE remains 2025.1 (`sinceBuild = 251`); earlier 2024.x releases are not supported. (Unchanged from 1.2.)

## [1.4.0] - 2026-06-03

### Changed

- Internal modernization of the application-dictionary model. This is a maintenance release with no change to completion, navigation, documentation, or run-configuration behavior.

### Compatibility

- Verified against IntelliJ IDEA Community 2025.1 and 2025.2. Minimum supported IDE remains 2025.1 (`sinceBuild = 251`); earlier 2024.x releases are not supported. (Unchanged from 1.2.)

## [1.3.0] - 2026-06-03

### Changed

- Internal code organization improved to make room for upcoming features. No user-visible changes in this release.

### Compatibility

- Minimum supported IDE remains 2025.1 (`sinceBuild = 251`); earlier 2024.x releases are not supported. (Unchanged from 1.2 — re-stated for clarity.)

## [1.2.0] - 2026-06-03

### Changed

- Dictionary loading no longer blocks IDE startup. AppleScript completion becomes available within seconds, while the full application catalog finishes indexing quietly in the background.
- When dictionary indexing takes more than a couple of seconds, a cancellable progress indicator appears in the status bar so you can see what the plugin is doing — and stop it if you don't need application-aware completion in the current session.

### Compatibility

- Minimum supported IDE raised to 2025.1 (`sinceBuild = 251`). 2024.3 users should stay on the 1.1.x line; the IDE shipped with that release does not include the runtime support needed for the new background dictionary loading.

## [1.1.0] - 2026-06-03

### Fixed

- Completion on overloaded AppleScript commands (same name across different suites) now lists all overloads instead of dropping to a single arbitrary one. Cmd+Click still navigates to a stable first-inserted overload, so existing workflows are unaffected.
- Eliminated a long-standing race condition in dictionary `xi:include` processing that could surface as sporadic `NullPointerException`s or, more rarely, a `HashMap` resize stuck-spin when several scripts opened at once during plugin warm-up.

### Removed

- The hidden "Generate Script Object" action stub was deleted. It was never registered in the menu and never had a UX attached — no user-visible change.

## [1.0.1] - 2026-06-03

### Fixed

- Resolved a data race in the dictionary registry that could cause sporadic `NullPointerException`s or brief IDE hangs when an AppleScript file was opened or completion was triggered while the plugin was still warming up. Affected users typically saw the issue right after IDE startup or after a project switch.

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

### Next

- The first compatibility release intentionally mirrored the original Java API shape to keep the rewrite reviewable. Later maintenance releases continue API ergonomics, dictionary loading, and parser hardening work while preserving existing user workflows.
