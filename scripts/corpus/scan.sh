#!/bin/bash
# One-shot differential parser-robustness scan (macOS).
#
# 1. Classifies a corpus of real AppleScripts with the ground-truth `osacompile` (classify.sh).
# 2. Runs the plugin parser (CorpusDifferentialTest) over the VALID_HERE subset.
# 3. Prints the false-positive coverage report (clean %, categorized error signatures).
#
# Usage:  scripts/corpus/scan.sh [<dir> ...]
# With no args, scans the standard macOS script folders.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
OUT="${CORPUS_OUT:-/tmp/applescript-corpus}"
VALID_DIR="$OUT/valid"
export CORPUS_OUT="$OUT"

DIRS=("$@")
if [ "${#DIRS[@]}" -eq 0 ]; then
  DIRS=("/Library/Scripts" "$HOME/Library/Scripts" "/System/Library/Automator" "/System/Library/ScriptingAdditions")
fi

bash "$HERE/classify.sh" "${DIRS[@]}" || exit $?

echo ""
echo "=== running plugin parser over VALID_HERE corpus (boots a test fixture) ==="
if ! (
  cd "$ROOT" && APPLESCRIPT_CORPUS_DIR="$VALID_DIR" ./gradlew test \
    --tests "com.intellij.plugin.applescript.test.parsing.CorpusDifferentialTest" \
    --rerun-tasks --console=plain > "$OUT/gradle.log" 2>&1
); then
  echo "parser scan failed; see $OUT/gradle.log" >&2
  tail -n 80 "$OUT/gradle.log" >&2 || true
  exit 1
fi
echo ""
if [ -f "$OUT/differential-report.txt" ]; then
  cat "$OUT/differential-report.txt"
else
  echo "parser scan completed without differential report; see $OUT/gradle.log" >&2
  tail -n 80 "$OUT/gradle.log" >&2 || true
  exit 1
fi
