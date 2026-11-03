"""Compile PC/NPC independent area output and natural entry; no runtime verification."""
from datetime import datetime, timezone
from pathlib import Path
import json
import os
import shutil
import subprocess
import zipfile
from build_development_closeout import ROOT, SERVER, JAVA, SEED, checked, pin, write


def main():
    parent = json.loads((ROOT / 'build/development-area-primary-current.json').read_text('utf-8'))
    base = checked(parent['runtime'])
    checked(parent['manifest'])
    if pin(base)['sha256'] != '0b3d0839b13574b592574452e62deba6926a906afeb987c6209d920d3e411526':
        raise ValueError('Review changed area-primary parent')
    seed = json.loads(SEED.read_text('utf-8'))
    frozen = {Path(x['path']).name: x for x in seed['inputs']}
    asm = [checked(x) for x in seed['asm']]
    run = ROOT / 'build/development-area-output' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'; inputs.mkdir(parents=True)
    sources = [ROOT / 'closeout-src/NativeCloseoutAreaOutput.java', ROOT / 'closeout-src/NativeCloseoutAreaNatural.java', ROOT / 'CloseoutAreaOutputMethods.java',
               ROOT / 'build_closeout_area_output.py', ROOT / 'build_development_closeout.py', ROOT / 'CloseoutAreaOutputAudit.java', checked(frozen['FreezeAcceptedMethods.java'])]
    for p in sources: shutil.copyfile(p, inputs / p.name)
    cp = ';'.join(map(str, [run / 'tools', *asm]))
    classes = run / 'classes'
    commands = []
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args)); commands.append(args)
            subprocess.run(args, cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180,
                           env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'FreezeAcceptedMethods.java', inputs / 'CloseoutAreaOutputMethods.java'])
        invoke([JAVA / 'java.exe', '-cp', cp, 'CloseoutAreaOutputMethods', base, classes, SERVER / 'lib'])
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [classes, base, SERVER / 'lib/*'])), '-d', classes, inputs / 'NativeCloseoutAreaOutput.java', inputs / 'NativeCloseoutAreaNatural.java'])
    with zipfile.ZipFile(base) as archive:
        before = {n: archive.read(n) for n in archive.namelist()}
    entries = dict(before)
    for p in classes.rglob('*.class'): entries[p.relative_to(classes).as_posix()] = p.read_bytes()
    changed = [n for n in entries if before.get(n) != entries[n]]
    expected = ['NativeSourceMassSlowTargets', 'NativeSourceWindTargets', 'NativeSourceWeaponFinalFrame',
                'ClientSession', 'NativeAreaMaterialAction', 'NativeAreaMaterialAction$Context', 'NativeSourceMassSlowTargets$Aim', 'NativeSourceWindTargets$Aim',
                'NativeCloseoutAreaOutput', 'NativeCloseoutAreaOutput$Value', 'NativeCloseoutAreaOutput$Scope', 'NativeCloseoutAreaOutput$Npc', 'NativeCloseoutAreaOutput$Cursor', 'NativeCloseoutAreaNatural']
    if set(changed) != {'local/lito381/server/' + n + '.class' for n in expected}:
        raise ValueError('Unexpected output bytecode delta: ' + repr(changed))
    candidate = run / 'candidate-area-output-unqualified.jar'
    with zipfile.ZipFile(candidate, 'w') as archive:
        for name, data in sorted(entries.items()): archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
    exports = [v for p in ['asm', 'asm.tree', 'asm.util'] for v in ['--add-exports', 'java.base/jdk.internal.org.objectweb.' + p + '=ALL-UNNAMED']]
    with (run / 'compile.log').open('a', encoding='utf-8') as log:
        invoke([JAVA / 'javac.exe', *exports, '-encoding', 'UTF-8', '-d', run / 'tools', inputs / 'CloseoutAreaOutputAudit.java'])
        invoke([JAVA / 'java.exe', *exports, '-cp', run / 'tools', 'CloseoutAreaOutputAudit', base, candidate, run / 'static-method-retention.txt'])
    runtime_dependencies = []
    (run / 'lib').mkdir()
    for name in ['mysql-connector-j-8.0.33.jar', 'protobuf-java-3.21.9.jar', 'HikariCP-5.1.0.jar', 'slf4j-api-1.7.36.jar']:
        source = SERVER / 'lib' / name
        target = run / 'lib' / name
        shutil.copyfile(source, target)
        if pin(source)['sha256'] != pin(target)['sha256']:
            raise ValueError('Runtime dependency copy changed')
        runtime_dependencies.append({'source': pin(source), 'packaged': pin(target)})
    manifest = {
        'status': 'independent-PC-NPC-area-output-and-early-release-implemented-compiled-tests-deferred',
        'runtime': pin(candidate), 'parent': parent,
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'dependencies': [pin(p) for p in asm],
        'runtime_dependencies': runtime_dependencies,
        'static_method_retention': pin(run / 'static-method-retention.txt'),
        'changed_entries': changed, 'commands': commands, 'log': pin(run / 'compile.log'),
        'scope': '76/167 PC/controlled/natural-primary flow freezes PC/NPC participants before roster waits; seven original calculation iterators filter stale first identities before RNG. Every output uses its actual recipient/addressed entity and complete existing cipher sender. An unrelated retired primary is not a guard on a valid accepted secondary output. Queued Value retains no Aim, Context, Scope or full roster. Complete original producer siblings release Aim and all companion receipts before deferred IO. Natural entry reuses its first Context and original admission/cost body.',
        'remaining': ['NPC/status mutation guards are built by the following area-commit and area-player-commit stages', 'same-SHA runtime validation'],
        'tests_executed': False, 'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-area-output-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__': main()
