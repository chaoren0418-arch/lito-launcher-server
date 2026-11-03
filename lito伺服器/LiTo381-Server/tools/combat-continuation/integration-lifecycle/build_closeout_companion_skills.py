"""Compile actor-owned companion mobskill state and statically compare original methods. No tests."""
from datetime import datetime, timezone
import json
import os
import shutil
import subprocess
import zipfile
from pathlib import Path
from build_development_closeout import ROOT, SERVER, JAVA, SEED, checked, pin, write


def main():
    parent = json.loads((ROOT / 'build/development-controlled-npc-data-current.json').read_text('utf-8'))
    base = checked(parent['runtime']); checked(parent['manifest'])
    if pin(base)['sha256'] != '1c8ba90c38c2cf7f4d28d2d8883ad8263d2b3c794dcbc608da41e37b167f2a51':
        raise ValueError('Review changed controlled-npc-data parent')
    seed = json.loads(SEED.read_text('utf-8'))
    frozen = {Path(x['path']).name: x for x in seed['inputs']}
    asm = [checked(x) for x in seed['asm']]
    run = ROOT / 'build/development-companion-skills' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'; inputs.mkdir(parents=True)
    for source in [ROOT / 'closeout-src/NativeCloseoutCompanionSkills.java', ROOT / 'CloseoutCompanionSkillsMethods.java',
                   ROOT / 'CloseoutCompanionSkillsAudit.java', ROOT / 'CloseoutAreaOutputAudit.java',
                   ROOT / 'build_closeout_companion_skills.py', ROOT / 'build_development_closeout.py', checked(frozen['FreezeAcceptedMethods.java'])]:
        shutil.copyfile(source, inputs / source.name)
    cp = ';'.join(map(str, [run / 'tools', *asm])); classes = run / 'classes'; commands = []
    source_refs = []
    for relative in ['model/L1MobSkillUse.java', 'model/skill/L1SkillDelayforMob.java', 'model/Instance/NpcAI.java', 'model/Instance/L1NpcInstance.java', 'model/Instance/L1PetInstance.java', 'model/Instance/L1SummonInstance.java']:
        original = Path('D:/lineage開發/1-伺服器/src/com/lineage/server') / relative
        copied = inputs / original.name; shutil.copyfile(original, copied)
        source_refs.append({'original': pin(original), 'frozen': pin(copied)})
    exports = [v for p in ['asm', 'asm.tree', 'asm.util'] for v in ['--add-exports', 'java.base/jdk.internal.org.objectweb.' + p + '=ALL-UNNAMED']]
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args)); commands.append(args)
            subprocess.run(args, cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180,
                           env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'FreezeAcceptedMethods.java', inputs / 'CloseoutCompanionSkillsMethods.java'])
        invoke([JAVA / 'java.exe', '-cp', cp, 'CloseoutCompanionSkillsMethods', base, classes, SERVER / 'lib'])
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [classes, base, SERVER / 'lib/*'])), '-d', classes, inputs / 'NativeCloseoutCompanionSkills.java'])
        with zipfile.ZipFile(base) as archive:
            before = {n: archive.read(n) for n in archive.namelist()}
        entries = dict(before)
        for p in classes.rglob('*.class'): entries[p.relative_to(classes).as_posix()] = p.read_bytes()
        changed = [n for n in entries if before.get(n) != entries[n]]
        expected = ['BattleCompanion', 'ClientSession', 'NativeCloseoutCompanionSkills', 'NativeCloseoutCompanionSkills$State', 'NativeCloseoutCompanionSkills$Lease']
        if set(changed) != {'local/lito381/server/' + n + '.class' for n in expected}:
            raise ValueError('Unexpected mutation guard delta: ' + repr(changed))
        candidate = run / 'candidate-companion-skills-unqualified.jar'
        with zipfile.ZipFile(candidate, 'w') as archive:
            for name, data in sorted(entries.items()): archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
        invoke([JAVA / 'javac.exe', *exports, '-encoding', 'UTF-8', '-d', run / 'tools', inputs / 'CloseoutAreaOutputAudit.java', inputs / 'CloseoutCompanionSkillsAudit.java'])
        invoke([JAVA / 'java.exe', *exports, '-cp', run / 'tools', 'CloseoutCompanionSkillsAudit', base, candidate, run / 'static-method-retention.txt'])
    parent_manifest = json.loads(checked(parent['manifest']).read_text('utf-8'))
    dependencies = []; (run / 'lib').mkdir()
    for item in parent_manifest['runtime_dependencies']:
        source = checked(item['packaged']); target = run / 'lib' / source.name; shutil.copyfile(source, target)
        if pin(source)['sha256'] != pin(target)['sha256']: raise ValueError('Runtime dependency copy mismatch')
        dependencies.append({'source': pin(source), 'packaged': pin(target)})
    manifest = {
        'status': 'companion-mobskill-state-lifecycle-compiled-static-audit-only', 'runtime': pin(candidate), 'parent': parent,
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'dependencies': [pin(p) for p in asm],
        'runtime_dependencies': dependencies, 'commands': commands, 'log': pin(run / 'compile.log'),
        'Source': source_refs,
        'static_method_retention': pin(run / 'static-method-retention.txt'), 'changed_entries': changed,
        'scope': 'Actual pet/summon actors own mobskill use counts and reuse deadlines. Constructor and accepted evolution install state from the frozen factory data; terminal HP/active writes and existing BATTLE/WORLD resting ticks reset use counts without clearing cooldowns. Actor/factory/life/generation leases require original actor/status monitors; a finished AI or evolution rejects old leases. No target/owner registry, scheduler, RNG or packet output is added. Ordinary target changes retain state. Selection/effect adapters are still open; this stage does not enable companion magic or claim full Source no-target AI completion.',
        'remaining': ['Bind Source mobskill selection and actual actor/visibility/effect ports under existing admission locks', 'Map remaining no-target AI finish cases (extend/recall/map restrictions) before full Source AI equivalence', 'Pet equipment SP, De actor/stat consumers and remaining skill/control/data consumers', 'Separately authorized runtime validation'],
        'tests_executed': False, 'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-companion-skills-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__': main()
