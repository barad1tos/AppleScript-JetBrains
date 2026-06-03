# Attribution and Third-Party Notices

This project is a maintained fork and Kotlin rewrite of the original AppleScript language support plugin for JetBrains IDEs.

## Current Maintained Fork

- Current maintainer: Roman Borodavkin
- Current repository: https://github.com/barad1tos/AppleScript-JetBrains
- Current fork and modifications: Copyright 2025-2026 Roman Borodavkin and contributors
- Current fork license: Apache License 2.0, except for bundled third-party runtime dependencies listed below.

This current-fork notice covers this repository's maintained derivative work and modifications. It does not replace or narrow the original project attribution preserved below.

## Original AppleScript Plugin

- Original project: https://github.com/ant-druha/AppleScript-IDEA
- Original author: Andrey Dernov
- Original license: Apache License 2.0
- Existing copyright notice preserved in `LICENSE`: Copyright 2017 Andrey Dernov

The current repository preserves the Apache License 2.0 terms and attribution for the original work.

## Bundled Runtime Dependencies

The plugin distribution bundles the following third-party runtime libraries. The bundled JAR files are kept intact, including their embedded license and notice files where the upstream artifacts provide them.

| Artifact                                          | Version        | License                                  | Source                                                   |
|---------------------------------------------------|----------------|------------------------------------------|----------------------------------------------------------|
| `org.apache.commons:commons-imaging`              | `1.0.0-alpha6` | Apache License 2.0                       | https://commons.apache.org/proper/commons-imaging/       |
| `org.bidib.com.github.markusbernhardt:proxy-vole` | `1.1.6`        | Apache License 2.0                       | https://github.com/akuhtz/proxy-vole                     |
| `net.java.dev.jna:jna`                            | `5.15.0`       | LGPL 2.1 or later, or Apache License 2.0 | https://github.com/java-native-access/jna                |
| `net.java.dev.jna:jna-platform`                   | `5.15.0`       | LGPL 2.1 or later, or Apache License 2.0 | https://github.com/java-native-access/jna                |
| `org.apache.commons:commons-configuration2`       | `2.11.0`       | Apache License 2.0                       | https://commons.apache.org/proper/commons-configuration/ |
| `commons-io:commons-io`                           | `2.19.0`       | Apache License 2.0                       | https://commons.apache.org/proper/commons-io/            |
| `org.apache.commons:commons-lang3`                | `3.20.0`       | Apache License 2.0                       | https://commons.apache.org/proper/commons-lang/          |
| `commons-logging:commons-logging`                 | `1.3.2`        | Apache License 2.0                       | https://commons.apache.org/proper/commons-logging/       |
| `org.apache.commons:commons-text`                 | `1.12.0`       | Apache License 2.0                       | https://commons.apache.org/proper/commons-text/          |
| `org.javadelight:delight-rhino-sandbox`           | `0.0.17`       | Apache License 2.0, MIT License, or GPL  | https://github.com/javadelight/delight-rhino-sandbox     |
| `org.mozilla:rhino-runtime`                       | `1.7.15`       | Mozilla Public License 2.0               | https://mozilla.github.io/rhino/                         |
| `org.slf4j:slf4j-api`                             | `2.0.16`       | MIT License                              | https://www.slf4j.org                                    |

Compile-only IntelliJ Platform and Kotlin coroutine APIs are provided by the target IDE runtime and are not bundled in the plugin ZIP.

## Major Changes in This Fork

- Rewritten from the original Java implementation to Kotlin for handwritten plugin code.
- Migrated to the modern IntelliJ Platform Gradle Plugin 2.x toolchain.
- Updated compatibility for current IntelliJ Platform releases. The current line targets build `251` and newer.
- Hardened SDEF parsing, dictionary loading, and background indexing behavior for current macOS and IDE runtimes.
- Added parser, resolver, smoke, static-analysis, and plugin-verifier coverage for the maintained codebase.

## Trademark Notice

Apple, AppleScript, macOS, JetBrains, IntelliJ IDEA, and related names or marks are trademarks of their respective owners. This project is not affiliated with, endorsed by, or sponsored by Apple, JetBrains, or the original maintainer.
