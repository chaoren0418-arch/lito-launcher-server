"""Compile complete controlled NPC birth facts from one readonly snapshot and statically compare original bytecode. No tests."""
from datetime import datetime, timezone
import json
import os
import shutil
import subprocess
import zipfile
from pathlib import Path
from build_development_closeout import ROOT, SERVER, JAVA, SEED, checked, pin, write


def main():
    parent = json.loads((ROOT / 'build/development-companion-evolution-current.json').read_text('utf-8'))
    base = checked(parent['runtime']); checked(parent['manifest'])
    if pin(base)['sha256'] != '88b4f390a641140de4f1b1136a757db4f6267f951169ca909707cd35763384f1':
        raise ValueError('Review changed companion-evolution parent')
    seed = json.loads(SEED.read_text('utf-8'))
    frozen = {Path(x['path']).name: x for x in seed['inputs']}
    asm = [checked(x) for x in seed['asm']]
    run = ROOT / 'build/development-controlled-npc-data' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'; inputs.mkdir(parents=True)
    for source in [ROOT / 'closeout-src/NativeCloseoutControlledNpcData.java', ROOT / 'closeout-src/NativeCloseoutMobSkillSelection.java', ROOT / 'CloseoutControlledNpcDataMethods.java',
                   ROOT / 'CloseoutControlledNpcDataAudit.java', ROOT / 'CloseoutAreaOutputAudit.java',
                   ROOT / 'build_closeout_controlled_npc_data.py', ROOT / 'build_development_closeout.py', checked(frozen['FreezeAcceptedMethods.java'])]:
        shutil.copyfile(source, inputs / source.name)
    cp = ';'.join(map(str, [run / 'tools', *asm])); classes = run / 'classes'; commands = []
    source_refs = []
    for relative in ['timecontroller/pet/HprPet.java', 'timecontroller/pet/MprPet.java', 'model/L1MobSkillUse.java', 'templates/L1MobSkill.java', 'model/Instance/L1NpcInstance.java']:
        original = Path('D:/lineage開發/1-伺服器/src/com/lineage/server') / relative
        copied = inputs / original.name; shutil.copyfile(original, copied)
        source_refs.append({'original': pin(original), 'frozen': pin(copied)})
    exports = [v for p in ['asm', 'asm.tree', 'asm.util'] for v in ['--add-exports', 'java.base/jdk.internal.org.objectweb.' + p + '=ALL-UNNAMED']]
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args)); commands.append(args)
            subprocess.run(args, cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180,
                           env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'FreezeAcceptedMethods.java', inputs / 'CloseoutControlledNpcDataMethods.java'])
        invoke([JAVA / 'java.exe', '-cp', cp, 'CloseoutControlledNpcDataMethods', base, classes, SERVER / 'lib'])
        invoke([JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', ';'.join(map(str, [classes, base, SERVER / 'lib/*'])), '-d', classes, inputs / 'NativeCloseoutControlledNpcData.java', inputs / 'NativeCloseoutMobSkillSelection.java'])
        with zipfile.ZipFile(base) as archive:
            before = {n: archive.read(n) for n in archive.namelist()}
        entries = dict(before)
        for p in classes.rglob('*.class'): entries[p.relative_to(classes).as_posix()] = p.read_bytes()
        changed = [n for n in entries if before.get(n) != entries[n]]
        expected = ['NativeSourceNpcFactoryType', 'NativeSourceNpcFactoryType$Stamp', 'NativeCloseoutControlledNpcData', 'NativeCloseoutControlledNpcData$Birth', 'NativeCloseoutControlledNpcData$Data', 'NativeCloseoutControlledNpcData$Row', 'NativeCloseoutMobSkillSelection', 'NativeCloseoutMobSkillSelection$Ports', 'NativeCloseoutMobSkillSelection$Choice', 'NativeCloseoutMobSkillSelection$Scan']
        if set(changed) != {'local/lito381/server/' + n + '.class' for n in expected}:
            raise ValueError('Unexpected mutation guard delta: ' + repr(changed))
        candidate = run / 'candidate-controlled-npc-data-unqualified.jar'
        with zipfile.ZipFile(candidate, 'w') as archive:
            for name, data in sorted(entries.items()): archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
        invoke([JAVA / 'javac.exe', *exports, '-encoding', 'UTF-8', '-d', run / 'tools', inputs / 'CloseoutAreaOutputAudit.java', inputs / 'CloseoutControlledNpcDataAudit.java'])
        invoke([JAVA / 'java.exe', *exports, '-cp', run / 'tools', 'CloseoutControlledNpcDataAudit', base, candidate, run / 'static-method-retention.txt'])
    parent_manifest = json.loads(checked(parent['manifest']).read_text('utf-8'))
    dependencies = []; (run / 'lib').mkdir()
    for item in parent_manifest['runtime_dependencies']:
        source = checked(item['packaged']); target = run / 'lib' / source.name; shutil.copyfile(source, target)
        if pin(source)['sha256'] != pin(target)['sha256']: raise ValueError('Runtime dependency copy mismatch')
        dependencies.append({'source': pin(source), 'packaged': pin(target)})
    manifest = {
        'status': 'controlled-NPC-birth-facts-and-selection-core-compiled-static-audit-only', 'runtime': pin(candidate), 'parent': parent,
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'dependencies': [pin(p) for p in asm],
        'runtime_dependencies': dependencies, 'commands': commands, 'log': pin(run / 'compile.log'),
        'Source': source_refs,
        'static_method_retention': pin(run / 'static-method-retention.txt'), 'changed_entries': changed,
        'scope': 'Pet/summon controlled factory and accepted pet evolution freeze recovery and complete mobskill scalar facts from the same existing readonly NPC snapshot. Includes original row order/type, trigger random/HP/companion HP/range/count, retargeting, dimensions, leverage, skill/action/gfx, summon bounds, poly and reuse, plus template INT/WIS/family and magic speeds. Data is immutable and detached from mutable source templates. Recovery defaults and original constructors/record identity retained. This is live birth-data wiring plus a compiled Source selection/dispatch core with explicit ports. Source trigger order, retarget scan behavior, strict percentile boundary, random draws and successful-only use count/delay callbacks are preserved. Actual actor/effect port binding to companion ticks and equipment SP consumers remain open.',
        'remaining': ['Controlled NPC actual actor/visibility/effect adapters and live tick binding for the compiled mobskill core', 'Pet equipment SP and De actor/stat consumer binding', 'remaining skill/control/data consumers and separately authorized runtime validation'],
        'tests_executed': False, 'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-controlled-npc-data-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__': main()
