#!/usr/bin/env python3
"""檢查 package 成品，並用相同 POM 的隔離模組驗證 main 轉換與掃描器。"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

sys.dont_write_bytecode = True
from as_record import build, jdk

TOOL = Path(__file__).resolve().parent
SERVER = TOOL.parents[1]
ROOT = SERVER.parents[1]
PACKAGE = 'local/lito381/server'
FORBIDDEN = ('com/lineage/', 'org/objectweb/asm/', 'local/lito381/asrecord/')


def run(args, **kwargs):
    subprocess.run(list(map(str, args)), check=True, **kwargs)


def verify_jars(target):
    for name in ('LiTo381-Server.jar', 'LiTo381-Server-modern.jar',
                 'LiTo381-Server-as-record-tests.jar'):
        with zipfile.ZipFile(target / name) as jar:
            assert not [n for n in jar.namelist() if n.startswith(FORBIDDEN)], name
            if 'as-record-tests' not in name:
                assert not any('AsRecordBuildFixture' in n or 'AsRecordBuildTest' in n
                               for n in jar.namelist()), name
            manifest = jar.read('META-INF/MANIFEST.MF').decode()
            assert 'asm-' not in manifest and 'legacy-stubs' not in manifest, manifest
    for path in (target / 'lib').glob('*.jar'):
        with zipfile.ZipFile(path) as jar:
            assert not any(n.startswith(FORBIDDEN) for n in jar.namelist()), path
    run([jdk('java'), '-Xverify:all', TOOL / 'src/test/java/VerifyMavenArtifacts.java',
         target / 'LiTo381-Server-as-record-tests.jar', PACKAGE.replace('/', '.') + '.AsRecordBuildFixture'])
    print('PACKAGE PASS: main/modern/test JAR and runtime lib contain no ASM/stubs/converter', flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--maven', required=True, help='mvn 或 mvn.cmd 路徑')
    parser.add_argument('--maven-repo', required=True, type=Path, help='隔離 Maven 快取')
    parser.add_argument('--module-only', action='store_true', help='只執行隔離模組，不檢查伺服器既有產物')
    args = parser.parse_args()
    if not args.module_only:
        verify_jars(SERVER / 'target')
    module = ROOT / 'scratch/issue-134/maven-selftest'
    module.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SERVER / 'pom.xml', module / 'pom.xml')
    # 真正使用同一份 POM 與工具；只替換來源集合，不改主專案 src/main。
    for relative in ('compile-stubs', 'tools/as-record/src/main/java'):
        shutil.copytree(SERVER / relative, module / relative, dirs_exist_ok=True)
    main_src = module / 'src/main/java' / PACKAGE
    test_src = module / 'src/test/java' / PACKAGE
    main_src.mkdir(parents=True, exist_ok=True)
    test_src.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SERVER / 'src/main/java' / PACKAGE / 'AsRecord.java', main_src / 'AsRecord.java')
    fixture = (SERVER / 'src/test/java' / PACKAGE / 'AsRecordBuildFixture.java').read_text(encoding='utf-8')
    (main_src / 'AsRecordMainFixture.java').write_text(
        fixture.replace('AsRecordBuildFixture', 'AsRecordMainFixture'), encoding='utf-8')
    for name in ('AsRecordBuildFixture.java', 'AsRecordBuildTest.java'):
        shutil.copy2(SERVER / 'src/test/java' / PACKAGE / name, test_src / name)
    temporary = module / 'tmp'
    temporary.mkdir(exist_ok=True)
    env = os.environ.copy()
    env['TEMP'] = env['TMP'] = str(temporary)
    env.pop('JAVA_TOOL_OPTIONS', None)
    env['MAVEN_OPTS'] = '-Djava.io.tmpdir=' + str(temporary)
    base = [args.maven, '-B', f'-Dmaven.repo.local={args.maven_repo.resolve()}',
            f'-DargLine=-Djava.io.tmpdir={temporary}']
    run([*base, 'clean', 'package', '-Dmaven.test.skip=false'], cwd=module, env=env)
    verify_jars(module / 'target')
    product = module / 'target/LiTo381-Server.jar'
    run([jdk('java'), '-Xverify:all', TOOL / 'src/test/java/VerifyMavenArtifacts.java',
         product, PACKAGE.replace('/', '.') + '.AsRecordMainFixture'])
    cp = build()
    scanned = subprocess.run([jdk('java'), '-cp', cp, 'local.lito381.asrecord.ScanRecordFields', str(product)],
                             capture_output=True, encoding='utf-8', check=True)
    rows = json.loads(scanned.stdout)
    assert len(rows) == 1, rows
    row = rows[0]
    assert row['components'] == [dict(name='id', descriptor='I', signature=None)], row
    assert row['instance_field_count'] == 2, row
    assert len(row['extra_fields']) == 1, row
    extra = row['extra_fields'][0]
    assert (extra['name'], extra['descriptor'], extra['access'], extra['flags']) == ('extra', 'I', 0, []), extra
    assert {a['opcode'] for a in extra['direct_accesses']} == {'GETFIELD', 'PUTFIELD'}, extra
    assert row['direct_access_classes'] == [PACKAGE + '/AsRecordMainFixture$Access'], row
    print('SCAN FIXTURE PASS: components/extra flags/static exclusion/direct field instructions', flush=True)
    # CI 的完全跳過測試模式也應能從乾淨目錄 package。
    run([*base, 'clean', 'package', '-Dmaven.test.skip=true'], cwd=module, env=env)
    run([jdk('java'), '-Xverify:all', TOOL / 'src/test/java/VerifyMavenArtifacts.java',
         product, PACKAGE.replace('/', '.') + '.AsRecordMainFixture'])
    print('MAVEN SELFTEST PASS', flush=True)


if __name__ == '__main__':
    main()
