#!/usr/bin/env bash
# Compile semantic-diff with Maven if available, otherwise javac + downloaded ASM jars.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
if command -v mvn >/dev/null 2>&1; then
  mvn -f "$ROOT/pom.xml" -B -DskipTests package \
    org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies \
    -DoutputDirectory="$ROOT/target/dependency" \
    -DincludeScope=runtime
  echo "built $ROOT/target/semantic-diff.jar"
  exit 0
fi
bash -c 'source /dev/null'
ASM_VERSION="9.7"
mkdir -p "$ROOT/lib" "$ROOT/target/classes"
for art in asm asm-tree; do
  jar="$ROOT/lib/${art}-${ASM_VERSION}.jar"
  if [[ ! -f "$jar" ]]; then
    curl -fsSL -o "$jar" \
      "https://repo1.maven.org/maven2/org/ow2/asm/${art}/${ASM_VERSION}/${art}-${ASM_VERSION}.jar"
  fi
done
mapfile -t srcs < <(find "$ROOT/src/main/java" -name '*.java' | sort)
javac --release 17 -encoding UTF-8 \
  -cp "$ROOT/lib/asm-${ASM_VERSION}.jar:$ROOT/lib/asm-tree-${ASM_VERSION}.jar" \
  -d "$ROOT/target/classes" \
  "${srcs[@]}"
echo "built $ROOT/target/classes (javac)"
