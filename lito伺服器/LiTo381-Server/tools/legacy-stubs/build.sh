#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
shopt -s globstar nullglob
sources=(compile-stubs/**/*.java)
if (( ${#sources[@]} == 0 )); then
  echo 'No compile-stubs Java sources; run tools/legacy-stubs/scan.py first.' >&2
  exit 1
fi
mkdir -p target/legacy-stubs
"$JAVAC" --release 17 -encoding UTF-8 -d target/legacy-stubs "${sources[@]}"
printf 'Compiled %s compile-only sources -> target/legacy-stubs (never packaged)\n' "${#sources[@]}"
