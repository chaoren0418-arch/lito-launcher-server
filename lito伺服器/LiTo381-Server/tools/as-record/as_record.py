#!/usr/bin/env python3
"""獨立 javac/ASM CLI；不修改伺服器 Maven 接線。"""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.request

ROOT = Path(__file__).resolve().parent
TARGET = ROOT / 'target'
HASHES = {
    'asm': 'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d',
    'asm-tree': '62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1',
}


def jdk(tool):
    suffix = '.exe' if os.name == 'nt' else ''
    if os.environ.get('JAVA_HOME'):
        return str(Path(os.environ['JAVA_HOME']) / 'bin' / (tool + suffix))
    found = shutil.which(tool)
    if not found:
        raise RuntimeError(f'{tool} not found; set JAVA_HOME to JDK 17+')
    return found


def run(args, **kwargs):
    return subprocess.run([str(a) for a in args], check=True, **kwargs)


def dependencies():
    repo = Path(os.environ.get('AS_RECORD_MAVEN_REPO', Path.home() / '.m2/repository'))
    result = []
    for artifact, expected in HASHES.items():
        relative = f'org/ow2/asm/{artifact}/9.7/{artifact}-9.7.jar'
        cached = repo / relative
        dest = TARGET / 'dependency' / f'{artifact}-9.7.jar'
        path = cached if cached.is_file() else dest
        if not path.is_file():
            dest.parent.mkdir(parents=True, exist_ok=True)
            data = urllib.request.urlopen('https://repo.maven.apache.org/maven2/' + relative, timeout=60).read()
            if hashlib.sha256(data).hexdigest() != expected:
                raise RuntimeError(f'ASM checksum mismatch: {artifact}')
            dest.write_bytes(data)
        if hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise RuntimeError(f'ASM checksum mismatch: {path}')
        result.append(str(path))
    return os.pathsep.join(result)


def compile_java(sources, output, cp):
    output.mkdir(parents=True, exist_ok=True)
    run([jdk('javac'), '--release', '17', '-encoding', 'UTF-8', '-cp', cp,
         '-d', output, *sources])


def build():
    cp = dependencies()
    output = TARGET / 'classes'
    compile_java(sorted((ROOT / 'src/main/java').rglob('*.java')), output, cp)
    return str(output) + os.pathsep + cp


def main():
    if len(sys.argv) != 2:
        print('usage: as-record <classes directory>', file=sys.stderr)
        return 1
    try:
        cp = build()
        run([jdk('java'), '-cp', cp, 'local.lito381.asrecord.AsRecord', sys.argv[1]])
        return 0
    except (OSError, RuntimeError, subprocess.CalledProcessError) as ex:
        print(f'as-record: {ex}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
