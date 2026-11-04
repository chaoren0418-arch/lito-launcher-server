#!/usr/bin/env bash
# Exercise the real cold-start CLI/build.sh with a PATH containing no Maven.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$ROOT/../../../.." && pwd)"
mkdir -p "$REPO/scratch"
WORK="$(mktemp -d "$REPO/scratch/semantic-diff-fallback.XXXXXX")"
TOOL="$WORK/tool with spaces"
mkdir -p "$WORK/bin" "$TOOL" "$WORK/left" "$WORK/right" "$WORK/target/left" "$WORK/target/right"
cp -R "$ROOT/src" "$TOOL/src"
cp "$ROOT/semantic-diff" "$ROOT/build.sh" "$ROOT/build-support.sh" "$TOOL/"
for command in bash dirname mkdir curl sha256sum find sort javac java; do
  ln -s "$(command -v "$command")" "$WORK/bin/$command"
done
if PATH="$WORK/bin" "$WORK/bin/bash" -c 'command -v mvn'; then
  echo "FAIL: Maven still visible in fallback PATH" >&2
  exit 1
fi
echo "PASS FALLBACK 1 Maven absent from isolated PATH"
echo 'public class Foo { public int value() { return 1; } }' > "$WORK/left/Foo.java"
echo 'public class Foo { public int value() { return 2; } }' > "$WORK/right/Foo.java"
for side in left right; do
  javac --release 17 -d "$WORK/target/$side" "$WORK/$side/Foo.java"
  jar cf "$WORK/target/$side.jar" -C "$WORK/target/$side" .
done
fallback() { PATH="$WORK/bin" "$WORK/bin/bash" "$TOOL/semantic-diff" "$@"; }
fallback --help > "$WORK/help.out" 2> "$WORK/help.err"
test -f "$TOOL/target/classes/local/lito381/semanticdiff/SemanticDiff.class"
grep -q 'usage: semantic-diff' "$WORK/help.err"
test ! -s "$WORK/help.out"
echo "PASS FALLBACK 2 cold CLI downloads/checks ASM and compiles with javac; --help exits 0"
fallback --left "$WORK/target/left.jar" --right "$WORK/target/left.jar" \
  --json "$WORK/same.json" > "$WORK/same.out" 2> "$WORK/same.err"
fallback --left "$WORK/target/left.jar" --right "$WORK/target/right.jar" \
  --json "$WORK/diff.json" > "$WORK/diff.out" 2> "$WORK/diff.err"
python3 - "$WORK/same.json" "$WORK/diff.json" <<'PY'
import json, sys
same, diff = (json.load(open(p, encoding="utf-8")) for p in sys.argv[1:])
assert same["counts"]["SAME"] == 1 and same["counts"]["DIFF"] == 0, same
row = diff["classes"][0]
assert row["status"] == "DIFF" and row["firstDiff"]["member"] == "value()I", row
assert row["firstDiff"]["insnIndex"] == 0, row
PY
cat "$WORK/same.out" "$WORK/diff.out"
! grep -q ': OK' "$WORK/same.out" "$WORK/diff.out"
grep -q ': OK' "$WORK/same.err"
echo "PASS FALLBACK 3 actual comparisons SAME=1 / DIFF=1; checksum diagnostics only on stderr"
PATH="$WORK/bin" "$WORK/bin/bash" "$TOOL/build.sh" > "$WORK/build.out" 2> "$WORK/build.err"
echo "PASS FALLBACK 4 build.sh also succeeds without Maven"
echo "=== FALLBACK ALL PASS ==="
echo "fallback evidence: $WORK"
