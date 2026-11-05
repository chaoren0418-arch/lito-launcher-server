#!/usr/bin/env bash
# Shared by build.sh and the CLI. ROOT is the tool directory.
ASM_VERSION="9.7"
ASM_SHA256_ASM="adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d"
ASM_SHA256_ANALYSIS="7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f"
ASM_SHA256_TREE="62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1"

download_asm() {
  mkdir -p "$ROOT/target/dependency"
  local art jar
  for art in asm asm-tree asm-analysis; do
    jar="$ROOT/target/dependency/${art}-${ASM_VERSION}.jar"
    if [[ ! -f "$jar" ]]; then
      curl -fsSL -o "$jar" \
        "https://repo.maven.apache.org/maven2/org/ow2/asm/${art}/${ASM_VERSION}/${art}-${ASM_VERSION}.jar" >&2
    fi
  done
  # Also called inside CP="$(classpath)": stdout must stay empty.
  echo "${ASM_SHA256_ASM}  $ROOT/target/dependency/asm-${ASM_VERSION}.jar" | sha256sum -c - >&2
  echo "${ASM_SHA256_TREE}  $ROOT/target/dependency/asm-tree-${ASM_VERSION}.jar" | sha256sum -c - >&2
  echo "${ASM_SHA256_ANALYSIS}  $ROOT/target/dependency/asm-analysis-${ASM_VERSION}.jar" | sha256sum -c - >&2
}

build_tool() {
  if command -v mvn >/dev/null 2>&1; then
    mvn -f "$ROOT/pom.xml" -B -DskipTests package \
      org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies \
      -DoutputDirectory="$ROOT/target/dependency" -DincludeScope=runtime >&2
  else
    download_asm
    mkdir -p "$ROOT/target/classes"
    local srcs
    mapfile -t srcs < <(find "$ROOT/src/main/java" -name '*.java' | sort)
    javac --release 17 -encoding UTF-8 \
      -cp "$ROOT/target/dependency/*" \
      -d "$ROOT/target/classes" "${srcs[@]}" >&2
  fi
}

classpath() {
  download_asm
  printf '%s' "$ROOT/target/classes:$ROOT/target/dependency/asm-${ASM_VERSION}.jar:$ROOT/target/dependency/asm-tree-${ASM_VERSION}.jar:$ROOT/target/dependency/asm-analysis-${ASM_VERSION}.jar"
}
