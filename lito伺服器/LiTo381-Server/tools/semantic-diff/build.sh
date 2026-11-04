#!/usr/bin/env bash
# Compile with Maven if available, otherwise javac + checksum-verified ASM jars.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
source "$ROOT/build-support.sh"
build_tool
echo "built $ROOT/target/classes"
