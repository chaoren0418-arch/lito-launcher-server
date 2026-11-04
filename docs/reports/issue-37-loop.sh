#!/usr/bin/env bash
# Issue #37 — 連續跑 ClientSessionClassicWorldMonsterAggressionIntegrationTest。
# 每輪把 Maven 輸出存成獨立 log、記錄 exit code，並解析 Surefire 統計。
# 任一輪非零退出或 Failures/Errors > 0 時，彙總後以 exit 1 結束。
# 產出在 target/（不進 git）。CI 將此目錄上傳為 artifact。
set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SERVER="$ROOT/lito伺服器/LiTo381-Server"
OUT="${ISSUE_37_LOOP_OUT:-$SERVER/target/issue-37-loop}"
TEST="${ISSUE_37_LOOP_TEST:-ClientSessionClassicWorldMonsterAggressionIntegrationTest}"
N="${ISSUE_37_LOOP_COUNT:-50}"

mkdir -p "$OUT"
rm -f "$OUT"/run-*.log "$OUT"/summary.tsv "$OUT"/summary.json "$OUT"/summary.md \
      "$OUT"/tested-revision.txt "$OUT"/compile.log "$OUT"/meta.txt

{
  echo "date_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "hostname=$(hostname)"
  echo "pwd=$SERVER"
  echo "test=$TEST"
  echo "loop_count=$N"
  git -C "$ROOT" rev-parse HEAD
  git -C "$ROOT" log -1 --format='commit_subject=%s'
  echo "--- java ---"
  java -version 2>&1
  echo "--- mvn ---"
  mvn -version 2>&1
} | tee "$OUT/meta.txt"

git -C "$ROOT" rev-parse HEAD | tee "$OUT/tested-revision.txt"
TESTED_SHA="$(cat "$OUT/tested-revision.txt")"

cd "$SERVER"
echo "compile: mvn -B -DskipTests compile test-compile"
if ! mvn -B -DskipTests compile test-compile > "$OUT/compile.log" 2>&1; then
  echo "COMPILE_FAILED" | tee "$OUT/summary.md"
  tail -40 "$OUT/compile.log"
  exit 1
fi

printf 'run\texit_code\ttests\tfailures\terrors\tskipped\telapsed_s\tstatus\n' \
    > "$OUT/summary.tsv"

pass=0
fail=0
i=1
while [ "$i" -le "$N" ]; do
  start_s="$(date +%s)"
  set +e
  mvn -B -Dtest="$TEST" -Dsurefire.rerunFailingTestsCount=0 surefire:test \
      > "$OUT/run-$i.log" 2>&1
  code=$?
  set -e
  elapsed=$(( $(date +%s) - start_s ))

  class_line="$(
    grep -E "Time elapsed:.*${TEST}" "$OUT/run-$i.log" | tail -1 || true
  )"
  if [ -n "$class_line" ]; then
    tests="$(echo "$class_line" | sed -n 's/.*Tests run: \([0-9]*\).*/\1/p')"
    failures="$(echo "$class_line" | sed -n 's/.*Failures: \([0-9]*\).*/\1/p')"
    errors="$(echo "$class_line" | sed -n 's/.*Errors: \([0-9]*\).*/\1/p')"
    skipped="$(echo "$class_line" | sed -n 's/.*Skipped: \([0-9]*\).*/\1/p')"
  else
    tests=NA
    failures=NA
    errors=NA
    skipped=NA
  fi

  if [ "$code" -eq 0 ] && [ "$failures" = 0 ] && [ "$errors" = 0 ] \
      && [ "$tests" != "NA" ] && [ "$tests" != 0 ]; then
    status=PASS
    pass=$((pass + 1))
  else
    status=FAIL
    fail=$((fail + 1))
  fi

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$i" "$code" "$tests" "$failures" "$errors" "$skipped" "$elapsed" "$status" \
      >> "$OUT/summary.tsv"
  echo "run=$i status=$status exit=$code tests=$tests failures=$failures errors=$errors skipped=$skipped elapsed=${elapsed}s"
  i=$((i + 1))
done

python3 - "$OUT" "$TESTED_SHA" "$N" "$pass" "$fail" "$TEST" <<'PY'
import json, pathlib, sys
out, sha, n, passed, failed, test = sys.argv[1:7]
n, passed, failed = int(n), int(passed), int(failed)
rows = []
lines = pathlib.Path(out, "summary.tsv").read_text(encoding="utf-8").splitlines()
header = lines[0].split("\t")
for line in lines[1:]:
    rec = dict(zip(header, line.split("\t")))
    for key in ("run", "exit_code", "elapsed_s"):
        rec[key] = int(rec[key])
    if rec["tests"] != "NA":
        for key in ("tests", "failures", "errors", "skipped"):
            rec[key] = int(rec[key])
    rows.append(rec)
payload = {
    "tested_revision": sha,
    "test": test,
    "loop_count": n,
    "pass": passed,
    "fail": failed,
    "runs": rows,
}
pathlib.Path(out, "summary.json").write_text(
    json.dumps(payload, indent=2) + "\n", encoding="utf-8")

md = []
md.append("# Issue 37 companion loop")
md.append("")
md.append(f"- tested_revision: `{sha}`")
md.append(f"- test: `{test}`")
md.append(f"- pass: **{passed}** / {n}")
md.append(f"- fail: **{failed}** / {n}")
md.append("")
md.append("| run | exit_code | Tests run | Failures | Errors | Skipped | elapsed_s | status |")
md.append("|---:|---:|---:|---:|---:|---:|---:|:---|")
for rec in rows:
    md.append(
        "| {run} | {exit_code} | {tests} | {failures} | {errors} | {skipped} | {elapsed_s} | {status} |".format(**rec)
    )
pathlib.Path(out, "summary.md").write_text("\n".join(md) + "\n", encoding="utf-8")
PY

echo "SUMMARY pass=$pass fail=$fail sha=$TESTED_SHA out=$OUT"
if [ "$fail" -ne 0 ] || [ "$pass" -ne "$N" ]; then
  exit 1
fi
exit 0
