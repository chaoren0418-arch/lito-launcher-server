#!/usr/bin/env python3
"""Exercise scanner inference with isolated, explicitly synthetic Java fixtures."""
import argparse
import importlib.util
import os
from pathlib import Path
import subprocess
import sys
import zipfile

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('legacy_scan', HERE / 'scan.py')
scan = importlib.util.module_from_spec(spec)
spec.loader.exec_module(scan)


def run(command):
    subprocess.run([str(arg) for arg in command], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--javac', default=str(Path(os.environ['JAVA_HOME']) / 'bin/javac') if os.environ.get('JAVA_HOME') else 'javac')
    parser.add_argument('--java', default=str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else 'java')
    args = parser.parse_args()
    repo = HERE.parents[3]
    root = repo / 'scratch/issue-108/selftest'
    sources = {
        'com/lineage/fixture/Mode.java': 'package com.lineage.fixture; public enum Mode { FIRST, SECOND }',
        'com/lineage/fixture/Api.java': 'package com.lineage.fixture; public interface Api { int call(String[][] a); static int shared() { return 4; } }',
        'com/lineage/fixture/Helper.java': '''package com.lineage.fixture;
public class Helper {
 public static Helper INSTANCE; public int count; public static long total;
 public static int twice(int x) { return x * 2; }
 public static int handleOnly(int x) { return x; }
 public int method() { return count; }
}''',
        'consumer/Use.java': '''package consumer;
import com.lineage.fixture.*;
public class Use {
 public Mode[] array;
 public static int go(Api api, Helper helper, Mode mode, int x) {
  Helper.INSTANCE = helper; Helper.INSTANCE.count = 3; Helper.total = 7L;
  int a = api.call(new String[1][2]) + Api.shared() + Helper.twice(x) + helper.method();
  java.util.function.IntUnaryOperator f = Helper::handleOnly;
  switch (mode) { case FIRST: a++; break; case SECOND: a--; }
  switch (x) { case 1: a++; break; case 2: a--; break; case 3: a += 3; }
  switch (x) { case -100: a++; break; case 2000: a--; }
  return a + f.applyAsInt(x);
 }
 public static String text() { return "\\u0000\\uD83D\\uDE00"; }
}''',
    }
    files = []
    for name, text in sources.items():
        path = root / 'src' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf8')
        files.append(path)
    target = root / 'target'
    target.mkdir(parents=True, exist_ok=True)
    run([args.javac, '--release', '17', '-encoding', 'UTF-8', '-d', target / 'fixture', *files])
    jars = []
    for name in ('first.jar', 'second.jar'):
        path = target / name
        with zipfile.ZipFile(path, 'w') as z:
            # Scanner sees only consumers; definitions are deliberately absent.
            for cls in sorted((target / 'fixture/consumer').glob('*.class')):
                z.write(cls, 'consumer/' + cls.name)
        jars.append(path)
    inventory = scan.scan(jars)
    rows = {r['name']: r for r in inventory['classes']}
    assert rows['com/lineage/fixture/Mode']['kind'] == 'enum'
    assert rows['com/lineage/fixture/Mode']['enum_constants'] == ['FIRST', 'SECOND']
    assert rows['com/lineage/fixture/Api']['kind'] == 'interface'
    helper = rows['com/lineage/fixture/Helper']
    assert helper['kind'] == 'class', 'self-typed singleton must not become enum'
    assert any(m['name'] == 'handleOnly' and m['static'] for m in helper['methods'])
    assert next(m for m in helper['fields'] if m['name'] == 'count')['static'] is False
    generated = scan.generate(inventory)
    stubs = []
    for name, text in generated.items():
        p = root / 'stubs' / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text, encoding='utf8')
        stubs.append(p)
    run([args.javac, '--release', '17', '-d', target / 'stubs', *stubs])
    run([args.javac, '--release', '17', '-cp', target / 'stubs', '-d', target / 'consumer', root / 'src/consumer/Use.java'])
    print('PASS enum/interface/class, arrays, fields, static/instance, method handles, switch decoding; consumer recompiles')
    # Exercise wide operands and both switch layouts independent of javac choices.
    assert list(scan.instructions(bytes([196, 21, 1, 0, 196, 132, 1, 0, 0, 1, 177])))[-1][0] == 10
    for op, tail in [(170, (0, 0, 0, 0)), (171, (0, 1, 7, 0))]:
        import struct
        code = bytes([op, 0, 0, 0]) + struct.pack('>iiii', *tail) + bytes([177])
        assert list(scan.instructions(code))[-1][0] == 20
    try:
        scan.ClassFile(b'\xca\xfe\xba\xbe')
    except ValueError:
        pass
    else:
        raise AssertionError('truncated input accepted')
    print('PASS wide/tableswitch/lookupswitch alignment and truncated-input rejection')
    probe = root / 'src/ThrowProbe.java'
    probe.write_text('''public class ThrowProbe {
 public static void main(String[] args) {
  try { com.lineage.fixture.Helper.handleOnly(1); throw new AssertionError("stub returned"); }
  catch (UnsupportedOperationException expected) {
   if (!"compile-only stub".equals(expected.getMessage())) throw new AssertionError(expected);
  }
  System.out.println("PASS compile-only method throws UnsupportedOperationException");
 }
}''', encoding='utf8')
    run([args.javac, '--release', '17', '-cp', target / 'stubs', '-d', target / 'consumer', probe])
    run([args.java, '-cp', str(target / 'consumer') + os.pathsep + str(target / 'stubs'), 'ThrowProbe'])
    print('SELFTEST PASS')


if __name__ == '__main__':
    main()
