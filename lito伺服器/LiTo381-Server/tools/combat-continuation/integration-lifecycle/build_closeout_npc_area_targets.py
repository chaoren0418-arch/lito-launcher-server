"""Compile Source NPC area target selection and pre-calculation guards. No tests."""
from datetime import datetime, timezone
import json
import os
import shutil
import subprocess
import zipfile
from pathlib import Path
from build_development_closeout import ROOT, SERVER, JAVA, SEED, checked, pin, write


def main():
    parent = json.loads((ROOT / 'build/development-battle-line-of-sight-current.json').read_text('utf-8'))
    base = checked(parent['runtime']); checked(parent['manifest'])
    if pin(base)['sha256'] != 'f6ea5099231a8b14cf5b964b4eb02284ea875f2ed35d42585c5707bc0d955399':
        raise ValueError('Review changed battle-line-of-sight parent')
    seed = json.loads(SEED.read_text('utf-8'))
    frozen = {Path(x['path']).name: x for x in seed['inputs']}
    asm = [checked(x) for x in seed['asm']]
    run = ROOT / 'build/development-npc-area-targets' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'; inputs.mkdir(parents=True)
    for source in [ROOT / 'closeout-src/NativeCloseoutNpcAreaTargets.java', ROOT / 'CloseoutNpcAreaTargetsMethods.java',
                   ROOT / 'CloseoutNpcAreaTargetsAudit.java', ROOT / 'CloseoutAreaOutputAudit.java',
                   ROOT / 'build_closeout_npc_area_targets.py', ROOT / 'build_development_closeout.py', checked(frozen['FreezeAcceptedMethods.java'])]:
        shutil.copyfile(source, inputs / source.name)
    cp = ';'.join(map(str, [run / 'tools', *asm])); classes = run / 'classes'; commands = []
    source_refs = []
    for relative in ['model/L1Character.java', 'model/map/L1V1Map.java', 'datatables/MapsTable.java', 'model/L1MobSkillUse.java']:
        original = Path('D:/lineage開發/1-伺服器/src/com/lineage/server') / relative
        copied = inputs / original.name; shutil.copyfile(original, copied)
        source_refs.append({'original': pin(original), 'frozen': pin(copied)})
    exports = [v for p in ['asm', 'asm.tree', 'asm.util'] for v in ['--add-exports', 'java.base/jdk.internal.org.objectweb.' + p + '=ALL-UNNAMED']]
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args)); commands.append(args)
            subprocess.run(args, cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180,
                           env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'FreezeAcceptedMethods.java', inputs / 'CloseoutNpcAreaTargetsMethods.java'])
        invoke([JAVA / 'java.exe', '-cp', cp, 'CloseoutNpcAreaTargetsMethods', base, classes, SERVER / 'lib'])
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [classes, base, SERVER / 'lib/*'])), '-d', classes, inputs / 'NativeCloseoutNpcAreaTargets.java'])
        with zipfile.ZipFile(base) as archive:
            before = {n: archive.read(n) for n in archive.namelist()}
        entries = dict(before)
        for p in classes.rglob('*.class'): entries[p.relative_to(classes).as_posix()] = p.read_bytes()
        changed = [n for n in entries if before.get(n) != entries[n]]
        expected = ['ClientSession', 'NativeCloseoutNpcAreaTargets', 'NativeCloseoutNpcAreaTargets$Selection']
        if set(changed) != {'local/lito381/server/' + n + '.class' for n in expected}:
            raise ValueError('Unexpected mutation guard delta: ' + repr(changed))
        candidate = run / 'candidate-npc-area-targets-unqualified.jar'
        with zipfile.ZipFile(candidate, 'w') as archive:
            for name, data in sorted(entries.items()): archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
        invoke([JAVA / 'javac.exe', *exports, '-encoding', 'UTF-8', '-d', run / 'tools', inputs / 'CloseoutAreaOutputAudit.java', inputs / 'CloseoutNpcAreaTargetsAudit.java'])
        invoke([JAVA / 'java.exe', *exports, '-cp', run / 'tools', 'CloseoutNpcAreaTargetsAudit', base, candidate, run / 'static-method-retention.txt'])
    parent_manifest = json.loads(checked(parent['manifest']).read_text('utf-8'))
    dependencies = []; (run / 'lib').mkdir()
    for item in parent_manifest['runtime_dependencies']:
        source = checked(item['packaged']); target = run / 'lib' / source.name; shutil.copyfile(source, target)
        if pin(source)['sha256'] != pin(target)['sha256']: raise ValueError('Runtime dependency copy mismatch')
        dependencies.append({'source': pin(source), 'packaged': pin(target)})
    manifest = {
        'status': 'npc-area-targets-compiled-static-audit-only', 'runtime': pin(candidate), 'parent': parent,
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'dependencies': [pin(p) for p in asm],
        'runtime_dependencies': dependencies, 'commands': commands, 'log': pin(run / 'compile.log'),
        'Source': source_refs,
        'static_method_retention': pin(run / 'static-method-retention.txt'), 'changed_entries': changed,
        'scope': 'Two Quest/Time magic damage ingress methods branch on birth-frozen positive Source mobskill width/height before original directional/radius selection. Room-member order is preserved; only first-ingress members on the actual NPC map and Source screen/static LOS are selected. The original NPC view, owner/generation/factory/life/position and original player role/life/position are rechecked before scalar capture and RNG. Ordinary invisibility is permitted. No new actor lookup authority, RNG, mutation, SQL or scheduling is introduced. Nonarea and missing legacy factory metadata retain the original body. Known Source area missing first proof rejects. All original ClientSession methods are inverse-compared and all other existing archive entries byte-retained.',
        'remaining': ['Area TYPE_GMBUFF execution as each visible player casting on self and success-only action/scheduling; visual/ground area ingress', 'Dynamic BATTLE door/arrow overlays; explicit GM-invisibility/ghost adapter and exact Source spatial iteration order', 'Full controlled NPC selection/payment/spell dispatch and WORLD effect binding', 'Remaining NPC target/admission/RNG and no-target AI cases, pet equipment SP, De and per-skill/control/data consumers; runtime validation separately authorized'],
        'tests_executed': False, 'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-npc-area-targets-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__': main()
