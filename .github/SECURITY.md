# Security Policy

## Scope

AppleScript Toolkit is a JetBrains IDE language plugin. Its relevant security surface includes:

- AppleScript run configurations that execute scripts selected by the user.
- SDEF/XML dictionary parsing and local dictionary cache handling.
- IntelliJ Platform plugin descriptor and extension wiring.
- Gradle dependencies and plugin distribution integrity.

The plugin should not execute scripts automatically without user action, claim official affiliation, or process private data beyond normal IDE/plugin operation.

## Supported Versions

| Version | Supported |
|---------|-----------|
| 2.x.x   | Yes       |
| < 2.0   | No        |

Only the latest major release line receives security fixes.

## Reporting a Vulnerability

Preferred: use GitHub Private Vulnerability Reporting from this repository's Security tab:

https://github.com/barad1tos/AppleScript-JetBrains/security/advisories/new

Please include:

- Description of the vulnerability.
- Steps to reproduce.
- Affected versions.
- Potential impact.
- Any relevant sample `.applescript`, `.sdef`, or `.xml` file.

## What to Expect

- Acknowledgment within 7 days.
- Assessment and response within 30 days.
- For confirmed issues: a fix in the next practical release.
- Credit in the changelog unless you prefer anonymity.

## What Qualifies

- Script execution behavior beyond explicit user intent.
- Unsafe SDEF/XML parsing behavior, including entity expansion or external-resource loading.
- Dependency vulnerabilities in the Gradle build or runtime dependency chain.
- Plugin signing, packaging, or distribution-integrity issues.

## What Does Not Qualify

- A script doing what the user explicitly chose to run.
- Parser false positives, missing completion, or ordinary IDE UI bugs.
- JetBrains IDE vulnerabilities; report those to JetBrains.
- Theoretical attacks requiring physical access to the developer's machine.

## Bug Bounty

This is a solo-maintained project. There is no monetary bug bounty, but confirmed reporters can be credited in release notes.
