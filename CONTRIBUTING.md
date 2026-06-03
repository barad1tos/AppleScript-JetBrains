# Contributing to AppleScript Support

Thanks for your interest in contributing. This project maintains AppleScript language support for modern JetBrains IDEs, so useful contributions usually fall into parser coverage, dictionary handling, code insight, run configurations, tests, or Marketplace/release documentation.

By participating, you agree to follow the [Code of Conduct](.github/CODE_OF_CONDUCT.md).

## Getting Started

1. Fork the repository and clone it locally.
2. Install **JDK 17**.
3. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```
4. Run tests:
   ```bash
   ./gradlew test --stacktrace
   ```
5. Verify plugin compatibility when touching runtime, descriptor, or dependency code:
   ```bash
   ./gradlew verifyPlugin --stacktrace
   ```

## Project Structure

| Path | What it does |
|------|--------------|
| `src/main/resources/AppleScript.bnf` | Grammar-Kit grammar source |
| `src/main/resources/_AppleScriptLexer.flex` | JFlex lexer source |
| `src/main/gen/` | Generated parser and PSI classes; do not edit directly |
| `src/main/kotlin/com/intellij/plugin/applescript/` | Hand-written Kotlin plugin code |
| `src/main/resources/META-INF/plugin.xml` | Plugin descriptor, extension registrations, and Marketplace description |
| `src/test/kotlin/` | Parser, service, PSI, smoke, and regression tests |
| `build.gradle.kts` | Gradle build, verification, ktlint, detekt, and plugin-verifier configuration |

## Generated Code

Do not edit files in `src/main/gen/` directly. Change the grammar or lexer source, regenerate, and verify drift:

```bash
./gradlew generateLexer generateParser
./gradlew verifyGeneratedSourcesMatch
```

Grammar changes are high-impact because they affect PSI, completion, references, inspections, and generated Java APIs. Include parser fixtures or regression tests for every grammar behavior change.

## Dictionary and macOS Behavior

Application dictionary discovery and AppleScript execution are macOS-specific. Guard tests and behavior accordingly. Loading `.sdef` or `.xml` dictionary files should remain useful on non-macOS platforms where possible.

## Submitting Changes

1. Create a focused branch, for example `fix/parser-whose-clause` or `docs/marketplace-readiness`.
2. Keep one PR to one logical change.
3. Use Conventional Commits-style messages (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
4. Include tests for parser, resolver, dictionary, or runtime behavior changes.
5. Include screenshots only when a user-visible IDE UI changes.

## Good PR Checklist

- `./gradlew build --stacktrace` passes.
- `./gradlew test --stacktrace` passes or the exact environment limitation is documented.
- `./gradlew verifyPlugin --stacktrace` passes for descriptor/runtime/dependency changes.
- No direct edits to generated `src/main/gen/` without the corresponding grammar or lexer source change.
- Marketplace-facing docs avoid unsupported affiliation, pricing, compatibility, or feature claims.

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
