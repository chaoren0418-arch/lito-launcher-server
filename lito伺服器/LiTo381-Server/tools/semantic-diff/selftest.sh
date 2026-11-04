#!/usr/bin/env bash
# Three acceptance tests for semantic-diff. Exit 0 only if all pass.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SERVER="$(cd "$ROOT/../.." && pwd)"
REF="$SERVER/reference/base-0.6.71.jar"
BUILD="$ROOT/build/selftest"
mkdir -p "$BUILD"
export PATH="${PATH}"

echo "==> build tool"
bash "$ROOT/semantic-diff" --help >/dev/null 2>&1 || true
# force a compile by invoking with missing args (ok) then a real compare later
if ! java -cp /dev/null -version >/dev/null 2>&1; then
  echo "java required" >&2
  exit 1
fi

run_diff() {
  bash "$ROOT/semantic-diff" "$@"
}

run_logged() {
  local log="$1"
  shift
  run_diff "$@" | tee "$log"
  parse_summary "$(grep '^SUMMARY ' "$log")"
}

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

parse_summary() {
  local line="$1"
  [[ "$line" == SUMMARY* ]] || fail "expected SUMMARY line, got: $line"
  SAME="${line##*SAME=}"; SAME="${SAME%% *}"
  DIFF="${line#*DIFF=}"; DIFF="${DIFF%% *}"
  ONLY_LEFT="${line#*ONLY_LEFT=}"; ONLY_LEFT="${ONLY_LEFT%% *}"
  ONLY_RIGHT="${line#*ONLY_RIGHT=}"
}

# --- fixtures ---
cat > "$BUILD/Fixture.java" <<'JAVA'
package semanticdiff.fixture;

import java.util.function.IntUnaryOperator;

public final class Fixture {
    public static final int ANSWER = 42;

    public int compute(int n) {
        int x = n + ANSWER;
        return x;
    }

    public IntUnaryOperator plus() {
        return v -> v + ANSWER;
    }

    public static final class Inner {
        public String name() {
            return "inner";
        }
    }
}
JAVA

mkdir -p "$BUILD/mutated-src"
cat > "$BUILD/mutated-src/Fixture.java" <<'JAVA'
package semanticdiff.fixture;

import java.util.function.IntUnaryOperator;

public final class Fixture {
    public static final int ANSWER = 43;

    public int compute(int n) {
        int x = n + ANSWER;
        return x;
    }

    public IntUnaryOperator plus() {
        return v -> v + ANSWER;
    }

    public static final class Inner {
        public String name() {
            return "inner";
        }
    }
}
JAVA

compile_jar() {
  local src="$1"
  local outdir="$2"
  local jar="$3"
  local gflag="$4"
  rm -rf "$outdir"
  mkdir -p "$outdir"
  javac --release 17 -encoding UTF-8 $gflag -d "$outdir" "$src"
  jar cf "$jar" -C "$outdir" .
}

compile_jar "$BUILD/Fixture.java" "$BUILD/g-debug/classes" "$BUILD/g-debug.jar" "-g"
compile_jar "$BUILD/Fixture.java" "$BUILD/g-none/classes" "$BUILD/g-none.jar" "-g:none"
compile_jar "$BUILD/mutated-src/Fixture.java" "$BUILD/mutated/classes" "$BUILD/mutated.jar" "-g:none"

echo
echo "=== SELFTEST 1: same JAR vs itself ==="
if [[ ! -f "$REF" ]]; then
  fail "missing reference JAR: $REF"
fi
run_logged "$BUILD/self-ref.log" --left "$REF" --right "$REF" --json "$BUILD/self-ref.json"
SUMMARY1="SUMMARY SAME=$SAME DIFF=$DIFF ONLY_LEFT=$ONLY_LEFT ONLY_RIGHT=$ONLY_RIGHT"
[[ "$DIFF" == "0" && "$ONLY_LEFT" == "0" && "$ONLY_RIGHT" == "0" ]] \
  || fail "self vs self must have 0 DIFF/ONLY_*; got $SUMMARY1"
[[ "$SAME" != "0" ]] || fail "self vs self SAME must be > 0"
echo "PASS SELFTEST 1 SAME=$SAME DIFF=0"

echo
echo "=== SELFTEST 1b: fixture JAR vs itself ==="
run_logged "$BUILD/self-fixture.log" --left "$BUILD/g-debug.jar" --right "$BUILD/g-debug.jar"
SUMMARY1B="SUMMARY SAME=$SAME DIFF=$DIFF ONLY_LEFT=$ONLY_LEFT ONLY_RIGHT=$ONLY_RIGHT"
[[ "$DIFF" == "0" && "$ONLY_LEFT" == "0" && "$ONLY_RIGHT" == "0" ]] \
  || fail "fixture vs itself must be 0 DIFF; got $SUMMARY1B"
echo "PASS SELFTEST 1b SAME=$SAME DIFF=0"

echo
echo "=== SELFTEST 2: -g vs -g:none ==="
run_logged "$BUILD/g-vs-none.log" --left "$BUILD/g-debug.jar" --right "$BUILD/g-none.jar" --json "$BUILD/g-vs-none.json" --md "$BUILD/g-vs-none.md"
SUMMARY2="SUMMARY SAME=$SAME DIFF=$DIFF ONLY_LEFT=$ONLY_LEFT ONLY_RIGHT=$ONLY_RIGHT"
[[ "$DIFF" == "0" && "$ONLY_LEFT" == "0" && "$ONLY_RIGHT" == "0" ]] \
  || fail "-g vs -g:none must be 0 DIFF; got $SUMMARY2"
echo "PASS SELFTEST 2 SAME=$SAME DIFF=0"

echo
echo "=== SELFTEST 3: constant change must be detected ==="
run_logged "$BUILD/mutated.log" --left "$BUILD/g-none.jar" --right "$BUILD/mutated.jar" --json "$BUILD/mutated.json"
SUMMARY3="SUMMARY SAME=$SAME DIFF=$DIFF ONLY_LEFT=$ONLY_LEFT ONLY_RIGHT=$ONLY_RIGHT"
[[ "$DIFF" != "0" ]] || fail "mutated constant must produce DIFF>=1; got $SUMMARY3"
python3 - "$BUILD/mutated.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
diffs = [c for c in data["classes"] if c["status"] == "DIFF"]
assert diffs, "expected at least one DIFF class"
print("DIFF classes:")
for c in diffs:
    fd = c.get("firstDiff") or {}
    print(" ", c["name"], fd.get("where"), fd.get("member"), "insn", fd.get("insnIndex"), fd.get("detail"))
PY
echo "PASS SELFTEST 3 DIFF=$DIFF"

echo
echo "=== SELFTEST ALL PASS ==="
echo "SELFTEST_1 $SUMMARY1"
echo "SELFTEST_1b $SUMMARY1B"
echo "SELFTEST_2 $SUMMARY2"
echo "SELFTEST_3 $SUMMARY3"
