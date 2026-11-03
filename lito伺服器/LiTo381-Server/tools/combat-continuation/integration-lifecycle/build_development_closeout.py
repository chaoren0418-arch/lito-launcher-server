"""Isolated, deterministic compilation/packaging only. Never invokes runtime tests.

The historical area builder also runs plans; this entry point deliberately has
no plan/verification/launch phase. Historical receipts remain SHA-scoped inputs.
"""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import os
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
SERVER = ROOT.parents[2]
JAVA = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
SEED = ROOT / 'build/source-area-pair-first-builds/20261027T070451268990Z/manifest.json'


def pin(path):
    path = Path(path).resolve()
    return {'path': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}


def checked(ref):
    if pin(ref['path'])['sha256'] != ref['sha256']:
        raise ValueError('Input changed: ' + ref['path'])
    return Path(ref['path'])


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def main():
    if pin(SEED)['sha256'] != '76aeb342518fa875ca286bb8bb418fe1305457fe7548e8b39c2b550457fb89bb':
        raise ValueError('Frozen seed manifest changed')
    seed = json.loads(SEED.read_text('utf-8'))
    base = checked(seed['base'])
    expected = checked(seed['runtime'])
    sources = {Path(ref['path']).name: checked(ref) for ref in seed['inputs']}
    asm = [checked(ref) for ref in seed['asm']]
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    run = ROOT / 'build/development-closeout-builds' / stamp
    inputs = run / 'inputs'
    inputs.mkdir(parents=True)
    for name, path in sources.items():
        shutil.copyfile(path, inputs / name)
    shutil.copyfile(__file__, inputs / Path(__file__).name)
    classes = run / 'classes'
    cp = ';'.join(map(str, [run / 'tools', *asm]))
    env = dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
    commands = []
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args))
            commands.append(args)
            if Path(args[0]).name == 'javac.exe':
                argfile = run / ('javac-' + str(len(commands)) + '.args')
                argfile.write_text('\n'.join(json.dumps(x, ensure_ascii=False) for x in args[1:]), encoding='utf-8')
                args = [args[0], '@' + str(argfile)]
            subprocess.run(args, cwd=SERVER, env=env, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180)
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', cp, '-d', run / 'tools', *[inputs / n for n in seed['tool_sources']]])
        # ASM code generation only: no game entry point, test plan or class init.
        invoke([JAVA / 'java.exe', '-cp', cp, 'SourceAreaPairFirstMethods', 'splice', base, classes, SERVER / 'lib'])
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [classes, base, SERVER / 'lib/*'])), '-d', classes, inputs / 'NativeSourceAreaPairFirst.java'])
    with zipfile.ZipFile(base) as archive:
        entries = {name: archive.read(name) for name in archive.namelist()}
    for path in classes.rglob('*.class'):
        entries[path.relative_to(classes).as_posix()] = path.read_bytes()
    candidate = run / 'candidate-development-unqualified.jar'
    with zipfile.ZipFile(candidate, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(entries.items()):
            archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
    if pin(candidate)['sha256'] != pin(expected)['sha256']:
        raise ValueError('Compile-only reproduction differs from frozen candidate')
    manifest = {
        'status': 'compiled-packaged-byte-identical-no-tests',
        'stage': 'inherited-area-candidate-reproduction',
        'seed_manifest': pin(SEED), 'base': pin(base), 'runtime': pin(candidate),
        'inputs': [pin(p) for p in sorted(inputs.iterdir())],
        'dependencies': [pin(p) for p in asm] + [pin(p) for p in sorted((SERVER / 'lib').glob('*.jar'))],
        'compiler': pin(JAVA / 'javac.exe'), 'commands': commands,
        'log': pin(run / 'compile.log'),
        'tests_executed': False, 'database_connected': False,
        'deployed': False, 'development_closeout_complete': False,
        'full_goal_achieved': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-closeout-build-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
