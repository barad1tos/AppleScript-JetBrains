#!/bin/bash
# Differential AppleScript corpus classifier (macOS).
#
# For each .scpt / .applescript under the given directories: decompile to source (osadecompile),
# then classify with the ground-truth Apple compiler (osacompile):
#
#   VALID_HERE  — compiles cleanly on this machine (all app/library dictionaries resolvable).
#                 A FAIR test of the plugin parser: any plugin error here is a genuine false positive.
#   NEEDS_DEPS  — osacompile fails on a missing app/library dictionary (e.g. -1728 "Can't get …").
#                 NOT a plugin grammar bug — the script needs a dictionary that isn't installed.
#   SYNTAX_ERR  — osacompile reports a real syntax error (the script itself is malformed).
#   DECOMP_FAIL — could not decompile to source.
#
# Writes decompiled sources + a manifest.tsv to $CORPUS_OUT (default /tmp/applescript-corpus).
# Read-only with respect to the repository. Pair with CorpusDifferentialTest (see scripts/corpus/scan.sh).
#
# Usage:  scripts/corpus/classify.sh <dir> [<dir> ...]
# Example: scripts/corpus/classify.sh /Library/Scripts "$HOME/Library/Scripts"
set -u

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <dir> [<dir> ...]" >&2
  exit 2
fi
if ! command -v osacompile >/dev/null 2>&1; then
  echo "error: osacompile not found — this harness requires macOS." >&2
  exit 3
fi

OUT="${CORPUS_OUT:-/tmp/applescript-corpus}"
rm -rf "$OUT"; mkdir -p "$OUT/src"
MANIFEST="$OUT/manifest.tsv"
: > "$MANIFEST"
total=0; validhere=0; needsdeps=0; syntaxerr=0; decompfail=0

classify_one() {
  local f="$1"
  total=$((total + 1))
  local key tmpsrc err
  key="$(printf '%s' "$f" | md5 -q)"
  tmpsrc="$OUT/src/${key}.applescript"
  case "$f" in
    *.applescript | *.txt) cp "$f" "$tmpsrc" 2>/dev/null ;;
    *) osadecompile "$f" > "$tmpsrc" 2>/dev/null ;;
  esac
  if [ ! -s "$tmpsrc" ]; then
    printf 'DECOMP_FAIL\t%s\t%s\n' "$tmpsrc" "$f" >> "$MANIFEST"; decompfail=$((decompfail + 1)); return
  fi
  # Normalize osadecompile's UTF-16 output to UTF-8 so the plugin parser (UTF-8) reads it correctly;
  # otherwise the BOM/wide chars surface as a spurious line-1 error, not a real parser bug.
  if file "$tmpsrc" | grep -q "UTF-16"; then
    iconv -f UTF-16 -t UTF-8 "$tmpsrc" > "${tmpsrc}.u8" 2>/dev/null && mv "${tmpsrc}.u8" "$tmpsrc"
  fi
  err="$(osacompile -o /dev/null "$tmpsrc" 2>&1)"
  if [ -z "$err" ]; then
    printf 'VALID_HERE\t%s\t%s\n' "$tmpsrc" "$f" >> "$MANIFEST"; validhere=$((validhere + 1))
  elif printf '%s' "$err" | grep -qiE "\-1728|-1708|can.t get|can.t find|isn.t running|doesn.t understand|where is|no user interaction"; then
    printf 'NEEDS_DEPS\t%s\t%s\t%s\n' "$tmpsrc" "$f" "$(printf '%s' "$err" | tr '\n' ' ')" >> "$MANIFEST"; needsdeps=$((needsdeps + 1))
  else
    printf 'SYNTAX_ERR\t%s\t%s\t%s\n' "$tmpsrc" "$f" "$(printf '%s' "$err" | tr '\n' ' ')" >> "$MANIFEST"; syntaxerr=$((syntaxerr + 1))
  fi
}

while IFS= read -r -d '' f; do classify_one "$f"; done < <(
  find "$@" \( -name '*.scpt' -o -name '*.applescript' \) -type f -print0 2>/dev/null
)

echo "=== corpus classification ==="
echo "total=$total  VALID_HERE=$validhere  NEEDS_DEPS=$needsdeps  SYNTAX_ERR=$syntaxerr  DECOMP_FAIL=$decompfail"
echo "manifest : $MANIFEST"
echo "sources  : $OUT/src  (point CorpusDifferentialTest at this via APPLESCRIPT_CORPUS_DIR)"
