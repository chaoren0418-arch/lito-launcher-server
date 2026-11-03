"""Compile per-evaluation Source NPC area door overlays. No tests."""
from datetime import datetime, timezone
import json
import os
import shutil
import subprocess
import zipfile
from pathlib import Path
from build_development_closeout import ROOT, SERVER, JAVA, SEED, checked, pin, write


def main():
    parent = json.loads((ROOT / 'build/development-npc-area-targets-current.json').read_text('utf-8'))
    base = checked(parent['runtime']); checked(parent['manifest'])
    if pin(base)['sha256'] != '010dd9d332adc00aacf5f4e9febb17c7db05eadfa485fcb9eec02ffd651d0c55':
        raise ValueError('Review changed npc-area-targets parent')
    seed = json.loads(SEED.read_text('utf-8'))
    frozen = {Path(x['path']).name: x for x in seed['inputs']}
    asm = [checked(x) for x in seed['asm']]
    run = ROOT / 'build/development-npc-area-doors' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'; inputs.mkdir(parents=True)
    for source in [ROOT / 'closeout-src/NativeCloseoutNpcAreaDoors.java', ROOT / 'CloseoutNpcAreaDoorsMethods.java',
                   ROOT / 'CloseoutNpcAreaDoorsAudit.java', ROOT / 'CloseoutAreaOutputAudit.java',
                   ROOT / 'build_closeout_npc_area_doors.py', ROOT / 'build_development_closeout.py', checked(frozen['FreezeAcceptedMethods.java'])]:
        shutil.copyfile(source, inputs / source.name)
    cp = ';'.join(map(str, [run / 'tools', *asm])); classes = run / 'classes'; commands = []
    source_refs = []
    for relative in ['model/L1Character.java', 'model/map/L1V1Map.java', 'datatables/MapsTable.java', 'model/L1MobSkillUse.java', 'model/Instance/L1DoorInstance.java']:
        original = Path('D:/lineage開發/1-伺服器/src/com/lineage/server') / relative
        copied = inputs / original.name; shutil.copyfile(original, copied)
        source_refs.append({'original': pin(original), 'frozen': pin(copied)})
    exports = [v for p in ['asm', 'asm.tree', 'asm.util'] for v in ['--add-exports', 'java.base/jdk.internal.org.objectweb.' + p + '=ALL-UNNAMED']]
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args)); commands.append(args)
            subprocess.run(args, cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180,
                           env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'FreezeAcceptedMethods.java', inputs / 'CloseoutNpcAreaDoorsMethods.java'])
        invoke([JAVA / 'java.exe', '-cp', cp, 'CloseoutNpcAreaDoorsMethods', base, classes, SERVER / 'lib'])
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [classes, base, SERVER / 'lib/*'])), '-d', classes, inputs / 'NativeCloseoutNpcAreaDoors.java'])
        with zipfile.ZipFile(base) as archive:
            before = {n: archive.read(n) for n in archive.namelist()}
        entries = dict(before)
        for p in classes.rglob('*.class'): entries[p.relative_to(classes).as_posix()] = p.read_bytes()
        changed = [n for n in entries if before.get(n) != entries[n]]
        expected = ['NativeCloseoutNpcAreaTargets', 'NativeCloseoutNpcAreaDoors']
        if set(changed) != {'local/lito381/server/' + n + '.class' for n in expected}:
            raise ValueError('Unexpected mutation guard delta: ' + repr(changed))
        candidate = run / 'candidate-npc-area-doors-unqualified.jar'
        with zipfile.ZipFile(candidate, 'w') as archive:
            for name, data in sorted(entries.items()): archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
        invoke([JAVA / 'javac.exe', *exports, '-encoding', 'UTF-8', '-d', run / 'tools', inputs / 'CloseoutAreaOutputAudit.java', inputs / 'CloseoutNpcAreaDoorsAudit.java'])
        invoke([JAVA / 'java.exe', *exports, '-cp', run / 'tools', 'CloseoutNpcAreaDoorsAudit', base, candidate, run / 'static-method-retention.txt'])
    parent_manifest = json.loads(checked(parent['manifest']).read_text('utf-8'))
    dependencies = []; (run / 'lib').mkdir()
    for item in parent_manifest['runtime_dependencies']:
        source = checked(item['packaged']); target = run / 'lib' / source.name; shutil.copyfile(source, target)
        if pin(source)['sha256'] != pin(target)['sha256']: raise ValueError('Runtime dependency copy mismatch')
        dependencies.append({'source': pin(source), 'packaged': pin(target)})
    manifest = {
        'status': 'npc-area-doors-compiled-static-audit-only', 'runtime': pin(candidate), 'parent': parent,
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'dependencies': [pin(p) for p in asm],
        'runtime_dependencies': dependencies, 'commands': commands, 'log': pin(run / 'compile.log'),
        'Source': source_refs,
        'static_method_retention': pin(run / 'static-method-retention.txt'), 'changed_entries': changed,
        'scope': 'The existing NPC area selection and per-target pre-RNG guard now pass their original Selection proof to a room-aware LOS helper. The helper initializes the existing readonly cache without vetoing on an unoverlaid ray, captures current room door/gate snapshots under Hub lock and applies Source NativeDeTerrainV1.setPassable on a fresh short-lived terrain layer. Original readonly arrays and scalar bounds are shared; the door map is owned by the new layer, never the arena cache. Closed doors use original direction-dependent center/side-cell semantics. Open and inactive doors restore original terrain rather than force arrow passage. Only the original room base map receives its doors. Room identity, start/map and door values are checked again after the ray. The exact Source LOS and terrain bodies, original ClientSession and every other existing archive entry remain byte-identical. Unloaded Source terrain retains existing unqualified legacy LOS. No new SQL, RNG, threads, actor writes or retained room state.',
        'remaining': ['Area TYPE_GMBUFF execution as each visible player casting on self and success-only action/scheduling; visual/ground area ingress', 'Dynamic door adapters for other shared BATTLE LOS consumers and map-specific arrow overrides; explicit GM-invisibility/ghost adapter and exact Source spatial iteration order', 'Full controlled NPC selection/payment/spell dispatch and WORLD effect binding', 'Remaining NPC target/admission/RNG and no-target AI cases, pet equipment SP, De and per-skill/control/data consumers; runtime validation separately authorized'],
        'tests_executed': False, 'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-npc-area-doors-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__': main()
