<p align="center">
  <img src="assets/logo.svg" alt="AppleScript" width="160">
</p>

<p align="center">
  <strong>AppleScript support for modern JetBrains IDEs.</strong>
</p>

<p align="center">
  <a href="MARKETPLACE_READINESS.md">
    <img src="https://img.shields.io/badge/Marketplace-readiness%20in%20progress-6CA8FF?style=for-the-badge&colorA=202431" alt="Marketplace readiness in progress">
  </a>
</p>

<p align="center">
  <a href="https://github.com/barad1tos/AppleScript-JetBrains/actions/workflows/ci.yml">
    <img src="https://github.com/barad1tos/AppleScript-JetBrains/actions/workflows/ci.yml/badge.svg" alt="CI">
  </a>
  <a href="https://codecov.io/gh/barad1tos/AppleScript-JetBrains">
    <img src="https://codecov.io/gh/barad1tos/AppleScript-JetBrains/graph/badge.svg" alt="Coverage">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-Apache--2.0-23C284?colorA=202431" alt="Apache 2.0 license">
  </a>
  <a href="gradle.properties">
    <img src="https://img.shields.io/badge/IntelliJ%20Platform-2025.1%2B-7C4DFF?colorA=202431" alt="IntelliJ Platform 2025.1+">
  </a>
</p>

<br>

The AppleScript plugin brings AppleScript editing, code insight, dictionary tooling, and macOS script execution back to current JetBrains IDEs. It is a Kotlin rewrite and revival of the original Apache-2.0 AppleScript plugin, maintained by Roman Borodavkin and updated for the modern IntelliJ Platform.

The project is not affiliated with Apple, JetBrains, or the original maintainer.

## What sets it apart

- **Modern compatibility** — targets IntelliJ Platform 2025.1+ with current Gradle, Kotlin, CI, and Plugin Verifier coverage.
- **AppleScript language model** — syntax highlighting, parsing, structure view, navigation, find usages, documentation, and rename support where the current PSI/resolver model supports them.
- **Dictionary-aware editing** — loads scriptable-application dictionaries and `.sdef` / `.xml` dictionary files to drive completion and documentation.
- **macOS runtime integration** — run configurations execute AppleScript through the system runtime on macOS.
- **Maintained fork hygiene** — Apache-2.0 attribution is preserved, and the Marketplace identity trade-off is documented before publication.

## Language Support

| Area       | Support                                                                                                                                                              |
|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| File types | `.applescript`, `.scpt`                                                                                                                                              |
| Syntax     | Highlighting for AppleScript keywords, literals, operators, handlers, tell blocks, and dictionary terms where available                                              |
| Parser     | Common AppleScript statements and expressions, Standard Additions object tokens, object references, handlers, `tell`, `try`, `whose`, and application-specific terms |
| Completion | Keywords, command names, command parameters, application names, and dictionary-backed terms                                                                          |
| Navigation | Structure view, documentation lookup, references, find usages, and rename where supported                                                                            |
| Runtime    | Run configurations using the macOS AppleScript runtime                                                                                                               |
| Templates  | Live templates for common AppleScript constructs                                                                                                                     |

## Compatibility

The current plugin version is `2.0.0`.

Release coordinates are defined in [gradle.properties](gradle.properties);
Plugin Verifier target IDEs are configured in [build.gradle.kts](build.gradle.kts).
Update this section, [CHANGELOG.md](CHANGELOG.md), and
[MARKETPLACE_READINESS.md](MARKETPLACE_READINESS.md) in the same change when
those Gradle values move.

- Minimum supported IntelliJ Platform build: `251` (`pluginSinceBuild=251`), corresponding to JetBrains IDEs 2025.1 and newer.
- Current verifier targets: IntelliJ IDEA Community 2025.1 and 2025.2.
- JVM target: 17.
- AppleScript execution and automatic application dictionary discovery require macOS.

Earlier 2024.x IDE releases are not supported by the current line because the plugin relies on runtime support available in 2025.1+.

## Installation

### JetBrains Marketplace

A Marketplace listing is planned. Until the listing is published, install a local build from disk. Current Marketplace identity decisions are tracked in [MARKETPLACE_READINESS.md](MARKETPLACE_READINESS.md).

