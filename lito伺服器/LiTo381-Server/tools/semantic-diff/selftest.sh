#!/usr/bin/env bash
# Acceptance tests for semantic-diff. Exit 0 only if all pass.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SERVER="$(cd "$ROOT/../.." && pwd)"
REF="$SERVER/reference/base-0.6.71.jar"
BUILD="$ROOT/build/selftest"
mkdir -p "$BUILD"
export PATH="${PATH}"

echo "==> build tool"
bash "$ROOT/semantic-diff" --help >/dev/null 2>&1 || true
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

expect_fail() {
  local log="$1"
  shift
  set +e
  run_diff "$@" > "$log" 2> "$log.err"
  local code=$?
  set -e
  cat "$log" "$log.err"
  [[ "$code" -ne 0 ]] || fail "expected non-zero exit for: $*"
}

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

parse_summary() {
  local line="$1"
  [[ "$line" == SUMMARY* ]] || fail "expected SUMMARY line, got: $line"
  SAME="${line#* SAME=}"; SAME="${SAME%% *}"
  DIFF="${line#* DIFF=}"; DIFF="${DIFF%% *}"
  ONLY_LEFT="${line#* ONLY_LEFT=}"; ONLY_LEFT="${ONLY_LEFT%% *}"
  ONLY_RIGHT="${line#* ONLY_RIGHT=}"; ONLY_RIGHT="${ONLY_RIGHT%% *}"
  BYTE_SAME="${line#* BYTE_SAME=}"; BYTE_SAME="${BYTE_SAME%% *}"
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

cat > "$BUILD/Extra.java" <<'JAVA'
package semanticdiff.fixture;

public final class Extra {
    public int extra() {
        return 1;
    }
}
JAVA

mkdir -p "$BUILD/mutated-src" "$BUILD/extra-mut-src" "$BUILD/both-mut-src" "$BUILD/ann-src"
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

cat > "$BUILD/extra-mut-src/Extra.java" <<'JAVA'
package semanticdiff.fixture;

public final class Extra {
    public int extra() {
        return 2;
    }
}
JAVA

cat > "$BUILD/ann-src/Marker.java" <<'JAVA'
package semanticdiff.fixture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Marker {}
JAVA

cat > "$BUILD/ann-src/InnerAnn.java" <<'JAVA'
package semanticdiff.fixture;

public @interface InnerAnn {
    enum Kind { A, B }
    Kind kind() default Kind.A;
}
JAVA

cat > "$BUILD/ann-src/OuterAnn.java" <<'JAVA'
package semanticdiff.fixture;

public @interface OuterAnn {
    InnerAnn nested() default @InnerAnn(kind = InnerAnn.Kind.B);
    InnerAnn.Kind kind() default InnerAnn.Kind.A;
}
JAVA

cat > "$BUILD/ann-src/AnnoHost.java" <<'JAVA'
package semanticdiff.fixture;

@OuterAnn
public final class AnnoHost {
    public int n() {
        return 0;
    }
}
JAVA

cat > "$BUILD/ann-src/Rec.java" <<'JAVA'
package semanticdiff.fixture;

public record Rec(int x) {}
JAVA

compile_jar() {
  local outdir="$1"
  local jar="$2"
  local gflag="$3"
  shift 3
  rm -rf "$outdir"
  mkdir -p "$outdir"
  javac --release 17 -encoding UTF-8 $gflag -d "$outdir" "$@"
  jar cf "$jar" -C "$outdir" .
}

compile_jar "$BUILD/g-debug/classes" "$BUILD/g-debug.jar" "-g" \
  "$BUILD/Fixture.java" "$BUILD/Extra.java"
compile_jar "$BUILD/g-none/classes" "$BUILD/g-none.jar" "-g:none" \
  "$BUILD/Fixture.java" "$BUILD/Extra.java"
compile_jar "$BUILD/mutated/classes" "$BUILD/mutated.jar" "-g:none" \
  "$BUILD/mutated-src/Fixture.java" "$BUILD/Extra.java"
compile_jar "$BUILD/both-mut/classes" "$BUILD/both-mut.jar" "-g:none" \
  "$BUILD/mutated-src/Fixture.java" "$BUILD/extra-mut-src/Extra.java"
compile_jar "$BUILD/ann/classes" "$BUILD/ann.jar" "-g:none" \
  "$BUILD/ann-src/Marker.java" "$BUILD/ann-src/InnerAnn.java" \
  "$BUILD/ann-src/OuterAnn.java" "$BUILD/ann-src/AnnoHost.java" \
  "$BUILD/ann-src/Rec.java"

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
echo "=== SELFTEST 4: --exclude outer class also drops \$ inner ==="
run_logged "$BUILD/exclude.log" \
  --left "$BUILD/g-none.jar" --right "$BUILD/mutated.jar" \
  --exclude "semanticdiff.fixture.Fixture" \
  --json "$BUILD/exclude.json"
SUMMARY4="SUMMARY SAME=$SAME DIFF=$DIFF ONLY_LEFT=$ONLY_LEFT ONLY_RIGHT=$ONLY_RIGHT"
python3 - "$BUILD/exclude.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
names = {c["name"] for c in data["classes"]}
assert "semanticdiff/fixture/Fixture" not in names, names
assert "semanticdiff/fixture/Fixture$Inner" not in names, names
assert "semanticdiff/fixture/Extra" in names, names
assert data["counts"]["DIFF"] == 0, data["counts"]
assert data["counts"]["SAME"] >= 1, data["counts"]
print("exclude kept", sorted(names))
PY
echo "PASS SELFTEST 4 SAME=$SAME DIFF=$DIFF (Fixture+Inner excluded)"

echo
echo "=== SELFTEST 5: --allow-methods overlays one method; unlisted diffs remain ==="
printf '%s\n' 'semanticdiff/fixture/Extra#extra()I' > "$BUILD/allow-methods.txt"
run_logged "$BUILD/allow.log" \
  --left "$BUILD/g-none.jar" --right "$BUILD/both-mut.jar" \
  --allow-methods "$BUILD/allow-methods.txt" --allow-right "$BUILD/g-none.jar" \
  --json "$BUILD/allow.json"
SUMMARY5="SUMMARY SAME=$SAME DIFF=$DIFF ONLY_LEFT=$ONLY_LEFT ONLY_RIGHT=$ONLY_RIGHT"
python3 - "$BUILD/allow.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
by = {c["name"]: c for c in data["classes"]}
extra = by["semanticdiff/fixture/Extra"]
fix = by["semanticdiff/fixture/Fixture"]
assert extra["status"] == "SAME", extra
assert extra["allowMethodsApplied"] is True
assert fix["status"] == "DIFF", fix
print("allow Extra=SAME Fixture=DIFF", fix.get("firstDiff"))
PY
echo "PASS SELFTEST 5 Extra SAME via allow-right; Fixture DIFF still caught ($SUMMARY5)"

echo
echo "=== SELFTEST 6: --classes .class suffix, comma list, file list, missing class ==="
run_logged "$BUILD/classes-suffix.log" \
  --left "$BUILD/g-none.jar" --right "$BUILD/g-none.jar" \
  --classes "semanticdiff/fixture/Fixture.class"
[[ "$SAME" == "1" && "$DIFF" == "0" ]] || fail "Fixture.class suffix should match 1 class, got SAME=$SAME DIFF=$DIFF"
echo "PASS SELFTEST 6a suffix SAME=$SAME"

run_logged "$BUILD/classes-comma.log" \
  --left "$BUILD/g-none.jar" --right "$BUILD/g-none.jar" \
  --classes "semanticdiff.fixture.Fixture,semanticdiff.fixture.Extra"
[[ "$SAME" == "2" && "$DIFF" == "0" ]] || fail "comma list should match 2 classes, got SAME=$SAME"
echo "PASS SELFTEST 6b comma SAME=$SAME"

printf '%s\n' 'semanticdiff/fixture/Fixture.class' 'semanticdiff.fixture.Extra' > "$BUILD/classes.txt"
run_logged "$BUILD/classes-file.log" \
  --left "$BUILD/g-none.jar" --right "$BUILD/g-none.jar" \
  --classes "$BUILD/classes.txt"
[[ "$SAME" == "2" && "$DIFF" == "0" ]] || fail "file list should match 2 classes, got SAME=$SAME"
echo "PASS SELFTEST 6c file SAME=$SAME"

expect_fail "$BUILD/classes-missing.log" \
  --left "$BUILD/g-none.jar" --right "$BUILD/g-none.jar" \
  --classes "semanticdiff.fixture.DoesNotExist"
grep -q "missing --classes entries: semanticdiff/fixture/DoesNotExist" "$BUILD/classes-missing.log.err" \
  || fail "missing class must be reported"
echo "PASS SELFTEST 6d missing class exits non-zero"

echo
echo "=== SELFTEST 7: enum/nested annotationDefault self-vs-self ==="
run_logged "$BUILD/ann-self.log" --left "$BUILD/ann.jar" --right "$BUILD/ann.jar" --json "$BUILD/ann-self.json"
SUMMARY7="SUMMARY SAME=$SAME DIFF=$DIFF ONLY_LEFT=$ONLY_LEFT ONLY_RIGHT=$ONLY_RIGHT"
[[ "$DIFF" == "0" && "$SAME" != "0" ]] || fail "annotation jar vs itself must be SAME; got $SUMMARY7"
echo "PASS SELFTEST 7 SAME=$SAME DIFF=0"

echo
echo "=== SELFTEST 8: type-annotation-only change must DIFF ==="
ASM_CP=""
if [[ -d "$ROOT/target/dependency" ]]; then
  ASM_CP="$(printf '%s:' "$ROOT/target/dependency"/*.jar)"
fi
if [[ -z "$ASM_CP" || "$ASM_CP" == "$ROOT/target/dependency/*.jar:" ]]; then
  ASM_CP="$ROOT/lib/asm-9.7.jar:$ROOT/lib/asm-tree-9.7.jar"
fi
cat > "$BUILD/AnnPatch.java" <<'JAVA'
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

public final class AnnPatch {
    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path inJar = Path.of(args[1]);
        Path outJar = Path.of(args[2]);
        String internal = args[3];
        byte[] data;
        try (ZipFile zip = new ZipFile(inJar.toFile())) {
            ZipEntry e = zip.getEntry(internal + ".class");
            try (InputStream in = zip.getInputStream(e)) {
                data = in.readAllBytes();
            }
        }
        ClassNode node = new ClassNode();
        new ClassReader(data).accept(node, 0);
        TypeAnnotationNode tan = new TypeAnnotationNode(
                TypeReference.newTypeReference(TypeReference.METHOD_RETURN).getValue(),
                null,
                "Lsemanticdiff/fixture/Marker;");
        switch (mode) {
            case "method" -> {
                MethodNode method = node.methods.get(0);
                if (method.visibleTypeAnnotations == null) {
                    method.visibleTypeAnnotations = new ArrayList<>();
                }
                method.visibleTypeAnnotations.add(tan);
            }
            case "insn" -> {
                MethodNode method = node.methods.stream()
                        .filter(m -> m.instructions != null && m.instructions.size() > 0)
                        .findFirst()
                        .orElseThrow();
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn.getOpcode() >= 0) {
                        if (insn.visibleTypeAnnotations == null) {
                            insn.visibleTypeAnnotations = new ArrayList<>();
                        }
                        insn.visibleTypeAnnotations.add(new TypeAnnotationNode(
                                TypeReference.newTypeReference(TypeReference.NEW).getValue(),
                                null,
                                "Lsemanticdiff/fixture/Marker;"));
                        break;
                    }
                }
            }
            case "record" -> {
                RecordComponentNode rec = node.recordComponents.get(0);
                if (rec.visibleTypeAnnotations == null) {
                    rec.visibleTypeAnnotations = new ArrayList<>();
                }
                rec.visibleTypeAnnotations.add(new TypeAnnotationNode(
                        TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                        null,
                        "Lsemanticdiff/fixture/Marker;"));
            }
            default -> throw new IllegalArgumentException(mode);
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] patched = writer.toByteArray();
        try (ZipFile zip = new ZipFile(inJar.toFile());
                ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outJar))) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                out.putNextEntry(new ZipEntry(e.getName()));
                if (e.getName().equals(internal + ".class")) {
                    out.write(patched);
                } else {
                    try (InputStream in = zip.getInputStream(e)) {
                        in.transferTo(out);
                    }
                }
                out.closeEntry();
            }
        }
    }
}
JAVA
javac --release 17 -encoding UTF-8 -cp "$ASM_CP" -d "$BUILD" "$BUILD/AnnPatch.java"
java -cp "$BUILD:$ASM_CP" AnnPatch method "$BUILD/ann.jar" "$BUILD/ann-method.jar" semanticdiff/fixture/AnnoHost
java -cp "$BUILD:$ASM_CP" AnnPatch insn "$BUILD/ann.jar" "$BUILD/ann-insn.jar" semanticdiff/fixture/AnnoHost
java -cp "$BUILD:$ASM_CP" AnnPatch record "$BUILD/ann.jar" "$BUILD/ann-record.jar" semanticdiff/fixture/Rec

