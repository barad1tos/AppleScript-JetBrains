# Changelog

All notable changes to AppleScript-IDEA will be documented in this file.

## [2.0.9] - 2026-06-21

- [Fix] Dynamic `tell application` blocks now parse correctly when the target application name comes from a variable or expression instead of a string literal.
- [Fix] Real-world Standard Additions commands such as `choose file ... of type`, `choose from list`, `open for access`, `write ... starting at`, and `close access` no longer produce false parser errors.
- [Fix] UI-scripting object references such as `tab group`, `process`, `window`, `group`, and `slider` keep parsing cleanly inside System Events automation scripts.
- [Fix] Dictionary-backed command parameters now preserve selector and class highlighting while accepting valid AppleScript command tails.
- [Fix] Valid object-reference command arguments such as `close every window` and anchored property phrases such as `require password to wake of security preferences` no longer show false parser errors.
- [Fix] Launcher-style scripts with a final top-level `end` after conditional branches now parse without a spurious error on the last line.
- [Fix] Semantic highlighting now better matches Script Editor-style AppleScript coloring for variables, dictionary terms, application references, handler calls, constants, and System Events process references.
- [Fix] Literal System Events process names that are not known local applications now get a weak warning instead of being confused with parser errors.
- [Fix] Application gutter markers now resolve real macOS app icons more reliably, including cached dictionaries and app bundles with icons stored in nested resources or extensions.
- [Fix] Malformed generated dictionary cache files no longer evict persisted dictionary information during gutter icon refresh.

## [2.0.8] - 2026-06-20

- [Fix] Valid AppleScript property-update statements no longer show false parser errors when assigning values to application object properties.
- [Fix] Dictionary commands with bracketed argument values now keep parsing following selector parameters instead of marking correct command calls as incomplete.
- [Fix] Command handlers inside `using terms from application` blocks now parse cleanly, preserving navigation and Find Usages behavior for handler names.

## [2.0.7] - 2026-06-09

### Fixed

- Fixed Structure View to show AppleScript handlers, properties, and script objects at the root level instead of showing an empty tree.
- Restored dictionary quick documentation when hovering over dictionary terms such as `track` inside `tell application "Music"`.
- Local variable references inside `tell…whose` filters now resolve for Find Usages and quick documentation.
- Added regression coverage for documentation, Find Usages, and Structure View.

## [2.0.6] - 2026-06-08

### Fixed

- Replaced deprecated IntelliJ Platform APIs so AppleScript Toolkit verifies cleanly on the supported IDE versions.
- Improved Find Usages accuracy for AppleScript handler calls and dictionary commands.

### Compatibility

- Verified against IntelliJ IDEA 2025.1, 2025.2, and 2026.1 without deprecated, internal, or experimental API warnings.

## [2.0.5] - 2026-06-08

### Fixed

- Fixed parser freezes and blocking dictionary lookups when AppleScript dictionaries are still loading.
- Improved parsing for Standard Additions command parameters such as `display notification`, `write ... starting at eof`, and `choose from list`.
- Fixed Typinator-style dictionary class references such as `rule set` and `containing set`.

### Tests

- Added regression coverage for real-world AppleScript parser edge cases.

## [2.0.3] - 2026-06-07

### Fixed

- Improved parsing for real-world AppleScript command phrases that use dictionary-style labeled arguments.
- Accepted additional folder-action and system handler signatures without surfacing spurious parse errors.
- Preserved parser diagnostics for malformed bracketed command arguments instead of swallowing following statements.

## [2.0.2] - 2026-06-05

### Fixed

- Fixed Marketplace release notes so the listing shows only the current release's `What's New` content.

## [2.0.1] - 2026-06-05

### Fixed

- Capped published Marketplace compatibility to IntelliJ Platform 2025.1 and 2025.2 so Plugin Verifier does not treat unsupported 2025.3+ IDE lines as part of this release.

### Compatibility

- Verified against IntelliJ IDEA Community 2025.1 and 2025.2 (`sinceBuild = 251`, `untilBuild = 252.*`).

## [2.0.0] - 2026-06-05

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
