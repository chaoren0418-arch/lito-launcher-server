#!/usr/bin/env bash
# B05 staged verify: per-file javac --release 17, member overlay, T020d semantic-diff.
# Usage: apply-b05.sh [workdir]
# Default workdir is a temporary directory. Pass scratch/issue-66 to keep logs.
set -euo pipefail

OVERLAY_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_ROOT="$(cd "$OVERLAY_DIR/../../.." && pwd)"
REPO_ROOT="$(cd "$SERVER_ROOT/../.." && pwd)"
STAGED="$SERVER_ROOT/merge-inputs/50635f7e/staged/local/lito381/server"
CANDIDATE="$SERVER_ROOT/reference/candidate-50635f7e.jar"
BASE="$SERVER_ROOT/reference/base-0.6.71.jar"
BATCHES="$REPO_ROOT/docs/combat-merge/batches.json"
SEMDIFF_DIR="$SERVER_ROOT/tools/semantic-diff"
OVERLAY_SRC="$OVERLAY_DIR/OverlayB05.java"

if [[ $# -ge 1 ]]; then
  WORKDIR="$(mkdir -p "$1" && cd "$1" && pwd)"
else
  WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/b05-overlay.XXXXXX")"
fi

CP="$CANDIDATE:$BASE"
if [[ -d "$SERVER_ROOT/lib" ]]; then
  shopt -s nullglob
  for jar in "$SERVER_ROOT/lib"/*.jar; do
    CP="$CP:$jar"
  done
  shopt -u nullglob
fi

python3 - "$BATCHES" "$WORKDIR" <<'PY'
import json, sys
from pathlib import Path
batches = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
b05 = next(b for b in batches["batches"] if b["id"] == "B05")
out = Path(sys.argv[2])
(out / "classes.txt").write_text("\n".join(b05["classes"]) + "\n", encoding="utf-8")
(out / "verify_entries.txt").write_text("\n".join(b05["verify_entries"]) + "\n", encoding="utf-8")
print(f"B05 classes={len(b05['classes'])} verify_entries={len(b05['verify_entries'])}")
PY

rm -rf "$WORKDIR/classes"
mkdir -p "$WORKDIR/classes"
: > "$WORKDIR/javac-each.log"
while IFS= read -r cls; do
  [[ -z "$cls" ]] && continue
  src="$STAGED/${cls}.java"
  if javac --release 17 -encoding UTF-8 -cp "$CP" -d "$WORKDIR/classes" "$src" >>"$WORKDIR/javac-each.log" 2>&1; then
    echo "===== ${cls}.java exit=0 =====" >>"$WORKDIR/javac-each.log"
  else
    echo "===== ${cls}.java exit=1 =====" >>"$WORKDIR/javac-each.log"
    echo "javac failed: $src" >&2
    tail -n 40 "$WORKDIR/javac-each.log" >&2
    exit 1
  fi
done < "$WORKDIR/classes.txt"

python3 - "$WORKDIR/classes" "$WORKDIR/verify_entries.txt" "$WORKDIR/b05-compiled.jar" <<'PY'
import sys, zipfile
from pathlib import Path
root = Path(sys.argv[1])
wanted = [line.strip() for line in Path(sys.argv[2]).read_text(encoding="utf-8").splitlines() if line.strip()]
wanted_set = set(wanted)
produced = sorted(p.relative_to(root).as_posix() for p in root.rglob("*.class"))
extra = [p for p in produced if p not in wanted_set]
missing = [p for p in wanted if p not in set(produced)]
print(f"compiled_classes={len(produced)} extra={len(extra)} missing={len(missing)}")
if extra or missing:
    print("extra:", extra)
    print("missing:", missing)
    sys.exit(1)
with zipfile.ZipFile(sys.argv[3], "w") as z:
    for rel in produced:
        z.write(root / rel, rel)
PY

SEMDIFF="$SEMDIFF_DIR/semantic-diff"
bash "$SEMDIFF" \
  --left "$WORKDIR/b05-compiled.jar" \
  --right "$CANDIDATE" \
  --classes "$WORKDIR/verify_entries.txt" \
  --json "$WORKDIR/semantic-compiled.json" \
  --md "$WORKDIR/semantic-compiled.md" | tee "$WORKDIR/semantic-compiled.summary"

source "$SEMDIFF_DIR/build-support.sh"
ROOT="$SEMDIFF_DIR"
SEM_CP="$(classpath)"
OVERLAY_CLASSES="$WORKDIR/overlay-classes"
rm -rf "$OVERLAY_CLASSES"
mkdir -p "$OVERLAY_CLASSES"
javac --release 17 -encoding UTF-8 -cp "$SEM_CP" -d "$OVERLAY_CLASSES" "$OVERLAY_SRC"

java -cp "$OVERLAY_CLASSES:$SEM_CP" local.lito381.semanticdiff.OverlayB05 \
  "$WORKDIR/b05-compiled.jar" \
  "$CANDIDATE" \
  "$WORKDIR/verify_entries.txt" \
  "$WORKDIR/b05-merged.jar" \
  "$WORKDIR/overlay.log" | tee "$WORKDIR/overlay.summary"

bash "$SEMDIFF" \
  --left "$WORKDIR/b05-merged.jar" \
  --right "$CANDIDATE" \
  --classes "$WORKDIR/verify_entries.txt" \
  --json "$WORKDIR/semantic-merged.json" \
  --md "$WORKDIR/semantic-merged.md" | tee "$WORKDIR/semantic-merged.summary"

python3 - "$WORKDIR/semantic-compiled.json" "$WORKDIR/semantic-merged.json" <<'PY'
import json, sys
from pathlib import Path

def load(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))

def counts(data):
    c = data.get("counts") or {}
    return {k: int(c.get(k, 0)) for k in ("SAME", "DIFF", "ONLY_LEFT", "ONLY_RIGHT", "BYTE_SAME")}

def selection(data):
    sel = data.get("classSelection") or {}
    return sel.get("missingLeft") or [], sel.get("extraLeft") or []

pre = load(sys.argv[1])
post = load(sys.argv[2])
pre_c, post_c = counts(pre), counts(post)
pre_m, pre_e = selection(pre)
post_m, post_e = selection(post)
print("compiled", pre_c, "missingLeft", len(pre_m), "extraLeft", len(pre_e))
print("merged  ", post_c, "missingLeft", len(post_m), "extraLeft", len(post_e))
ok = (
    post_c["SAME"] == 120
    and post_c["DIFF"] == 0
    and post_c["ONLY_LEFT"] == 0
    and post_c["ONLY_RIGHT"] == 0
    and post_c["BYTE_SAME"] == 0
    and not post_m
    and not post_e
)
if not ok:
    sys.exit("B05 overlay verification failed")
print("B05 overlay verification OK SAME=120 DIFF=0 BYTE_SAME=0 extra=0 missing=0")
PY

echo "WORKDIR=$WORKDIR"
