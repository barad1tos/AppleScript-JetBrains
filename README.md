<p align="center">
  <img src="assets/logo.svg" alt="AppleScript Toolkit" width="160">
</p>

<p align="center">
  <strong>AppleScript support for modern JetBrains IDEs.</strong>
</p>

<p align="center">
  <a href="https://github.com/barad1tos/AppleScript-JetBrains/releases/latest">
    <img src="https://img.shields.io/badge/Download-latest-blue?style=for-the-badge&colorA=1F2430" alt="Download latest release">
  </a>
</p>

<p align="center">
  <a href="https://github.com/barad1tos/AppleScript-JetBrains/actions/workflows/ci.yml">
    <img src="https://github.com/barad1tos/AppleScript-JetBrains/actions/workflows/ci.yml/badge.svg" alt="CI">
  </a>
  <a href="https://codecov.io/gh/barad1tos/AppleScript-JetBrains">
    <img src="https://codecov.io/gh/barad1tos/AppleScript-JetBrains/graph/badge.svg" alt="Coverage">
  </a>
  <a href="gradle.properties">
    <img src="https://img.shields.io/badge/IntelliJ%20Platform-2025.1%20to%202026.1-7C4DFF?colorA=1F2430" alt="IntelliJ Platform 2025.1 to 2026.1">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-Apache--2.0-23C284?colorA=1F2430" alt="Apache 2.0 license">
  </a>
</p>

<br>

AppleScript Toolkit brings AppleScript editing, code insight, dictionary tooling, and macOS script execution back to current JetBrains IDEs. It is a maintained revival of the original Apache-2.0 AppleScript plugin, with the handwritten IntelliJ Platform implementation modernized in Kotlin and the existing Grammar-Kit parser core preserved and hardened.

## What sets it apart

- **Modern compatibility** — targets IntelliJ Platform 2025.1 through 2026.1 with current Gradle, Kotlin, CI, and Plugin Verifier coverage
- **AppleScript language model** — syntax highlighting, parsing, structure view, navigation, find usages, documentation, and rename support where the current PSI/resolver model supports them
- **Dictionary-aware editing** — loads scriptable-application dictionaries and `.sdef` / `.xml` dictionary files to drive completion and documentation
- **macOS runtime integration** — run configurations execute AppleScript through the system runtime on macOS
- **Maintained fork hygiene** — Apache-2.0 attribution is preserved, and Marketplace identity trade-offs are documented before publication

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

- Minimum: IntelliJ Platform `251` (JetBrains IDEs 2025.1)
- Maximum: IntelliJ Platform `261.*` (JetBrains IDEs 2026.1.x)
- JVM target: 17
- AppleScript execution and dictionary discovery require macOS

## Installation

### From JetBrains Marketplace

A paid Marketplace listing is planned under the `barad1tos software` vendor profile.
Until the listing is published, install a local build from disk.

### From disk

1. Download the `.zip` from [Releases](https://github.com/barad1tos/AppleScript-JetBrains/releases/latest)
2. **Settings** → **Plugins** → **⚙** → **Install Plugin from Disk...**
3. Select the downloaded `.zip` and restart the IDE

## Usage Notes

On macOS, the plugin can run scripts through the system AppleScript runtime and can discover dictionaries from installed scriptable applications. On Linux or Windows, script execution and automatic macOS application discovery are unavailable, but manually loaded `.sdef` or `.xml` dictionary files can still provide dictionary-aware editing support.

Application dictionary indexing runs in the background. Completion may become available before the full application catalog has finished indexing.

Dictionary-aware completion depends on available SDEF data. If an application is missing, not scriptable, or exposes unusual dictionary markup, completion and documentation can be incomplete until the dictionary is loaded or a parser issue is fixed.

## Building from source

```bash
./gradlew buildPlugin        # Build the distribution ZIP
./gradlew test               # Run the test suite
./gradlew verifyPlugin       # Verify against configured IDE targets
```

Output: `build/distributions/AppleScript-IDEA-<version>.zip`

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines and [CHANGELOG.md](CHANGELOG.md) for release history.

## Community

- Found a bug? [Open a bug report](https://github.com/barad1tos/AppleScript-JetBrains/issues/new?template=bug_report.yml)
- Have a feature idea? [Open a feature request](https://github.com/barad1tos/AppleScript-JetBrains/issues/new?template=feature_request.yml)
- Security issue? Use [GitHub Private Vulnerability Reporting](https://github.com/barad1tos/AppleScript-JetBrains/security/advisories/new)

## Relationship to the Original Project

This project is derived from the original AppleScript plugin by Andrey Dernov, distributed under the Apache License 2.0. The parser and lexer remain based on the existing Grammar-Kit/JFlex core. This maintained fork keeps the Apache-2.0 attribution while updating the implementation for current JetBrains IDEs.

The current maintained fork and modifications are Copyright 2025-2026 Roman Borodavkin and contributors.

This paid Marketplace line uses a new plugin id, `software.barad1tos.applescript.toolkit`, under the `barad1tos software` vendor profile. Existing users of the original listing will need to install this paid listing separately.

## Credits

- Maintained fork, Kotlin modernization, and parser hardening by Roman Borodavkin and contributors
- Original AppleScript plugin by Andrey Dernov, distributed under the Apache License 2.0
- IntelliJ Platform SDK by JetBrains

## License

[Apache 2.0](LICENSE) — see `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md`.
