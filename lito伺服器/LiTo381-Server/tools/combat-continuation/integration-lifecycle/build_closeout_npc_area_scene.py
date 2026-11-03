"""Compile Source area-magic actual-map rejection before original scheduling. Static inspection only. No tests."""
from datetime import datetime, timezone
import json
import os
import shutil
import subprocess
import zipfile
from pathlib import Path
from build_development_closeout import ROOT, SERVER, JAVA, SEED, checked, pin, write


def main():
    parent = json.loads((ROOT / 'build/development-npc-area-cost-current.json').read_text('utf-8'))
    base = checked(parent['runtime']); checked(parent['manifest'])
    if pin(base)['sha256'] != '1e411e4fa9bc9a4f42c8e9be3f1780592e9465abb093b75d60a5886b5bf5e147':
        raise ValueError('Review changed npc-area-cost parent')
    seed = json.loads(SEED.read_text('utf-8'))
    frozen = {Path(x['path']).name: x for x in seed['inputs']}
    asm = [checked(x) for x in seed['asm']]
    run = ROOT / 'build/development-npc-area-scene' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'; inputs.mkdir(parents=True)
    for source in [ROOT / 'closeout-src/NativeCloseoutNpcAreaScene.java', ROOT / 'CloseoutNpcAreaSceneMethods.java',
                   ROOT / 'CloseoutNpcAreaSceneAudit.java', ROOT / 'CloseoutAreaOutputAudit.java',
                   ROOT / 'build_closeout_npc_area_scene.py', ROOT / 'build_development_closeout.py', checked(frozen['FreezeAcceptedMethods.java'])]:
        shutil.copyfile(source, inputs / source.name)
    cp = ';'.join(map(str, [run / 'tools', *asm])); classes = run / 'classes'; commands = []
    source_refs = []
    for relative in ['model/L1MobSkillUse.java', 'model/skill/L1SkillUse.java', 'model/L1MagicNpc.java', 'model/Instance/L1PetInstance.java', 'model/Instance/L1SummonInstance.java']:
        original = Path('D:/lineage開發/1-伺服器/src/com/lineage/server') / relative
        copied = inputs / original.name; shutil.copyfile(original, copied)
        source_refs.append({'original': pin(original), 'frozen': pin(copied)})
    exports = [v for p in ['asm', 'asm.tree', 'asm.util'] for v in ['--add-exports', 'java.base/jdk.internal.org.objectweb.' + p + '=ALL-UNNAMED']]
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args)); commands.append(args)
            subprocess.run(args, cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180,
                           env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'FreezeAcceptedMethods.java', inputs / 'CloseoutNpcAreaSceneMethods.java'])
        invoke([JAVA / 'java.exe', '-cp', cp, 'CloseoutNpcAreaSceneMethods', base, classes, SERVER / 'lib'])
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [classes, base, SERVER / 'lib/*'])), '-d', classes, inputs / 'NativeCloseoutNpcAreaScene.java'])
        with zipfile.ZipFile(base) as archive:
            before = {n: archive.read(n) for n in archive.namelist()}
        entries = dict(before)
        for p in classes.rglob('*.class'): entries[p.relative_to(classes).as_posix()] = p.read_bytes()
        changed = [n for n in entries if before.get(n) != entries[n]]
        expected = ['BattleQuestEncounter', 'BattleTimeAttackEncounter', 'NativeCloseoutNpcAreaScene']
        if set(changed) != {'local/lito381/server/' + n + '.class' for n in expected}:
            raise ValueError('Unexpected mutation guard delta: ' + repr(changed))
        candidate = run / 'candidate-npc-area-scene-unqualified.jar'
        with zipfile.ZipFile(candidate, 'w') as archive:
            for name, data in sorted(entries.items()): archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
        invoke([JAVA / 'javac.exe', *exports, '-encoding', 'UTF-8', '-d', run / 'tools', inputs / 'CloseoutAreaOutputAudit.java', inputs / 'CloseoutNpcAreaSceneAudit.java'])
        invoke([JAVA / 'java.exe', *exports, '-cp', run / 'tools', 'CloseoutNpcAreaSceneAudit', base, candidate, run / 'static-method-retention.txt'])
    parent_manifest = json.loads(checked(parent['manifest']).read_text('utf-8'))
    dependencies = []; (run / 'lib').mkdir()
    for item in parent_manifest['runtime_dependencies']:
        source = checked(item['packaged']); target = run / 'lib' / source.name; shutil.copyfile(source, target)
        if pin(source)['sha256'] != pin(target)['sha256']: raise ValueError('Runtime dependency copy mismatch')
        dependencies.append({'source': pin(source), 'packaged': pin(target)})
    manifest = {
        'status': 'npc-area-actual-map-admission-compiled-static-audit-only', 'runtime': pin(candidate), 'parent': parent,
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'dependencies': [pin(p) for p in asm],
        'runtime_dependencies': dependencies, 'commands': commands, 'log': pin(run / 'compile.log'),
        'Source': source_refs,
        'static_method_retention': pin(run / 'static-method-retention.txt'), 'changed_entries': changed,
        'scope': 'Four selected Quest/Time Attack spell attempts now carry the actual encounter while its original monitor is held. Only Source positive width/height area rows check the actual NPC arena: Quest uses the existing actor force index, Time Attack the owning arena. Map93 or missing authoritative arena rejects the selected area attempt before the original admission, payment, spell scheduling, reuse write or attack construction; original physical fallback remains. Ordinary and legacy rows delegate unchanged. All original encounter bodies inverse-compare and all other existing archive entries are byte-retained. No new locks, actor registry, RNG, SQL or output. This implements map rejection only; actual PC-self TYPE_GMBUFF, visible-player LOS and success-only scheduling remain open.',
        'remaining': ['Area TYPE_GMBUFF execution as each visible player casting on self, visibility/LOS and success-only action/scheduling', 'Full controlled NPC selection/payment/spell dispatch and WORLD effect binding', 'Remaining NPC target/admission/RNG consumers and no-target AI finish cases', 'Pet equipment SP, De and remaining per-skill/control/data consumers; separately authorized runtime validation'],
        'tests_executed': False, 'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-npc-area-scene-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__': main()
