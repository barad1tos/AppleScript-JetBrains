# Differential corpus harness

A reliable way to measure the AppleScript parser against real-world scripts — instead of pasting
scripts into the IDE and eyeballing errors.

## Why

AppleScript is dictionary-driven: the same tokens parse differently depending on which application
and library dictionaries are in scope. A script that uses `Typinator` or a `use script` library
cannot be validated without those dictionaries — **even Apple's own `osacompile` fails on it**. So
"paste a random script and look for red squiggles" conflates two very different things:

- genuine plugin parser bugs, and
- constructs that are simply unresolvable because their dictionary isn't loaded.

This harness separates them using `osacompile` as a ground-truth oracle.

## How it works

```
for each script in the corpus:
  osacompile accepts it here? ──┬── NO  → NEEDS_DEPS  → not a plugin bug (missing dictionary)
                               └── YES → plugin reports errors? ──┬── YES → FALSE POSITIVE (real bug)
                                                                  └── NO  → clean ✓
```

The output is a **coverage number** ("N% of ground-truth-valid scripts parse without false
positives") and the false positives **categorized by normalized error signature**, so the parser is
hardened class-by-class rather than one pasted script at a time.

## Run it (macOS)

```bash
scripts/corpus/scan.sh                       # scans the standard macOS script folders
scripts/corpus/scan.sh /path/to/your/scripts # or point it at your own corpus
```

This classifies the corpus, runs the plugin parser over the `VALID_HERE` subset, and prints the
report (also written to `$CORPUS_OUT/differential-report.txt`, default `/tmp/applescript-corpus/`).

### Pieces

- `classify.sh` — decompiles (`osadecompile`) + classifies (`osacompile`) into
  `VALID_HERE` / `NEEDS_DEPS` / `SYNTAX_ERR` / `DECOMP_FAIL`.
- `CorpusDifferentialTest` (`src/test/.../parsing/CorpusDifferentialTest.kt`) — parses the
  `APPLESCRIPT_CORPUS_DIR` sources, counts clean files, and categorizes false positives. Skips
  silently when the env var is unset, so it is a no-op in the normal suite.

## Scope note

The corpus itself (system / third-party scripts) is **not committed** — it is regenerated locally
from the machine. Only the tooling lives in the repo. The committed regression corpus is the curated
set under `src/test/resources/testData/parse/realWorld/` (`RealWorldCorpusTest`).