run_logged "$BUILD/ann-method.log" \
  --left "$BUILD/ann.jar" --right "$BUILD/ann-method.jar" \
  --classes semanticdiff.fixture.AnnoHost --json "$BUILD/ann-method.json"
[[ "$DIFF" == "1" ]] || fail "method type annotation change must DIFF; got DIFF=$DIFF"
echo "PASS SELFTEST 8a method type-ann DIFF=1"

run_logged "$BUILD/ann-insn.log" \
  --left "$BUILD/ann.jar" --right "$BUILD/ann-insn.jar" \
  --classes semanticdiff.fixture.AnnoHost --json "$BUILD/ann-insn.json"
[[ "$DIFF" == "1" ]] || fail "insn type annotation change must DIFF; got DIFF=$DIFF"
echo "PASS SELFTEST 8b insn type-ann DIFF=1"

run_logged "$BUILD/ann-record.log" \
  --left "$BUILD/ann.jar" --right "$BUILD/ann-record.jar" \
  --classes semanticdiff.fixture.Rec --json "$BUILD/ann-record.json"
[[ "$DIFF" == "1" ]] || fail "record type annotation change must DIFF; got DIFF=$DIFF"
echo "PASS SELFTEST 8c record type-ann DIFF=1"

echo
echo "=== SELFTEST ALL PASS ==="
echo "SELFTEST_1 $SUMMARY1"
echo "SELFTEST_1b $SUMMARY1B"
echo "SELFTEST_2 $SUMMARY2"
echo "SELFTEST_3 $SUMMARY3"
echo "SELFTEST_4 $SUMMARY4"
echo "SELFTEST_5 $SUMMARY5"
echo "SELFTEST_7 $SUMMARY7"
