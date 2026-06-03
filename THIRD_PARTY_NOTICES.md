# Third-Party Notices

This project is a maintained fork and Kotlin rewrite of the original AppleScript language support plugin for JetBrains IDEs.

## Original AppleScript Plugin

- Original project: https://github.com/ant-druha/AppleScript-IDEA
- Original author: Andrey Dernov
- Original license: Apache License 2.0
- Existing copyright notice preserved in `LICENSE`: Copyright 2017 Andrey Dernov

The current repository preserves the Apache License 2.0 terms and attribution for the original work.

## Major Changes in This Fork

- Rewritten from the original Java implementation to Kotlin for hand-written plugin code.
- Migrated to the modern IntelliJ Platform Gradle Plugin 2.x toolchain.
- Updated compatibility for current IntelliJ Platform releases. The current line targets build `251` and newer.
- Hardened SDEF parsing, dictionary loading, and background indexing behavior for current macOS and IDE runtimes.
- Added parser, resolver, smoke, static-analysis, and plugin-verifier coverage for the maintained codebase.

## Trademark Notice

Apple, AppleScript, macOS, JetBrains, IntelliJ IDEA, and related names or marks are trademarks of their respective owners. This project is not affiliated with, endorsed by, or sponsored by Apple, JetBrains, or the original maintainer.
