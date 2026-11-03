"""Compile isolated PC/natural NPC primary extension; no runtime tests or old pointer updates."""
from datetime import datetime, timezone
import json
import os
import shutil
import subprocess
import zipfile
from build_development_closeout import ROOT, SERVER, JAVA, checked, pin, write


def main():
    parent = json.loads((ROOT / 'build/development-release-compat-current.json').read_text('utf-8'))
    base = checked(parent['runtime'])
    checked(parent['manifest'])
    if pin(base)['sha256'] != '85fafef4c65ef90331885b73ab86fbe1bf819804f12360408d1c7a9bec53e60e':
        raise ValueError('Review changed release-compatible parent')
    run = ROOT / 'build/development-area-primary' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'
    inputs.mkdir(parents=True)
    source = ROOT / 'closeout-src/NativeSourceAreaPairFirst.java'
    for p in [source, ROOT / 'build_closeout_area_primary.py', ROOT / 'build_development_closeout.py']:
        shutil.copyfile(p, inputs / p.name)
    command = [JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [base, SERVER / 'lib/*'])), '-d', run / 'classes', inputs / source.name]
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        subprocess.run(list(map(str, command)), cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180,
                       env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
    with zipfile.ZipFile(base) as archive:
        before = {n: archive.read(n) for n in archive.namelist()}
    entries = dict(before)
    for p in (run / 'classes').rglob('*.class'):
        entries[p.relative_to(run / 'classes').as_posix()] = p.read_bytes()
    changed = [n for n in entries if before.get(n) != entries[n]]
    if changed != ['local/lito381/server/NativeSourceAreaPairFirst.class']:
        raise ValueError('Unexpected area primary bytecode delta: ' + repr(changed))
    candidate = run / 'candidate-area-primary-unqualified.jar'
    with zipfile.ZipFile(candidate, 'w') as archive:
        for name, data in sorted(entries.items()):
            archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
    manifest = {
        'status': 'PC-natural-primary-first-identity-implemented-compiled-tests-deferred',
        'runtime': pin(candidate), 'parent': parent,
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'changed_entries': changed,
        'command': list(map(str, command)), 'log': pin(run / 'compile.log'),
        'scope': '76/167 existing PC Aim and natural NPC first Context freeze original primary status/life/binding before original roster wait; same Context field, original controlled checks, geometry, costs and output paths retained.',
        'remaining': ['natural dispatcher and independent output seams are built by the next area-output stage', 'full per-skill semantics and runtime verification'],
        'tests_executed': False, 'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-area-primary-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
