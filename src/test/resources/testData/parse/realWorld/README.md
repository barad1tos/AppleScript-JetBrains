# Real-world AppleScript regression corpus

This directory holds the Phase 8 (v2.0) real-world parser-hardening corpus. Each
`.applescript` fixture is a realistic script that exercises a grammar construct the
v1.0 grammar handled poorly (whose-filters, `library playlist N`, `ASCII character N`,
non-ASCII operators `≥ ≤ ≠ ÷`, nested `tell APP … end tell`, `on error … end try`).

Each `.applescript` file pairs with one `RealWorldCorpusTest.testXxx()` method that
asserts the parsed PSI tree contains **zero `PsiErrorElement` nodes**. The corpus is
the contract: every downstream grammar change is gated by a fixture's error count
dropping to zero.

## Sanitization Recipe

The committed fixtures are **neutral, hand-authored** scripts — they contain no
personal data. When adding a fixture derived from a personal/user-supplied script,
sanitize it BEFORE committing, by hand (visual diff — never an automated `sed`
pipeline, which misses context-sensitive identifiers):

| Replace | With |
|---------|------|
| Absolute POSIX paths (`/Users/<name>/…`) | `~/Music/…` or an app-agnostic equivalent |
| Bundle identifiers (`com.acme.RealApp`) | `com.example.app` |
| Opaque hash / persistent IDs | `PLACEHOLDER_ID_123` |
| Email addresses | `user@example.com` |
| Account / server / host names | generic placeholders (`"Work"`, `example.com`) |

### Preserve (do NOT alter)

The whole point of a fixture is the grammar shape, so preserve:

- Every construct that triggers a parse error: `every … whose …`, `library playlist N`,
  `current track`, `tell APP "name" … end tell`, `path to <constant>`,
  `ASCII character N`, `ASCII number`, `current date`, `try … on error … end try`.
- Variable names, handler names, and the leading `(* … *)` block comment.
- Non-ASCII operators (`≥ ≤ ≠ ÷`) exactly as written.

After sanitizing, re-check that the script still exercises the construct it was added
for. If the sanitized shape no longer matches, redo the sanitization preserving it.

## Regenerating snapshots

`RealWorldCorpusTest` is error-count-only (it does not write `.txt` PSI snapshots), so
there is normally nothing to regenerate. If a future fixture is migrated to a
`ParsingTestCase`/`.txt`-snapshot harness, regenerate baselines with the IntelliJ
system property (not the legacy regenerate-script env var):

```bash
./gradlew test --tests "*RealWorldCorpusTest" -Didea.tests.overwrite.data=true
```

## Source Provenance

All fixtures below are hand-authored for this repository (original work, no external
source), neutral by construction. The canonical 333-LOC motivation script
(`fetch_tracks.applescript`) is intentionally **not** committed — it is a personal
verification script kept local under `.planning/` and used only for the local
manual-smoke error-count metric (see `08-07`).

| File | Origin | License | Sanitized? |
|------|--------|---------|------------|
| `music_library.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `mail_archive.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `finder_select.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `system_events_processes.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `calendar_events.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `safari_tabs.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `standard_suite_text.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `standard_additions_paths.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `nested_tell_run.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `try_on_error.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `non_ascii_math.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |
| `shortcuts_invoke.applescript` | hand-authored | repo (Apache-2.0) | n/a (no PII) |

## Coverage matrix

| Construct | Fixture(s) |
|-----------|------------|
| whose-filter (+ compound boolean) | `music_library`, `mail_archive`, `finder_select`, `system_events_processes`, `calendar_events`, `safari_tabs`, `nested_tell_run`, `shortcuts_invoke` |
| `library playlist N` / `current track` | `music_library`, `nested_tell_run` |
| non-ASCII operators `≥ ≤ ≠ ÷` | `non_ascii_math`, `music_library`, `mail_archive`, `calendar_events` |
| `ASCII character N` / `ASCII number` | `music_library`, `standard_suite_text` |
| `path to <constant>` / `current date` | `standard_additions_paths`, `standard_suite_text`, `calendar_events` |
| nested `tell APP … end tell` in `on run argv` | `nested_tell_run` |
| `try … on error errMsg number N … end try` | `try_on_error`, `standard_additions_paths` |
