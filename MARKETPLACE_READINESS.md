# Marketplace Readiness

**Status:** Draft
**Last reviewed:** 2026-06-04

This document tracks repository and plugin metadata readiness for a JetBrains Marketplace publication. It does not authorize publishing, tagging, or changing the Marketplace listing.

## Current State

Current version and compatibility values are sourced from `gradle.properties`;
Plugin Verifier target IDEs are sourced from `build.gradle.kts`. Keep this
section, `README.md`, and `CHANGELOG.md` synchronized whenever those Gradle
values change.

- Plugin version: `2.0.0`
- Plugin id: `com.intellij.plugin.applescript`
- Plugin name: `Applescript Toolkit`
- Minimum supported build: `251` (`pluginSinceBuild=251`)
- Current verifier targets: IntelliJ IDEA Community 2025.1 and 2025.2
- License: Apache License 2.0
- Vendor metadata: `Roman Borodavkin`

## Checklist

- [x] README rewritten for a maintained Marketplace/product presentation.
- [x] Old upstream screenshots and issue/download links removed from README.
- [x] Neutral plugin icon replaces the previous Apple-like silhouette.
- [x] Neutral file type icon replaces the previous Apple-like file icon.
- [x] Legacy upstream screenshots, bundled language PDFs, old PsiViewer JAR, and unused SDEF media assets removed.
- [x] `plugin.xml` vendor and description updated for Marketplace display.
- [x] `NOTICE` added with current maintainer and original-project attribution.
- [x] `THIRD_PARTY_NOTICES.md` added with original-project attribution.
- [x] Apache 2.0 license retained.
- [x] CI, CodeQL, Dependabot, pre-commit, zizmor, ktlint, detekt, and plugin-verifier wiring are present.
- [x] GitHub community files added: contributing guide, code of conduct, security policy, issue templates, PR template, and CODEOWNERS.
- [x] GitHub Issues and Private Vulnerability Reporting enabled for the repository.
- [x] Bundled runtime dependencies enumerated in `THIRD_PARTY_NOTICES.md`.
- [ ] Latest CI status confirmed on the publication branch.
- [x] Local validation commands rerun after this metadata pass.
- [ ] Bundled dependency license posture reviewed before paid Marketplace publication, especially multi-license dependencies such as JNA and delight-rhino-sandbox.
- [ ] Marketplace screenshots captured and uploaded through the Marketplace media section.
- [ ] Pricing, trial, refund/support, and paid-vs-free publication decisions made.
- [ ] Developer EULA and privacy-policy requirements reviewed for paid Marketplace publication.
- [ ] Plugin id and listing ownership decision made before publication.
- [ ] Marketplace listing resources configured in the Marketplace admin panel: source code, issue tracker, documentation, license, tags, and getting-started text.

## Plugin Identity Decision

The current plugin id is preserved:

```xml
<id>com.intellij.plugin.applescript</id>
```

This is intentional for now. Keeping the legacy id can preserve upgrade continuity if the existing Marketplace listing is controlled by the current maintainer or transferred before publication. In that path, users of the old listing can receive a normal in-IDE update instead of installing a separate plugin.

For a new paid fork listing, a new id and vendor namespace is probably cleaner, for example an id under the repository owner's namespace. That path avoids ambiguity around listing ownership, vendor identity, paid licensing, and support responsibility, but it breaks automatic upgrade continuity from the legacy listing.

Do not change the id silently. Before a paid Marketplace publication, choose one of these options explicitly:

- **Keep legacy id:** requires existing listing ownership/control or transfer, plus confirmation that Marketplace policy accepts the retained id/name for this publication model.
- **Use a new id:** requires a new listing and migration messaging; existing users must install the new plugin manually or through separate communication.

Until that decision is made, plugin id/listing ownership remains a publication blocker.

