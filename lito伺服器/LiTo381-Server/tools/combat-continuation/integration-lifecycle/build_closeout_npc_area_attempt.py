"""Compile synchronous no-effect NPC area casts before original scheduling. No tests."""
from datetime import datetime, timezone
import json
import os
import shutil
import subprocess
import zipfile
from pathlib import Path
from build_development_closeout import ROOT, SERVER, JAVA, SEED, checked, pin, write


def main():
    parent = json.loads((ROOT / 'build/development-npc-area-output-current.json').read_text('utf-8'))
    base = checked(parent['runtime']); checked(parent['manifest'])
    if pin(base)['sha256'] != 'dbe13804e6a9ff64c6837c5cd84df6c1251b20b0922a397d86d6113a1c07da66':
        raise ValueError('Review changed npc-area-output parent')
    seed = json.loads(SEED.read_text('utf-8'))
    frozen = {Path(x['path']).name: x for x in seed['inputs']}
    asm = [checked(x) for x in seed['asm']]
    run = ROOT / 'build/development-npc-area-attempt' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'; inputs.mkdir(parents=True)
    for source in [ROOT / 'closeout-src/NativeCloseoutNpcAreaAttempt.java', ROOT / 'CloseoutNpcAreaAttemptMethods.java',
                   ROOT / 'CloseoutNpcAreaAttemptAudit.java', ROOT / 'CloseoutAreaOutputAudit.java',
                   ROOT / 'build_closeout_npc_area_attempt.py', ROOT / 'build_development_closeout.py', checked(frozen['FreezeAcceptedMethods.java'])]:
        shutil.copyfile(source, inputs / source.name)
    cp = ';'.join(map(str, [run / 'tools', *asm])); classes = run / 'classes'; commands = []
    source_refs = []
    for relative in ['ActionCodes.java', 'model/L1MobSkillUse.java', 'model/skill/L1SkillUse.java', 'model/Instance/L1NpcInstance.java']:
        original = Path('D:/lineage開發/1-伺服器/src/com/lineage/server') / relative
        copied = inputs / original.name; shutil.copyfile(original, copied)
        source_refs.append({'original': pin(original), 'frozen': pin(copied)})
    if 'ACTION_SkillBuff = 19;' not in (inputs / 'ActionCodes.java').read_text('utf-8-sig'):
        raise ValueError('Review changed Source auxiliary action constant')
    exports = [v for p in ['asm', 'asm.tree', 'asm.util'] for v in ['--add-exports', 'java.base/jdk.internal.org.objectweb.' + p + '=ALL-UNNAMED']]
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args)); commands.append(args)
            subprocess.run(args, cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180,
                           env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'FreezeAcceptedMethods.java', inputs / 'CloseoutNpcAreaAttemptMethods.java'])
        invoke([JAVA / 'java.exe', '-cp', cp, 'CloseoutNpcAreaAttemptMethods', base, classes, SERVER / 'lib'])
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [classes, base, SERVER / 'lib/*'])), '-d', classes, inputs / 'NativeCloseoutNpcAreaAttempt.java'])
        with zipfile.ZipFile(base) as archive:
            before = {n: archive.read(n) for n in archive.namelist()}
        entries = dict(before)
        for p in classes.rglob('*.class'): entries[p.relative_to(classes).as_posix()] = p.read_bytes()
        changed = [n for n in entries if before.get(n) != entries[n]]
        expected = ['GameHub', 'GameHub$MonsterAiTick', 'NativeCloseoutNpcAreaScene', 'ClientSession', 'NativeCloseoutNpcAreaDoors', 'NativeCloseoutNpcAreaAttempt', 'NativeCloseoutNpcAreaAttempt$Context', 'NativeCloseoutNpcAreaAttempt$Receipt']
        if set(changed) != {'local/lito381/server/' + n + '.class' for n in expected}:
            raise ValueError('Unexpected mutation guard delta: ' + repr(changed))
        candidate = run / 'candidate-npc-area-attempt-unqualified.jar'
        with zipfile.ZipFile(candidate, 'w') as archive:
            for name, data in sorted(entries.items()): archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
        invoke([JAVA / 'javac.exe', *exports, '-encoding', 'UTF-8', '-d', run / 'tools', inputs / 'CloseoutAreaOutputAudit.java', inputs / 'CloseoutNpcAreaAttemptAudit.java'])
        invoke([JAVA / 'java.exe', *exports, '-cp', run / 'tools', 'CloseoutNpcAreaAttemptAudit', base, candidate, run / 'static-method-retention.txt'])
    parent_manifest = json.loads(checked(parent['manifest']).read_text('utf-8'))
    dependencies = []; (run / 'lib').mkdir()
    for item in parent_manifest['runtime_dependencies']:
        source = checked(item['packaged']); target = run / 'lib' / source.name; shutil.copyfile(source, target)
        if pin(source)['sha256'] != pin(target)['sha256']: raise ValueError('Runtime dependency copy mismatch')
        dependencies.append({'source': pin(source), 'packaged': pin(target)})
    manifest = {
        'status': 'npc-area-synchronous-empty-cast-compiled-static-audit-only', 'runtime': pin(candidate), 'parent': parent,
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'dependencies': [pin(p) for p in asm],
        'runtime_dependencies': dependencies, 'commands': commands, 'log': pin(run / 'compile.log'),
        'Source': source_refs,
        'static_method_retention': pin(run / 'static-method-retention.txt'), 'changed_entries': changed,
        'scope': 'The proven empty-self-target TYPE_GMBUFF Definition branch completes its no-effect casts synchronously inside the original Hub/encounter invocation. Original first NPC and player views, actual map, visible-screen and room door ray checks precede success receipts. No successes returns through the existing same-attempt physical fallback before any magic schedule/reuse writes. Success retains the original zero-cost schedule and Source reuse body; the existing tick carries completed receipts and the completed output is delivered before the original primary-player rejection gate and never re-enters the damage loop. Original NPC, lifetime, room and viewer proofs remain required even if the unrelated original primary retires. Atomic claim and output suppression prevent replay; the original success-only action/cipher path is retained. There is no actor pending lease, new scheduler, RNG, SQL or output under the AI locks. Natural mobskill use-count semantics remain unimplemented; visual/ground and nonempty self-cast branches remain open.',
        'remaining': ['Natural mobskill use-count/finish-AI semantics and broader Source selection behavior', 'Nonempty TYPE_GMBUFF self execution, statuses/graphics and visual/ground lanes', 'Other dynamic-door consumers, map-specific arrow overrides, explicit GM-invisibility/ghost adapter and exact Source spatial iteration order', 'Controlled NPC spell dispatch and WORLD effects; per-skill/control/data consumers; runtime validation separately authorized'],
        'tests_executed': False, 'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-npc-area-attempt-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__': main()
