"""Compose reviewed 0.6.70/71 seams into combat candidate; compile/package only."""
from pathlib import Path
from datetime import datetime, timezone
import json
import csv
import os
import shutil
import subprocess
import zipfile
from build_development_closeout import ROOT, SERVER, JAVA, pin, checked, write

LAUNCHER = SERVER.parent.parent / 'lito登入器/LiTo381-Launcher'
WORLD = LAUNCHER / 'tools/world-resource-compat'
EXPORTS = [value for package in ['asm', 'asm.tree', 'asm.util']
           for value in ['--add-exports', 'java.base/jdk.internal.org.objectweb.' + package + '=ALL-UNNAMED']]


def main():
    parent_ref = ROOT / 'build/development-closeout-build-current.json'
    parent = json.loads(parent_ref.read_text('utf-8'))
    base = checked(parent['runtime'])
    if pin(base)['sha256'] != '3ccbb6f5957436cc0ba9bd67d373f9dfcbedad16553009842eaf5db6d0e88e9a':
        raise ValueError('Review new combat parent before composing release seams')
    formal = LAUNCHER / 'app/server/LiTo381-SharedResources-candidate.jar'
    if pin(formal)['sha256'] != 'e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498':
        raise ValueError('Review new formal release before composing')
    run = ROOT / 'build/development-release-compat' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'
    inputs.mkdir(parents=True)
    names = ['PacketBoxProducerPatch.java', 'BluePotionPresentationPatch.java', 'SelectionRecoveryPatch.java', 'ClassIconServerPatch.java']
    refs = [WORLD / n for n in names] + [SERVER / 'tools/gm-polyme/Patch.java', Path(__file__), ROOT / 'build_development_closeout.py', ROOT / 'CloseoutMethodInventory.java']
    for source in refs:
        shutil.copyfile(source, inputs / source.name)
    asmroot = Path.home() / '.m2/repository/org/ow2/asm'
    asm = [asmroot / n / '9.7' / (n + '-9.7.jar') for n in ['asm', 'asm-tree']]
    cp = ';'.join(map(str, [run / 'tools', base, *asm]))
    env = dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
    commands = []
    with zipfile.ZipFile(base) as z:
        entries = {n: z.read(n) for n in z.namelist()}
    original = dict(entries)
    with (run / 'compile.log').open('w', encoding='utf-8') as log:
        def invoke(args):
            args = list(map(str, args))
            commands.append(args)
            subprocess.run(args, cwd=SERVER, env=env, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180)
        invoke([JAVA / 'javac.exe', *EXPORTS, '-encoding', 'UTF-8', '-d', run / 'tools', *[inputs / n for n in names]])
        invoke([JAVA / 'javac.exe', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'Patch.java'])
        def patch(tool, cls, internal=True):
            entry = 'local/lito381/server/' + cls + '.class'
            before = run / (tool + '-' + cls + '.before.class')
            after = run / (tool + '-' + cls + '.after.class')
            before.write_bytes(entries[entry])
            invoke([JAVA / 'java.exe', *(EXPORTS if internal else []), '-cp', cp, tool, before, after])
            entries[entry] = after.read_bytes()
        patch('PacketBoxProducerPatch', 'Protocol')
        patch('BluePotionPresentationPatch', 'ClientSession')
        patch('SelectionRecoveryPatch', 'InventoryCatalog$NativeArmorStatus')
        patch('SelectionRecoveryPatch', 'ClientSession')
        patch('ClassIconServerPatch', 'Protocol')
        patch('ClassIconServerPatch', 'ClientSession')
        patch('Patch', 'GmTravel', False)
        patch('Patch', 'ClientSession', False)
    helpers = ['GmPolyMe', 'GmPolyMe$Forms', 'SelectionRecovery', 'SharedPresentationPackets']
    with zipfile.ZipFile(formal) as release:
        for name in helpers:
            entry = 'local/lito381/server/' + name + '.class'
            if entry in entries:
                raise ValueError('Unexpected existing helper: ' + entry)
            entries[entry] = release.read(entry)
    candidate = run / 'candidate-release-compat-unqualified.jar'
    with zipfile.ZipFile(candidate, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(entries.items()):
            archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), data)
    changed = [n for n in original if original[n] != entries[n]]
    expected = ['Protocol', 'ClientSession', 'InventoryCatalog$NativeArmorStatus', 'GmTravel']
    if set(changed) != {'local/lito381/server/' + n + '.class' for n in expected}:
        raise ValueError('Unexpected changed classes')
    contract_path = WORLD / 'build/presentation-contract.json'
    contract = json.loads(contract_path.read_text('utf-8'))
    ancestor_ref = next(x['jar'] for x in contract['jars'] if x['label'] == 'backend-qualified')
    ancestor = checked(ancestor_ref)
    inventories = {}
    with (run / 'compile.log').open('a', encoding='utf-8') as log:
        invoke([JAVA / 'javac.exe', *EXPORTS, '-encoding', 'UTF-8', '-d', run / 'tools', inputs / 'CloseoutMethodInventory.java'])
        for label, path in [('ancestor', ancestor), ('formal', formal), ('parent', base), ('candidate', candidate)]:
            inventory = run / (label + '-methods.tsv')
            invoke([JAVA / 'java.exe', *EXPORTS, '-cp', run / 'tools', 'CloseoutMethodInventory', path, inventory])
            with inventory.open(encoding='utf-8') as stream:
                inventories[label] = {(r['owner'], r['method'], r['descriptor']): r['sha256'] for r in csv.DictReader(stream, delimiter='\t')}
    def delta(a, b):
        return {k for k in inventories[a].keys() | inventories[b].keys() if inventories[a].get(k) != inventories[b].get(k)}
    release_delta, candidate_delta = delta('ancestor', 'formal'), delta('parent', 'candidate')
    if release_delta != candidate_delta or len(release_delta) != 11:
        raise ValueError('Release method delta drift')
    if any(inventories['candidate'].get(k) != inventories['formal'].get(k) for k in release_delta):
        raise ValueError('Release seam conflicts with combat implementation: review methods')
    # All other methods in each patched combat class remain identical to parent.
    # No runtime code or test plan executes during this static inventory.
    dependencies = [pin(p) for p in asm]
    release = json.loads((LAUNCHER / 'docs/maintenance/release-0.6.70.json').read_text('utf-8'))
    asset_refs = [ref for ref in release['runtime'] if any(t in ref['path'].replace('\\', '/') for t in ['/app/compat/', '/app/resources/class-icons/'])]
    for ref in asset_refs:
        checked(ref)
    dependencies.extend(asset_refs)
    manifest = {
        'status': 'release-seams-compiled-packaged-static-matched-tests-deferred',
        'parent': parent, 'formal': pin(formal), 'runtime': pin(candidate),
        'sources': [pin(p) for p in refs], 'frozen_sources': [pin(p) for p in sorted(inputs.iterdir())],
        'dependencies': dependencies, 'commands': commands,
        'formal_ancestor': ancestor_ref, 'formal_ancestor_contract': pin(contract_path),
        'static_method_comparison': {
            'changed_methods': [list(k) for k in sorted(release_delta)],
            'all_11_release_methods_match_formal': True,
            'all_other_combat_methods_retained': True,
            'inventories': [pin(run / (n + '-methods.tsv')) for n in inventories],
        },
        'changed_entries': changed, 'added_helpers_from_formal': helpers,
        'log': pin(run / 'compile.log'), 'tests_executed': False,
        'database_connected': False, 'deployed': False,
        'development_closeout_complete': False, 'qualified_for_release': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'runtime': pin(candidate), 'manifest': pin(run / 'manifest.json')}
    write(ROOT / 'build/development-release-compat-current.json', result)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