The plugin display name is standardized as `Applescript Toolkit` in both the source descriptor and Gradle-patched distribution metadata. This avoids the generic Marketplace template term "Support" and avoids JetBrains product-name terms such as "IDEA" while keeping the legacy plugin id unchanged for the listing-ownership decision above.

## Technical Positioning

This repository should be presented as a maintained modern fork, not as a from-scratch parser rewrite.

The handwritten IntelliJ Platform implementation has been substantially modernized, including Kotlin ports, current Gradle and IntelliJ Platform tooling, dictionary loading hardening, structured-concurrency cleanup, CI, static analysis, and Plugin Verifier coverage.

The AppleScript parser and lexer remain based on the existing Grammar-Kit/JFlex core from the original plugin line. This is intentional for the first Marketplace-ready line: AppleScript has many natural-language and dictionary-specific edge cases, and preserving the accumulated parser shape reduces the risk of breaking existing scripts while the maintained fork hardens behavior with focused regression tests and real-world corpus fixtures.

Future parser-core modernization is a roadmap item, not a blocker for the initial Marketplace release, as long as release validation continues to cover generated-source drift, parser regressions, real-world fixtures, and Plugin Verifier compatibility.

## Media and Listing Notes

JetBrains recommends using the Marketplace media section for screenshots instead of embedding screenshots directly in the plugin description. Screenshots should be captured from the real IDE with legible UI text and should show actual plugin workflows:

- Syntax highlighting and structure view.
- Dictionary-backed completion.
- Run configuration setup.
- Documentation/navigation for dictionary terms.

The README currently keeps screenshots as TODO items because no current Marketplace-quality screenshots are available in this repository.

## Validation Plan

Run these checks before publication:

```bash
./gradlew build --stacktrace
./gradlew test --stacktrace
./gradlew verifyPluginStructure --stacktrace
./gradlew verifyPlugin --stacktrace
```

Also review CI results for:

- `detekt`
- `ktlintCheck`
- `runIdeHeadlessSmoke`
- `verifyBundledCoroutinesVersions`
- `scripts/verify-plugin-verifier.py`

## Validation Results

Verified locally on 2026-06-03:

- `xmllint --noout src/main/resources/META-INF/plugin.xml` — passed.
- `xmllint --noout src/main/resources/META-INF/pluginIcon.svg` — passed.
- `xmllint --noout src/main/resources/META-INF/pluginIcon_dark.svg` — passed.
- `xmllint --noout src/main/resources/icons/applescript_file_icon.svg` — passed.
- `xmllint --noout src/main/resources/icons/applescript_file_icon_dark.svg` — passed.
- `xmllint --noout assets/logo.svg` — passed.
- `ruby -e 'require "yaml"; ARGV.each { |f| YAML.load_file(f) }' ...` for issue templates, Dependabot, and workflows — passed.
- `git diff --check` — passed.
- `./gradlew build --stacktrace` — passed.
- `./gradlew test --stacktrace` — passed; test task was up-to-date after the build run.
- `./gradlew verifyPluginStructure --stacktrace` — passed, but reported descriptor identity warnings for the legacy `com.intellij` id prefix and `intellij` in the id.
- `./gradlew verifyPlugin --stacktrace` — passed against IC 2025.1 and 2025.2; Plugin Verifier reported 12 deprecated API usages and one non-dynamic extension restriction.
- `python3 scripts/verify-plugin-verifier.py` — passed; internal API usages: 0, experimental usages: 0, deprecated usages: 12.

Observed non-blocking build warnings:

- Gradle reports deprecated Gradle features that will be incompatible with Gradle 10.
- Plugin Verifier warns about deprecated APIs in legacy JDOM, run configuration, live template, smoke starter, project base-dir, and code style integration paths.

## Publication Guardrails

- Do not publish to JetBrains Marketplace from this metadata-preparation pass.
- Do not push tags from this pass.
- Do not change runtime/plugin behavior unless a Marketplace metadata or packaging requirement makes it necessary.
- Document product/legal trade-offs before making identity, pricing, EULA, or affiliation decisions.