### Local build

```bash
./gradlew buildPlugin
```

Install the generated ZIP from `build/distributions/` via:

`Settings | Plugins | Gear icon | Install Plugin from Disk...`

Restart the IDE after installation.

## Usage Notes and Limitations

AppleScript files use `.applescript` or `.scpt` extensions.

On macOS, the plugin can run scripts through the system AppleScript runtime and can discover dictionaries from installed scriptable applications. On Linux or Windows, script execution and automatic macOS application discovery are unavailable, but manually loaded `.sdef` or `.xml` dictionary files can still provide dictionary-aware editing support.

Application dictionary indexing runs in the background. Completion may become available before the full application catalog has finished indexing.

AppleScript has a large natural-language grammar and many application-specific dialects. The parser is intended to cover common production scripts and the maintained regression corpus, but some unusual constructs or dictionary-specific phrases may still require parser or resolver fixes.

Dictionary-aware completion depends on available SDEF data. If an application is missing, not scriptable, or exposes unusual dictionary markup, completion and documentation can be incomplete until the dictionary is loaded or a parser issue is fixed.

## Screenshots

Marketplace screenshots are still needed.

Planned captures:

- Syntax highlighting and structure view.
- Dictionary-backed completion inside a `tell application` block.
- Run configuration for an AppleScript file.
- Documentation/navigation for dictionary terms.

## Development

### Building from source

```bash
./gradlew buildPlugin                 # Build the distribution ZIP
./gradlew test --stacktrace           # Run the configured test suite
./gradlew verifyPlugin --stacktrace   # Verify against configured IDE targets
```

Output: `build/distributions/AppleScript-IDEA-<version>.zip`

Full local validation used for Marketplace readiness:

```bash
./gradlew build --stacktrace
./gradlew test --stacktrace
./gradlew verifyPluginStructure --stacktrace
./gradlew verifyPlugin --stacktrace
```

The repository also contains CI, CodeQL, Dependabot, pre-commit, zizmor, ktlint, detekt, and plugin-verifier wiring. See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines and [CHANGELOG.md](CHANGELOG.md) for release history.

## Community

Found a bug? [Open a bug report](https://github.com/barad1tos/AppleScript-JetBrains/issues/new?template=bug_report.yml) with IDE version, plugin version, operating system, a minimal script sample, and logs if relevant.

Have a feature idea? [Open a feature request](https://github.com/barad1tos/AppleScript-JetBrains/issues/new?template=feature_request.yml) for parser, completion, dictionary, run configuration, template, or Marketplace-readiness improvements.

Security issue? Please use GitHub Private Vulnerability Reporting as described in [.github/SECURITY.md](.github/SECURITY.md).

## Relationship to the Original Project

This project is derived from the original AppleScript plugin for JetBrains IDEs by Andrey Dernov, distributed under the Apache License 2.0. The original plugin was free and targeted older IntelliJ Platform versions.

This maintained fork keeps the Apache-2.0 attribution while updating the implementation for current JetBrains IDEs, including a Kotlin rewrite, modern IntelliJ Platform Gradle Plugin migration, dictionary loading hardening, parser compatibility work, and current CI/plugin-verifier coverage.

The current maintained fork and modifications are Copyright 2025-2026 Roman Borodavkin and contributors. That notice covers this repository's maintained derivative work and does not replace the original author attribution.

The legacy plugin identity is currently preserved for upgrade-continuity analysis. A paid Marketplace publication may require a separate plugin id, display name, and vendor namespace if the existing listing cannot be controlled or transferred. See `MARKETPLACE_READINESS.md`.

## Links

- Repository: https://github.com/barad1tos/AppleScript-JetBrains
- Issues: https://github.com/barad1tos/AppleScript-JetBrains/issues
- Original Apache-2.0 project: https://github.com/ant-druha/AppleScript-IDEA

## Credits

- Maintained fork and Kotlin rewrite by Roman Borodavkin and contributors.
- Original AppleScript plugin by Andrey Dernov, distributed under the Apache License 2.0.
- IntelliJ Platform SDK by JetBrains.

## License

This project is distributed under the Apache License 2.0. See `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md`.
